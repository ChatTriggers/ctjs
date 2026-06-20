package com.chattriggers.ctjs

import com.chattriggers.ctjs.api.CustomCommand
import com.chattriggers.ctjs.engine.Console
import com.chattriggers.ctjs.engine.Register
import com.chattriggers.ctjs.internal.engine.module.ModuleManager
import com.chattriggers.ctjs.internal.utils.Initializer
import kotlinx.serialization.json.Json
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import java.io.File
import java.net.URI
import java.net.URLConnection
import kotlin.concurrent.thread

class CTJS : ClientModInitializer {
    override fun onInitializeClient() {
        Initializer.initializers.forEach(Initializer::init)

        Runtime.getRuntime().addShutdownHook(Thread {
            Console.close()
        })
    }

    companion object {
        const val MOD_ID = "ctjs"
        const val MOD_VERSION = "3.0.0-beta"
        const val MODULES_FOLDER = "./config/ChatTriggers/modules"

        val configLocation = File("./config")
        val assetsDir = File(configLocation, "ChatTriggers/assets/").apply { mkdirs() }

        @JvmStatic
        var isLoaded = true
            private set

        internal val isDevelopment = FabricLoader.getInstance().isDevelopmentEnvironment

        internal val json = Json {
            useAlternativeNames = true
            ignoreUnknownKeys = true
        }

        @JvmOverloads
        internal fun makeWebRequest(url: String, userAgent: String? = "Mozilla/5.0 (ChatTriggers)"): URLConnection =
            URI(url).toURL().openConnection().apply {
                setRequestProperty("User-Agent", userAgent)
                connectTimeout = 3000
                readTimeout = 3000
            }

        @JvmStatic
        fun unload() {
            isLoaded = false

            ModuleManager.teardown()
            Register.clearCustomTriggers()
            CustomCommand.unregisterAll()

            Console.clear()
        }

        @JvmStatic
        fun load() {
            Minecraft.getInstance().options.save()
            unload()

            thread {
                ModuleManager.setup()
                Minecraft.getInstance().options.load()

                // Need to set isLoaded to true before running modules, otherwise custom triggers
                // activated at the top level will not work
                isLoaded = true

                ModuleManager.entryPass()
            }
        }
    }
}
