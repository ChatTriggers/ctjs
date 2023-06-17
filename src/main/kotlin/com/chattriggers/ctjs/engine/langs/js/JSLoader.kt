package com.chattriggers.ctjs.engine.langs.js

import com.chattriggers.ctjs.console.LogType
import com.chattriggers.ctjs.console.printToConsole
import com.chattriggers.ctjs.console.printTraceToConsole
import com.chattriggers.ctjs.engine.ILoader
import com.chattriggers.ctjs.engine.langs.Lang
import com.chattriggers.ctjs.engine.module.Module
import com.chattriggers.ctjs.engine.module.ModuleManager.modulesFolder
import com.chattriggers.ctjs.launch.IInjector
import com.chattriggers.ctjs.launch.Mixin
import com.chattriggers.ctjs.launch.MixinCallback
import com.chattriggers.ctjs.triggers.Trigger
import com.chattriggers.ctjs.triggers.TriggerType
import org.mozilla.javascript.*
import org.mozilla.javascript.Function
import org.mozilla.javascript.commonjs.module.ModuleScriptProvider
import org.mozilla.javascript.commonjs.module.Require
import org.mozilla.javascript.commonjs.module.provider.StrongCachingModuleScriptProvider
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider
import java.io.File
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.net.URI
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
object JSLoader : ILoader {
    private val triggers = ConcurrentHashMap<TriggerType, ConcurrentSkipListSet<Trigger>>()

    private lateinit var moduleContext: Context
    private lateinit var evalContext: Context
    private lateinit var scope: Scriptable
    private lateinit var require: CTRequire
    private lateinit var moduleProvider: ModuleScriptProvider

    private var mixinLibsLoaded = false
    private var mixinsFinalized = false
    private var nextMixinId = 0
    private val mixinIdMap = mutableMapOf<Int, MixinCallback>()
    private val mixins = mutableMapOf<Mixin, ILoader.MixinDetails>()

    private val INVOKE_MIXIN_CALL = MethodHandles.lookup().findStatic(
        JSLoader::class.java,
        "invokeMixin",
        MethodType.methodType(Any::class.java, Callable::class.java, Array<Any?>::class.java),
    )

    override fun exec(type: TriggerType, args: Array<out Any?>) {
        triggers[type]?.forEach { it.trigger(args) }
    }

    override fun addTrigger(trigger: Trigger) {
        triggers.getOrPut(trigger.type, ::ConcurrentSkipListSet).add(trigger)
    }

    override fun clearTriggers() {
        triggers.clear()
    }

    override fun removeTrigger(trigger: Trigger) {
        triggers[trigger.type]?.remove(trigger)
    }

    override fun setup(jars: List<URL>) {
        // Ensure all active mixins are invalidated
        // TODO: It would be nice to do this, but it's possible to have a @Redirect or similar
        //       mixin try to call it's handler between when we start loading and when we finish
        //       loading, which would crash. So we need to be smarter about invalidation on load.
        // mixinIdMap.values.forEach(MixinCallback::release)

        JSContextFactory.addAllURLs(jars)

        moduleContext = JSContextFactory.enterContext()
        val sourceProvider = UrlModuleSourceProvider(listOf(modulesFolder.toURI()), listOf())
        moduleProvider = StrongCachingModuleScriptProvider(sourceProvider)
        scope = ImporterTopLevel(moduleContext)
        require = CTRequire(moduleProvider)
        require.install(scope)

        Context.exit()

        JSContextFactory.optimize = false
        evalContext = JSContextFactory.enterContext()
        Context.exit()
        JSContextFactory.optimize = true

        mixinLibsLoaded = false
    }

    override fun mixinSetup(modules: List<Module>): Map<Mixin, ILoader.MixinDetails> {
        loadMixinLibs()

        wrapInContext {
            modules.forEach { module ->
                try {
                    val uri = File(module.folder, module.metadata.mixinEntry!!).normalize().toURI()
                    require.loadCTModule(uri.toString(), uri)
                } catch (e: Throwable) {
                    e.printTraceToConsole(console)
                }
            }
        }

        mixinsFinalized = true
        mixinLibsLoaded = true

        return mixins
    }

    override fun entrySetup(): Unit = wrapInContext {
        if (!mixinLibsLoaded)
            loadMixinLibs()

        val moduleProvidedLibs = saveResource(
            "/assets/chattriggers/js/moduleProvidedLibs.js",
            File(modulesFolder.parentFile, "chattriggers-modules-provided-libs.js"),
            true
        )

        try {
            moduleContext.evaluateString(
                scope,
                moduleProvidedLibs,
                "moduleProvided",
                1, null
            )
        } catch (e: Throwable) {
            e.printTraceToConsole(console)
        }
    }

