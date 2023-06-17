package com.chattriggers.ctjs.launch

import com.chattriggers.ctjs.engine.ILoader
import com.chattriggers.ctjs.engine.module.ModuleManager
import kotlinx.serialization.json.*
import org.spongepowered.asm.mixin.Mixins
import java.io.ByteArrayInputStream
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

internal object DynamicMixinManager {
    internal const val GENERATED_PROTOCOL = "ct-generated"
    internal const val GENERATED_MIXIN = "ct-generated.mixins.json"
    internal const val GENERATED_PACKAGE = "com/chattriggers/ctjs/generated_mixins"

    lateinit var mixins: Map<ILoader, Map<Mixin, ILoader.MixinDetails>>

    fun initialize() {
        mixins = ModuleManager.mixinSetup()
    }

    fun applyMixins() {
        val dynamicMixins = mutableListOf<String>()

        for ((loader, mixinMap) in ModuleManager.mixinSetup()) {
            for ((mixin, details) in mixinMap) {
                // TODO: Generate bytecode for mixin
            }
        }

        ByteBasedStreamHandler[GENERATED_MIXIN] = createDynamicMixinsJson(dynamicMixins)

        injectConfiguration()
    }

    private fun createDynamicMixinsJson(mixins: List<String>): ByteArray {
        return buildJsonObject {
            put("required", JsonPrimitive(true))
            put("minVersion", JsonPrimitive("0.8"))
            put("package", JsonPrimitive("com.chattriggers.ctjs.generated_mixins"))
            put("compatibilityLevel", JsonPrimitive("JAVA_17"))
            putJsonObject("injectors") {
                put("defaultRequire", JsonPrimitive(1))
            }

            putJsonArray("client") { mixins.forEach(::add) }
        }.toString().toByteArray()
    }

    private fun injectConfiguration() {
        // Credit to hugeblank and his allium project for this setup
        // https://github.com/hugeblank/allium/blob/mixins/src/main/java/dev/hugeblank/allium/AlliumPreLaunch.java
        val classLoader = DynamicMixinManager::class.java.classLoader
        val addUrlMethod = classLoader::class.java.methods.first { it.name == "addUrlFwd" }
        addUrlMethod.isAccessible = true
        addUrlMethod.invoke(classLoader, ByteBasedStreamHandler.url)

        Mixins.addConfiguration(GENERATED_MIXIN)
    }

    private object ByteBasedStreamHandler : URLStreamHandler() {
        private val classBytes = mutableMapOf<String, ByteArray>()

        val url = URL(GENERATED_PROTOCOL, null, -1, "/", ByteBasedStreamHandler)

        operator fun set(path: String, bytes: ByteArray) {
            check(classBytes.put(path, bytes) == null)
        }

        override fun openConnection(url: URL): URLConnection? =
            classBytes[url.path.drop(1)]?.let { Connection(url, it) }

        private class Connection(url: URL, private val bytes: ByteArray) : URLConnection(url) {
            override fun getInputStream() = ByteArrayInputStream(bytes)
            override fun connect() = throw UnsupportedOperationException()
        }
    }
}
