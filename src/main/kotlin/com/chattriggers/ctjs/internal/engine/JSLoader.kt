package com.chattriggers.ctjs.internal.engine

import com.chattriggers.ctjs.api.triggers.ITriggerType
import com.chattriggers.ctjs.api.triggers.Trigger
import com.chattriggers.ctjs.engine.LogType
import com.chattriggers.ctjs.engine.printToConsole
import com.chattriggers.ctjs.engine.printTraceToConsole
import com.chattriggers.ctjs.internal.engine.module.Module
import com.chattriggers.ctjs.internal.engine.module.ModuleManager.modulesFolder
import org.apache.commons.io.FileUtils
import org.mozilla.javascript.*
import org.mozilla.javascript.commonjs.module.ModuleScriptProvider
import org.mozilla.javascript.commonjs.module.Require
import org.mozilla.javascript.commonjs.module.provider.StrongCachingModuleScriptProvider
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider
import java.io.File
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

    private lateinit var moduleScope: Scriptable
    private lateinit var evalScope: Scriptable
    private lateinit var require: CTRequire
    private lateinit var moduleProvider: ModuleScriptProvider

    fun setup(jars: List<URL>) {
        JSContextFactory.addAllURLs(jars)

        val cx = JSContextFactory.enterContext()
        val sourceProvider = UrlModuleSourceProvider(listOf(modulesFolder.toURI()), listOf())
        moduleProvider = StrongCachingModuleScriptProvider(sourceProvider)
        moduleScope = ImporterTopLevel(cx)
        evalScope = ImporterTopLevel(cx)
        require = CTRequire(moduleProvider)
        require.install(moduleScope)
        require.install(evalScope)
        Context.exit()
    }

    fun entrySetup(): Unit = wrapInContext {
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

    private class CTRequire(
        moduleProvider: ModuleScriptProvider,
    ) : Require(Context.getContext(), moduleScope, moduleProvider, null, null, false) {
        fun loadCTModule(cachedName: String, uri: URI): Scriptable {
            return getExportedModuleInterface(Context.getContext(), cachedName, uri, null, false)
        }
    }
}