    override fun entryPass(module: Module, entryURI: URI): Unit = wrapInContext {
        try {
            require.loadCTModule(module.name, entryURI)
        } catch (e: Throwable) {
            println("Error loading module ${module.name}")
            "Error loading module ${module.name}".printToConsole(console, LogType.ERROR)
            e.printTraceToConsole(console)
        }
    }

    internal inline fun <T> wrapInContext(context: Context = moduleContext, crossinline block: () -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        val missingContext = Context.getCurrentContext() == null
        if (missingContext) {
            try {
                JSContextFactory.enterContext(context)
            } catch (e: Throwable) {
                JSContextFactory.enterContext()
            }
        }

        try {
            return block()
        } finally {
            if (missingContext) Context.exit()
        }
    }

    override fun eval(code: String): String {
        return wrapInContext(evalContext) {
            Context.toString(evalContext.evaluateString(scope, code, "<eval>", 1, null))
        }
    }

    override fun getLanguage() = Lang.JS

    override fun trigger(trigger: Trigger, method: Any, args: Array<out Any?>) {
        wrapInContext {
            try {
                require(method is Function) { "Need to pass actual function to the register function, not the name!" }

                method.call(Context.getCurrentContext(), scope, scope, args)
            } catch (e: Throwable) {
                e.printTraceToConsole(console)
                removeTrigger(trigger)
            }
        }
    }

    private fun loadMixinLibs() {
        val mixinProvidedLibs = saveResource(
            "/assets/chattriggers/js/mixinProvidedLibs.js",
            File(modulesFolder.parentFile, "chattriggers-mixin-provided-libs.js"),
            true,
        )

        wrapInContext {
            try {
                moduleContext.evaluateString(
                    scope,
                    mixinProvidedLibs,
                    "mixinProvided",
                    1, null
                )
            } catch (e: Throwable) {
                e.printTraceToConsole()
            }
        }
    }

    override fun invokeMixinLookup(id: Int): MixinCallback {
        val callback = mixinIdMap[id] ?: error("Unknown mixin id $id for loader ${this::class.simpleName}")

        callback.handle = if (callback.method != null) {
            try {
                require(callback.method is Callable) {
                    "The value passed to MixinCallback.attach() must be a function"
                }

                INVOKE_MIXIN_CALL.bindTo(callback.method)
            } catch (e: Throwable) {
                // TODO: More detail (how can we get the module name?)
                "Error loading mixin callback".printToConsole(console)
                e.printTraceToConsole(console)

                MethodHandles.dropArguments(
                    MethodHandles.constant(Any::class.java, null),
                    0,
                    Array<Any?>::class.java,
                )
            }
        } else null

        return callback
    }

    @JvmStatic
    fun invokeMixin(func: Callable, args: Array<Any?>): Any? {
        return wrapInContext {
            func.call(Context.getCurrentContext(), scope, scope, args)
        }
    }

    fun registerInjector(mixin: Mixin, injector: IInjector): MixinCallback? {
        return if (mixinsFinalized) {
            // We are reprocessing the mixin entry file from a ct reload, so we can
            // just return the existing mixin callback. If none exists, then the user
            // has added a mixin, which requires a reload

            val existing = mixins[mixin]?.injectors?.find { it.injector == injector }
            if (existing != null) {
                existing
            } else {
                "A new injector mixin was registered at runtime. This will require a restart, and will " +
                    "have no effect until then!".printToConsole(console)
                null
            }
        } else {
            val id = nextMixinId++
            MixinCallback(id, injector).also {
                mixinIdMap[id] = it
                mixins.getOrPut(mixin, ILoader::MixinDetails).injectors.add(it)
            }
        }
    }

    fun registerFieldWidener(mixin: Mixin, fieldName: String, isMutable: Boolean) {
        if (mixinsFinalized) {
            if (mixins[mixin]?.fieldWideners?.contains(fieldName) != null) {
                "A new field widener was registered at runtime. This will require a restart, and will " +
                    "have no effect until then!".printToConsole(console)
            }
        } else {
            mixins.getOrPut(mixin, ILoader::MixinDetails).fieldWideners[fieldName] = isMutable
        }
    }

    fun registerMethodWidener(mixin: Mixin, methodName: String, isMutable: Boolean) {
        if (mixinsFinalized) {
            if (mixins[mixin]?.methodWideners?.contains(methodName) != null) {
                "A new method widener was registered at runtime. This will require a restart, and will " +
                    "have no effect until then!".printToConsole(console)
            }
        } else {
            mixins.getOrPut(mixin, ILoader::MixinDetails).methodWideners[methodName] = isMutable
        }
    }

    class CTRequire(
        moduleProvider: ModuleScriptProvider,
    ) : Require(moduleContext, scope, moduleProvider, null, null, false) {
        fun loadCTModule(cachedName: String, uri: URI): Scriptable {
            return getExportedModuleInterface(moduleContext, cachedName, uri, null, false)
        }
    }
}
