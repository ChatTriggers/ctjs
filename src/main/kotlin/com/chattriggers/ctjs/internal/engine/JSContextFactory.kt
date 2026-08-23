package com.chattriggers.ctjs.internal.engine

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.internal.launch.CTJavaObjectMappingProvider
import org.mozilla.javascript.Context
import org.mozilla.javascript.Context.EMIT_DEBUG_OUTPUT
import org.mozilla.javascript.Context.FEATURE_LOCATION_INFORMATION_IN_ERROR
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.WrapFactory
import java.io.File
import java.net.URL

object JSContextFactory : ContextFactory() {
    private val loaderLock = Any()
    private val bootstrapClassLoader = javaClass.classLoader

    @Volatile
    private var generationClassLoader: GenerationModuleClassLoader? = null

    var optimize = true

    internal fun stageGenerationLoader(urls: Collection<URL>) =
        GenerationModuleClassLoader.stage(urls, bootstrapClassLoader)

    internal fun activateGenerationLoader(loader: GenerationModuleClassLoader) {
        check(loader.isActive) { "Cannot publish an inactive module classloader" }
        synchronized(loaderLock) {
            generationClassLoader = loader
        }
    }

    internal fun deactivateGenerationLoader(loader: GenerationModuleClassLoader) {
        synchronized(loaderLock) {
            if (generationClassLoader === loader)
                generationClassLoader = null
        }
    }

    internal fun activeGenerationLoader(): GenerationModuleClassLoader? =
        generationClassLoader?.takeIf { it.isActive }

    override fun onContextCreated(cx: Context) {
        super.onContextCreated(cx)

        cx.debugOutputPath = File(".", "DEBUG")
        cx.applicationClassLoader = activeGenerationLoader() ?: bootstrapClassLoader
        cx.optimizationLevel = if (optimize) 9 else 0
        cx.languageVersion = Context.VERSION_ES6
        cx.errorReporter = JSErrorReporter

        cx.wrapFactory = WrapFactory().apply {
            isJavaPrimitiveWrap = false
        }

        if (!CTJS.isDevelopment)
            cx.javaObjectMappingProvider = CTJavaObjectMappingProvider
    }

    override fun hasFeature(cx: Context?, featureIndex: Int): Boolean {
        when (featureIndex) {
            FEATURE_LOCATION_INFORMATION_IN_ERROR -> return true
            EMIT_DEBUG_OUTPUT -> return CTJS.isDevelopment
        }

        return super.hasFeature(cx, featureIndex)
    }
}
