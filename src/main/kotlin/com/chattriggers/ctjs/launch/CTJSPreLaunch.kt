package com.chattriggers.ctjs.launch

import com.chattriggers.ctjs.engine.module.ModuleManager
import com.chattriggers.ctjs.console.printTraceToConsole
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import org.spongepowered.asm.mixin.Mixins
import java.io.ByteArrayInputStream
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

class CTJSPreLaunch : PreLaunchEntrypoint {
    override fun onPreLaunch() {
        Mappings.initialize()

        // TODO: Threaded setup?
        ModuleManager.setup()

        try {
            generateMixins()
        } catch (e: Throwable) {
            IllegalStateException("Error generating dynamic mixins", e).printTraceToConsole()
        }
    }

    // Credit to hugeblank and his allium project for this entire setup
    // https://github.com/hugeblank/allium/blob/mixins/src/main/java/dev/hugeblank/allium/AlliumPreLaunch.java
    private fun generateMixins() {
        val classLoader = this::class.java.classLoader
        val addUrlMethod = classLoader::class.java.methods.first { it.name == "addUrlFwd" }
        addUrlMethod.isAccessible = true
        addUrlMethod.invoke(classLoader, EldritchURLStreamHandler.create(getDynamicUrlTargets()))

        Mixins.addConfiguration(GENERATED_MIXIN)
    }

    private fun getDynamicUrlTargets(): Map<String, ByteArray> {
        val dynamicTargets = mutableMapOf<String, ByteArray>()

        val dynamicMixins = getDynamicMixins()
        dynamicMixins.forEach { (name, bytes) ->
            dynamicTargets["$GENERATED_PACKAGE/$name.class"] = bytes
        }

        val dynamicMixinConfig = buildJsonObject {
            put("required", JsonPrimitive(true))
            put("minVersion", JsonPrimitive("0.8"))
            put("package", JsonPrimitive("com.chattriggers.ctjs.generated_mixins"))
            put("compatibilityLevel", JsonPrimitive("JAVA_17"))
            putJsonObject("injectors") {
                put("defaultRequire", JsonPrimitive(1))
            }

            putJsonArray("client") {
                dynamicMixins.keys.map(::JsonPrimitive).forEach(::add)
            }
        }

        dynamicTargets[GENERATED_MIXIN] = dynamicMixinConfig.toString().toByteArray()

        return dynamicTargets
    }

    private fun getDynamicMixins(): Map<String, ByteArray> {
        var classNameCounter = 0
        val mixinList = mutableMapOf<String, ByteArray>()

        for (module in ModuleManager.cachedModules) {
            val mixins = module.metadata.mixins ?: continue

            for ((eventName, mixin) in mixins) {
                val className = "CTMixin_${module.name}_${eventName}_${classNameCounter++}"
                mixinList[className] = MixinGenerator("${GENERATED_PACKAGE}/$className", eventName, mixin).generate()
            }
        }

        return mixinList
    }

    class EldritchURLStreamHandler(private val providers: Map<String, ByteArray>) : URLStreamHandler() {
        override fun openConnection(url: URL): URLConnection? =
            providers[url.path.substring(1)]?.let { EldritchConnection(url, it) }

        private class EldritchConnection(url: URL?, private val bytes: ByteArray?) : URLConnection(url) {
            override fun getInputStream() = ByteArrayInputStream(bytes)
            override fun connect() = throw UnsupportedOperationException()
        }

        companion object {
            fun create(providers: Map<String, ByteArray>) =
                URL(GENERATED_PROTOCOL, null, -1, "/", EldritchURLStreamHandler(providers))
        }
    }

    companion object {
        private const val GENERATED_PROTOCOL = "ct-generated"
        const val GENERATED_MIXIN = "ct-generated.mixins.json"
        private const val GENERATED_PACKAGE = "com/chattriggers/ctjs/generated_mixins"
    }
}
