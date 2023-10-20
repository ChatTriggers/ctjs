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
import java.net.URLClassLoader

object JSContextFactory : ContextFactory() {
    private val classLoader = ModifiedURLClassLoader()
    var optimize = true

    fun addAllURLs(urls: List<URL>) = classLoader.addAllURLs(urls)

    override fun onContextCreated(cx: Context) {
        super.onContextCreated(cx)

        cx.debugOutputPath = File(".", "DEBUG")
        cx.applicationClassLoader = classLoader
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

    private class ModifiedURLClassLoader : URLClassLoader(arrayOf(), javaClass.classLoader) {
        val sources = mutableSetOf<URL>()

        fun addAllURLs(urls: List<URL>) {
            (urls - sources).forEach(::addURL)
        }

        public override fun addURL(url: URL) {
            super.addURL(url)
            sources.add(url)
        }
    }
}
