package com.chattriggers.ctjs

import com.chattriggers.ctjs.api.Config
import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.client.KeyBind
import com.chattriggers.ctjs.api.client.Player
import com.chattriggers.ctjs.api.commands.DynamicCommands
import com.chattriggers.ctjs.api.message.ChatLib
import com.chattriggers.ctjs.api.triggers.TriggerType
import com.chattriggers.ctjs.api.world.Scoreboard
import com.chattriggers.ctjs.api.world.TabList
import com.chattriggers.ctjs.api.world.World
import com.chattriggers.ctjs.engine.Console
import com.chattriggers.ctjs.engine.Register
import com.chattriggers.ctjs.engine.printTraceToConsole
import com.chattriggers.ctjs.internal.commands.StaticCommand
import com.chattriggers.ctjs.internal.engine.module.ModuleManager
import com.chattriggers.ctjs.internal.lifecycle.GenerationSnapshot
import com.chattriggers.ctjs.internal.lifecycle.OwnedKind
import com.chattriggers.ctjs.internal.lifecycle.RuntimeGenerations
import com.chattriggers.ctjs.internal.listeners.ClientListener
import com.chattriggers.ctjs.internal.utils.Initializer
import com.chattriggers.ctjs.internal.utils.NetworkStreams
import kotlinx.serialization.json.Json
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.net.URI
import java.net.URL
import java.net.URLConnection
import java.security.MessageDigest
import java.util.*
import kotlin.concurrent.thread

class CTJS : ClientModInitializer {
    override fun onInitializeClient() {
        Client.referenceSystemTime = System.nanoTime()
        Initializer.initializers.forEach(Initializer::init)

        val playerUuid = Player.getUUID().toString()
        thread(name = "CTJS statistics") {
            reportHashedUUID(playerUuid)
        }

        Config.loadData()

        Runtime.getRuntime().addShutdownHook(Thread {
            TriggerType.GAME_UNLOAD.triggerAll()
            val shutdownGeneration = RuntimeGenerations.stopCurrent()
            shutdownGeneration.invalidate(OwnedKind.CLASS_LOADER)
            Console.close()
        })
    }

    private fun reportHashedUUID(playerUuid: String) {
        val uuid = playerUuid.encodeToByteArray()
        val salt = (System.getProperty("user.name") ?: "").encodeToByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val hashedUUID = md.digest(uuid)
        val hash = Base64.getUrlEncoder().encodeToString(hashedUUID)

        val url = "$WEBSITE_ROOT/api/statistics/track?hash=$hash&version=$MOD_VERSION"
        val connection = makeWebRequest(url)
        NetworkStreams.useInput(connection) { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (input.read(buffer) >= 0) {
                // Consume the response so keep-alive connections can be released cleanly.
            }
        }
    }

    companion object {
        const val MOD_ID = "ctjs"
        const val WEBSITE_ROOT = "https://www.chattriggers.com"
        const val MOD_VERSION = "3.0.0"
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
        fun unload(asCommand: Boolean = true) {
            Client.getMinecraft().execute {
                unloadOnClientThread(asCommand, finalizeGeneration = true)
            }
        }

        private fun unloadOnClientThread(asCommand: Boolean, finalizeGeneration: Boolean): GenerationSnapshot {
            if (isLoaded) {
                TriggerType.WORLD_UNLOAD.triggerAll()
                TriggerType.GAME_UNLOAD.triggerAll()
            }

            val generation = RuntimeGenerations.stopCurrent()

            isLoaded = false

            generation.invalidate(OwnedKind.TASK)
            generation.invalidate(OwnedKind.TIMEOUT)
            ClientListener.pruneInvalidatedTasks()

            generation.invalidate(OwnedKind.CLASS_LOADER)

            ModuleManager.teardown()
            KeyBind.clearKeyBinds()
            Register.clearCustomTriggers()
            StaticCommand.unregisterAll()
            DynamicCommands.unregisterAll()

            generation.invalidate(OwnedKind.UI)

            Scoreboard.clearCustom()
            TabList.clearCustom()

            generation.invalidate(OwnedKind.RESOURCE)

            if (Config.clearConsoleOnLoad)
                Console.clear()

            if (asCommand)
                ChatLib.chat("&7Unloaded ChatTriggers")

            if (finalizeGeneration)
                generation.markDead()
            return generation
        }

        @JvmStatic
        fun load(asCommand: Boolean = true) {
            Client.getMinecraft().execute {
                loadOnClientThread(asCommand)
            }
        }

        private fun loadOnClientThread(asCommand: Boolean) {
            val minecraft = Client.getMinecraft()
            minecraft.options.save()
            val oldGeneration = unloadOnClientThread(asCommand = false, finalizeGeneration = false)

            if (asCommand)
                ChatLib.chat("&cReloading ChatTriggers...")

            thread(name = "CTJS module setup") {
                val preparedModules = try {
                    // Module update checks and filesystem preparation may block, so keep them
                    // off the render thread. Module entrypoints are deliberately not run here:
                    // top-level module code is allowed to call Minecraft-backed CTJS APIs.
                    ModuleManager.setup()
                } catch (e: Throwable) {
                    minecraft.execute {
                        oldGeneration.markDead()
                        e.printTraceToConsole()
                        if (asCommand)
                            ChatLib.chat("&cFailed to reload ChatTriggers")
                    }
                    return@thread
                }

                minecraft.execute {
                    minecraft.options.load()

                    val newGeneration = RuntimeGenerations.publishNext(oldGeneration)
                    if (newGeneration == null) {
                        preparedModules.close()
                        oldGeneration.markDead()
                        isLoaded = false
                        IllegalStateException("CTJS reload generation was superseded").printTraceToConsole()
                        if (asCommand)
                            ChatLib.chat("&cFailed to reload ChatTriggers")
                        return@execute
                    }

                    try {
                        check(ModuleManager.publishPrepared(preparedModules, newGeneration)) {
                            "Failed to publish the module classloader generation"
                        }

                        // Need to set isLoaded to true before running modules, otherwise custom triggers
                        // activated at the top level will not work. Everything below may invoke module
                        // code or Minecraft APIs and therefore must remain on the client/render thread.
                        isLoaded = true

                        ModuleManager.entryPass()

                        if (asCommand)
                            ChatLib.chat("&aDone reloading!")

                        TriggerType.GAME_LOAD.triggerAll()
                        if (World.isLoaded())
                            TriggerType.WORLD_LOAD.triggerAll()
                    } catch (e: Throwable) {
                        val failedGeneration = RuntimeGenerations.stopCurrent()
                        failedGeneration.invalidateAll()
                        failedGeneration.markDead()
                        ClientListener.pruneInvalidatedTasks()
                        isLoaded = false
                        e.printTraceToConsole()
                        if (asCommand)
                            ChatLib.chat("&cFailed to reload ChatTriggers")
                    } finally {
                        oldGeneration.markDead()
                    }
                }
            }
        }
    }
}
