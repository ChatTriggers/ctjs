package com.chattriggers.ctjs.internal.launch

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.api.Mappings
import com.chattriggers.ctjs.internal.engine.JSLoader
import com.chattriggers.ctjs.internal.engine.module.ModuleManager
import com.chattriggers.ctjs.internal.launch.generation.DynamicMixinGenerator
import com.chattriggers.ctjs.internal.launch.generation.GenerationContext
import com.chattriggers.ctjs.internal.launch.generation.Utils
import kotlinx.serialization.json.*
import org.spongepowered.asm.mixin.Mixins
import org.spongepowered.asm.service.MixinService
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

internal object DynamicMixinManager {
    internal const val GENERATED_PROTOCOL = "ct-generated"
    internal const val GENERATED_MIXIN = "ct-generated.mixins.json"
    internal const val GENERATED_PACKAGE = "com/chattriggers/ctjs/generated_mixins"

    lateinit var mixins: Map<Mixin, MixinDetails>

    fun initialize() {
        mixins = JSLoader.mixinSetup(ModuleManager.cachedModules.filter { it.metadata.mixinEntry != null })
    }

    fun applyAccessWideners() {
        for ((mixin, details) in mixins) {
            val mappedClass = Mappings.getMappedClass(mixin.target) ?: run {
                if (mixin.remap == false) {
                    Mappings.getUnmappedClass(mixin.target)
                } else {
                    error("Unknown class name ${mixin.target}")
                }
            }

            for ((field, isMutable) in details.fieldWideners)
                Utils.widenField(mappedClass, field, isMutable)
            for ((method, isMutable) in details.methodWideners)
                Utils.widenMethod(mappedClass, method, isMutable)
        }
    }

    fun applyMixins() {
        val dynamicMixins = mutableListOf<String>()

        if (CTJS.isDevelopment) deleteOldMixinClasses()

        for ((mixin, details) in mixins) {
            val ctx = GenerationContext(mixin)
            val generator = DynamicMixinGenerator(ctx, details)
            ByteBasedStreamHandler[ctx.generatedClassFullPath + ".class"] = generator.generate()
            dynamicMixins += ctx.generatedClassName
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

    private fun deleteOldMixinClasses() {
        val dir = File(CTJS.configLocation, "ChatTriggers/mixin-classes")
        dir.listFiles()?.forEach { it.delete() }
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
