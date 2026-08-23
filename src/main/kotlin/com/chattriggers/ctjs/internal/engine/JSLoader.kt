package com.chattriggers.ctjs.internal.engine

import com.chattriggers.ctjs.api.triggers.ITriggerType
import com.chattriggers.ctjs.api.triggers.Trigger
import com.chattriggers.ctjs.engine.LogType
import com.chattriggers.ctjs.engine.MixinCallback
import com.chattriggers.ctjs.engine.printToConsole
import com.chattriggers.ctjs.engine.printTraceToConsole
import com.chattriggers.ctjs.internal.engine.module.Module
import com.chattriggers.ctjs.internal.engine.module.ModuleManager.modulesFolder
import com.chattriggers.ctjs.internal.launch.IInjector
import com.chattriggers.ctjs.internal.launch.Mixin
import com.chattriggers.ctjs.internal.launch.MixinDetails
import com.chattriggers.ctjs.internal.lifecycle.RuntimeOwner
import org.apache.commons.io.FileUtils
import org.mozilla.javascript.*
import org.mozilla.javascript.commonjs.module.ModuleScriptProvider
import org.mozilla.javascript.commonjs.module.Require
import org.mozilla.javascript.commonjs.module.provider.StrongCachingModuleScriptProvider
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider
import java.io.File
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.net.URI
import java.net.URL
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
object JSLoader {
    private val triggers = ConcurrentHashMap<ITriggerType, ConcurrentSkipListSet<Trigger>>()
    private val runtimeLock = Any()

    @Volatile
    private var activeRuntime: RuntimeState? = null

    private val moduleScope: Scriptable
        get() = currentRuntime().moduleScope
    private val evalScope: Scriptable
        get() = currentRuntime().evalScope
    private val require: CTRequire
        get() = currentRuntime().require

    private var mixinLibsLoaded = false
    private var mixinsFinalized = false
    private var nextMixinId = 0
    private val mixinIdMap = mutableMapOf<Int, MixinCallback>()
    private val mixins = mutableMapOf<Mixin, MixinDetails>()

    private val INVOKE_MIXIN_CALL = MethodHandles.lookup().findVirtual(
        MixinCallback::class.java,
        "invokeStable",
        MethodType.methodType(Any::class.java, Array<Any?>::class.java),
    )

    internal fun prepareGeneration(jars: List<URL>): PreparedGenerationRuntime {
        // Ensure all active mixins are invalidated
        // TODO: It would be nice to do this, but it's possible to have a @Redirect or similar
        //       mixin try to call it's handler between when we start loading and when we finish
        //       loading, which would crash. So we need to be smarter about invalidation on load.
        // mixinIdMap.values.forEach(MixinCallback::release)

        return PreparedGenerationRuntime(JSContextFactory.stageGenerationLoader(jars))
    }

    internal fun publishPrepared(prepared: PreparedGenerationRuntime, owner: RuntimeOwner): Boolean {
        val loader = prepared.takeLoader() ?: return false
        if (!loader.activate(owner) { retireLoader(loader) })
            return false

        try {
            JSContextFactory.activateGenerationLoader(loader)

            val runtime = createRuntime(loader, owner)
            synchronized(runtimeLock) {
                check(activeRuntime == null) { "A module generation runtime is already active" }
                activeRuntime = runtime
            }

            mixinLibsLoaded = false
            return true
        } catch (e: Throwable) {
            loader.close()
            throw e
        }
    }

    internal fun addGenerationJars(jars: List<URL>): GenerationModuleClassLoader? {
        val loader = activeRuntime?.loader ?: return null
        return loader.takeIf { it.addJars(jars) && it.isActive }
    }

    internal fun isActiveLoader(loader: GenerationModuleClassLoader?): Boolean =
        loader != null && activeRuntime?.loader === loader && loader.isActive

    private fun createRuntime(loader: GenerationModuleClassLoader, owner: RuntimeOwner): RuntimeState {
        val cx = JSContextFactory.enterContext()
        try {
            cx.applicationClassLoader = loader
            val sourceProvider = UrlModuleSourceProvider(listOf(modulesFolder.toURI()), listOf())
            val moduleProvider = StrongCachingModuleScriptProvider(sourceProvider)
            val moduleScope = ImporterTopLevel(cx)
            val evalScope = ImporterTopLevel(cx)
            val require = CTRequire(moduleScope, moduleProvider)
            require.install(moduleScope)
            require.install(evalScope)
            return RuntimeState(loader, owner, moduleScope, evalScope, require)
        } finally {
            Context.exit()
        }
    }

