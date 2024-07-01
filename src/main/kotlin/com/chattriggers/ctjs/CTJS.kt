package com.chattriggers.ctjs

import com.chattriggers.ctjs.api.Config
import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.client.KeyBind
import com.chattriggers.ctjs.api.client.Player
import com.chattriggers.ctjs.api.client.Sound
import com.chattriggers.ctjs.api.commands.DynamicCommands
import com.chattriggers.ctjs.api.message.ChatLib
import com.chattriggers.ctjs.api.render.Image
import com.chattriggers.ctjs.api.triggers.TriggerType
import com.chattriggers.ctjs.api.world.Scoreboard
import com.chattriggers.ctjs.api.world.World
import com.chattriggers.ctjs.engine.Console
import com.chattriggers.ctjs.engine.Register
import com.chattriggers.ctjs.internal.commands.StaticCommand
import com.chattriggers.ctjs.internal.engine.JSLoader
import com.chattriggers.ctjs.internal.engine.module.Module
import com.chattriggers.ctjs.internal.engine.module.ModuleManager
import com.chattriggers.ctjs.internal.utils.Initializer
import kotlinx.serialization.json.Json
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.net.URL
import java.net.URLConnection
import java.security.MessageDigest
import java.util.*
import kotlin.concurrent.thread

class CTJS : ClientModInitializer {
    override fun onInitializeClient() {
        Client.referenceSystemTime = System.nanoTime()
        Initializer.initializers.forEach(Initializer::init)

        thread {
            reportHashedUUID()
        }

        Config.loadData()

        Runtime.getRuntime().addShutdownHook(Thread {
            TriggerType.GAME_UNLOAD.triggerAll()
            Console.close()
        })
    }

    private fun reportHashedUUID() {
        val uuid = Player.getUUID().toString().encodeToByteArray()
        val salt = (System.getProperty("user.name") ?: "").encodeToByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val hashedUUID = md.digest(uuid)
        val hash = Base64.getUrlEncoder().encodeToString(hashedUUID)

        val url = "$WEBSITE_ROOT/api/statistics/track?hash=$hash&version=$MOD_VERSION"
        val connection = makeWebRequest(url)
        connection.getInputStream()
    }

    companion object {
        const val WEBSITE_ROOT = "https://www.chattriggers.com"
        const val MOD_VERSION = "3.0.0-beta"
        const val MODULES_FOLDER = "./config/ChatTriggers/modules"

        val configLocation = File("./config")
        val assetsDir = File(configLocation, "ChatTriggers/assets/").apply { mkdirs() }

        @JvmStatic
        var isLoaded = true
            private set

        internal val images = mutableListOf<Image>()
        internal val sounds = mutableListOf<Sound>()
        internal val isDevelopment = FabricLoader.getInstance().isDevelopmentEnvironment

        internal val json = Json { useAlternativeNames = true }

        @JvmOverloads
        internal fun makeWebRequest(url: String, userAgent: String? = "Mozilla/5.0 (ChatTriggers)"): URLConnection =
            URL(url).openConnection().apply {
                setRequestProperty("User-Agent", userAgent)
                connectTimeout = 3000
                readTimeout = 3000
            }

        /**
         * Gets the active (currently running) Module object.
         *
         * There are different ways the active module is determined:
         * - At engine startup, the active module is the module that is currently having its "entry" script ran. The
         *   "entry" scripts are ran in dependency order such that modules that don't have any dependencies are ran
         *   first.
         * - After engine startup, the active module is the module that belongs to the trigger that is currently
         *   running, as triggers are the only way user code is ran after engine startup.
         * - On background threads started from the Thread global, the active module is the module which spawned the
         *   background thread.
         * - On background threads not started from the Thread global, such as when invoking java.lang.Thread manually,
         *   the active module is undefined, and this method will return null.
         * - During mixin application, the active module is undefined, and this method will return null.
         *
         * Due to the way user code is exclusively invoked via triggers, this method allows library authors to determine
         * where their library code is being invoked from, assuming this method is called outside a trigger created by
         * the library.
         */
        @JvmStatic
        fun activeModule(): Module? {
            // Copy the object so the users can't change the mutable internal state
            return JSLoader.activeModule?.copy()
        }

        @JvmStatic
        fun unload(asCommand: Boolean = true) {
            TriggerType.WORLD_UNLOAD.triggerAll()
            TriggerType.GAME_UNLOAD.triggerAll()
            Scoreboard.clearCustom()

            isLoaded = false

            ModuleManager.teardown()
            KeyBind.clearKeyBinds()
            Register.clearCustomTriggers()
            StaticCommand.unregisterAll()
            DynamicCommands.unregisterAll()

            if (Config.clearConsoleOnLoad)
                Console.clear()

            Client.scheduleTask {
                images.forEach(Image::destroy)
                sounds.forEach(Sound::destroy)

                images.clear()
                sounds.clear()
            }

            if (asCommand)
                ChatLib.chat("&7Unloaded ChatTriggers")
        }

        @JvmStatic
        fun load(asCommand: Boolean = true) {
            Client.getMinecraft().options.write()
            unload(asCommand = false)

            if (asCommand)
                ChatLib.chat("&cReloading ChatTriggers...")

            thread {
                ModuleManager.setup()
                Client.getMinecraft().options.load()

                // Need to set isLoaded to true before running modules, otherwise custom triggers
                // activated at the top level will not work
                isLoaded = true

                ModuleManager.entryPass()

                if (asCommand)
                    ChatLib.chat("&aDone reloading!")

                TriggerType.GAME_LOAD.triggerAll()
                if (World.isLoaded())
                    TriggerType.WORLD_LOAD.triggerAll()
            }
        }
    }
}