    private fun retireLoader(loader: GenerationModuleClassLoader) {
        synchronized(runtimeLock) {
            if (activeRuntime?.loader === loader)
                activeRuntime = null
        }
        JSContextFactory.deactivateGenerationLoader(loader)
        mixinLibsLoaded = false
    }

    private fun currentRuntime(): RuntimeState =
        checkNotNull(activeRuntime) { "No active module generation runtime" }

    internal fun currentRuntimeOwner(): RuntimeOwner = currentRuntime().owner

    internal class PreparedGenerationRuntime internal constructor(
        private var loader: GenerationModuleClassLoader?,
    ) : AutoCloseable {
        internal fun takeLoader(): GenerationModuleClassLoader? = synchronized(this) {
            loader.also { loader = null }
        }

        override fun close() {
            takeLoader()?.close()
        }
    }

    internal fun mixinSetup(modules: List<Module>): Map<Mixin, MixinDetails> {
        loadMixinLibs()

        wrapInContext {
            modules.forEach { module ->
                try {
                    val uri = File(module.folder, module.metadata.mixinEntry!!).normalize().toURI()
                    require.loadCTModule(uri.toString(), uri)
                } catch (e: Throwable) {
                    e.printTraceToConsole()
                }
            }
        }

        mixinsFinalized = true
        mixinLibsLoaded = true

        return mixins
    }

    /** Re-executes already transformed mixin entry files to publish current-generation handlers. */
    internal fun rebindMixinCallbacks(modules: List<Module>) {
        if (modules.isEmpty())
            return
        loadMixinLibs()
        wrapInContext {
            modules.forEach { module ->
                try {
                    val uri = File(module.folder, module.metadata.mixinEntry!!).normalize().toURI()
                    require.loadCTModule("ctjs:mixin:${module.name}", uri)
                } catch (e: Throwable) {
                    "Error rebinding mixin callbacks for module ${module.name}".printToConsole(LogType.ERROR)
                    e.printTraceToConsole()
                }
            }
        }
    }

    fun entrySetup(): Unit = wrapInContext {
        if (!mixinLibsLoaded)
            loadMixinLibs()

        val moduleProvidedLibs = saveResource(
            "/assets/ctjs/js/moduleProvidedLibs.js",
            File(modulesFolder.parentFile, "chattriggers-modules-provided-libs.js"),
        )

        try {
            val script = it.compileString(moduleProvidedLibs, "moduleProvided", 1, null)
            script.exec(it, moduleScope)
            script.exec(it, evalScope)
        } catch (e: Throwable) {
            e.printTraceToConsole()
        }
    }

    fun entryPass(module: Module, entryURI: URI): Unit = wrapInContext {
        try {
            require.loadCTModule(module.name, entryURI)
        } catch (e: Throwable) {
            println("Error loading module ${module.name}")
            "Error loading module ${module.name}".printToConsole(LogType.ERROR)
            e.printTraceToConsole()
        }
    }

    fun exec(type: ITriggerType, args: Array<out Any?>) {
        triggers[type]?.forEach { it.trigger(args) }
    }

    fun addTrigger(trigger: Trigger) {
        triggers.getOrPut(trigger.type, ::ConcurrentSkipListSet).add(trigger)
    }

    fun clearTriggers() {
        triggers.clear()
    }

    internal fun registeredTriggerCount(): Int = triggers.values.sumOf(Set<*>::size)

    internal fun activeLoaderGenerationId(): Long? = activeRuntime?.loader?.generationId

    fun removeTrigger(trigger: Trigger) {
        triggers[trigger.type]?.remove(trigger)
    }

    // Note: block takes a Context since most caller use it. Context.getContext() is a threadlocal access, so we might
    //       as well avoid it if we can
    internal inline fun <T> wrapInContext(context: Context? = null, crossinline block: (Context) -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        var cx = context ?: Context.getCurrentContext()
        val missingContext = cx == null
        if (missingContext)
            cx = JSContextFactory.enterContext()

        try {
            return block(cx)
        } finally {
            if (missingContext) Context.exit()
        }
    }

    fun eval(code: String): String? {
        return wrapInContext {
            ScriptRuntime.doTopCall(
                { cx, scope, thisObj, args ->
                    try {
                        ScriptRuntime.toString(cx.evaluateString(scope, code, "<eval>", 1, null))
                    } catch (e: Throwable) {
                        e.printTraceToConsole()
                    }
                },
                it,
                evalScope,
                evalScope,
                emptyArray(),
                true,
            ) as? String
        }
    }

    fun invoke(method: Callable, args: Array<out Any?>, thisObj: Scriptable = moduleScope): Any? {
        return wrapInContext {
            Context.jsToJava(method.call(it, moduleScope, thisObj, args), Any::class.java)
        }
    }

    fun trigger(trigger: Trigger, method: Any, args: Array<out Any?>) {
        try {
            require(method is Callable) { "Need to pass actual function to the register function, not the name!" }
            invoke(method, args)
        } catch (e: Throwable) {
            e.printTraceToConsole()
            removeTrigger(trigger)
        }
    }

    private fun loadMixinLibs() {
        if (mixinLibsLoaded)
            return
        val mixinProvidedLibs = saveResource(
            "/assets/ctjs/js/mixinProvidedLibs.js",
            File(modulesFolder.parentFile, "chattriggers-mixin-provided-libs.js"),
        )

        wrapInContext {
            try {
                it.evaluateString(
                    moduleScope,
                    mixinProvidedLibs,
                    "mixinProvided",
                    1, null
                )
            } catch (e: Throwable) {
                e.printTraceToConsole()
            }
        }
        mixinLibsLoaded = true
    }

    @JvmStatic
    fun mixinIsAttached(id: Int) = mixinIdMap[id]?.isActive() == true

    fun invokeMixinLookup(id: Int): MixinCallback {
        val callback = mixinIdMap[id] ?: error("Unknown mixin id $id for loader ${this::class.simpleName}")

        // The generated call site must always resolve to a stable trampoline. The
        // generation-owned slot inside MixinCallback decides whether there is a live
        // handler at invocation time, so linking during the STOPPING/publish window is
        // a safe no-op instead of a failed bootstrap that permanently poisons the
        // invokedynamic instruction.
        callback.handle = try {
            INVOKE_MIXIN_CALL.bindTo(callback)
        } catch (e: Throwable) {
            // This is a pretty vague error, but the trace should make the issue clear
            // since it will include the stack trace from the Mixed-into class
            "Error loading mixin callback".printToConsole()
            e.printTraceToConsole()

            MethodHandles.dropArguments(
                MethodHandles.constant(Any::class.java, null),
                0,
                Array<Any?>::class.java,
            )
        }

        return callback
    }

    @JvmStatic
    fun invokeMixin(func: Callable, args: Array<Any?>): Any? {
        return wrapInContext {
            Context.jsToJava(func.call(it, moduleScope, moduleScope, args), Any::class.java)
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
                ("A new injector mixin was registered at runtime. This will require a restart, and will " +
                    "have no effect until then!").printToConsole()
                null
            }
        } else {
            val id = nextMixinId++
            MixinCallback(id, injector).also {
                mixinIdMap[id] = it
                mixins.getOrPut(mixin, ::MixinDetails).injectors.add(it)
            }
        }
    }

    fun registerFieldWidener(mixin: Mixin, fieldName: String, isMutable: Boolean) {
        if (!mixinsFinalized)
            mixins.getOrPut(mixin, ::MixinDetails).fieldWideners[fieldName] = isMutable
    }

    fun registerMethodWidener(mixin: Mixin, methodName: String, isMutable: Boolean) {
        if (!mixinsFinalized)
            mixins.getOrPut(mixin, ::MixinDetails).methodWideners[methodName] = isMutable
    }

    /**
     * Save a resource to the OS's filesystem from inside the jar
     *
     * @param resourceName name of the file inside the jar
     * @param outputFile file to save to
     */
    private fun saveResource(resourceName: String?, outputFile: File): String {
        require(resourceName != null && resourceName != "") {
            "ResourcePath cannot be null or empty"
        }

        val parsedResourceName = resourceName.replace('\\', '/')
        val resource = javaClass.getResourceAsStream(parsedResourceName)
            ?: throw IllegalArgumentException("The embedded resource '$parsedResourceName' cannot be found.")

        val res = resource.bufferedReader().readText()
        FileUtils.write(outputFile, res, Charset.defaultCharset())
        return res
    }

    private data class RuntimeState(
        val loader: GenerationModuleClassLoader,
        val owner: RuntimeOwner,
        val moduleScope: Scriptable,
        val evalScope: Scriptable,
        val require: CTRequire,
    )

    private class CTRequire(
        moduleScope: Scriptable,
        moduleProvider: ModuleScriptProvider,
    ) : Require(Context.getContext(), moduleScope, moduleProvider, null, null, false) {
        override fun getExportedModuleInterface(
            cx: Context,
            id: String,
            uri: URI?,
            base: URI?,
            isMain: Boolean,
        ): Scriptable {
            return CommonJsExports.ensureDefault(super.getExportedModuleInterface(cx, id, uri, base, isMain))
        }

        fun loadCTModule(cachedName: String, uri: URI): Scriptable {
            return getExportedModuleInterface(Context.getContext(), cachedName, uri, null, false)
        }
    }
}
