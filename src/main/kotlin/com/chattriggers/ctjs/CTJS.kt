package com.chattriggers.ctjs

import com.chattriggers.ctjs.console.*
import com.chattriggers.ctjs.engine.module.ModuleManager
import com.chattriggers.ctjs.minecraft.libs.renderer.Image
import com.chattriggers.ctjs.minecraft.objects.Sound
import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.minecraft.wrappers.Player
import com.chattriggers.ctjs.triggers.TriggerType
import com.chattriggers.ctjs.utils.Config
import com.chattriggers.ctjs.utils.Initializer
import com.google.gson.Gson
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.net.URL
import java.net.URLConnection
import java.security.MessageDigest
import java.util.*
import kotlin.concurrent.thread

internal class CTJS : ClientModInitializer {
    override fun onInitializeClient() {
        Client.referenceSystemTime = System.nanoTime()

        Initializer.initializers.forEach(Initializer::init)

        thread {
            ModuleManager.entryPass()
            reportHashedUUID()
        }

        Config.loadData()

        Runtime.getRuntime().addShutdownHook(Thread {
            TriggerType.GAME_UNLOAD.triggerAll()
            ConsoleManager.closeConsoles()
        })
    }

    private fun reportHashedUUID() {
        val uuid = Player.getUUID().toString().encodeToByteArray()
        val salt = (System.getProperty("user.name") ?: "").encodeToByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val hashedUUID = md.digest(uuid)
        val hash = Base64.getUrlEncoder().encodeToString(hashedUUID)

        val url = "${WEBSITE_ROOT}/api/statistics/track?hash=$hash&version=${Reference.MOD_VERSION}"
        val connection = makeWebRequest(url)
        connection.getInputStream()
    }

    companion object {
        const val WEBSITE_ROOT = "https://www.chattriggers.com"
        internal val images = mutableListOf<Image>()
        internal val sounds = mutableListOf<Sound>()
        internal val isDevelopment = FabricLoader.getInstance().isDevelopmentEnvironment

        val configLocation = File("./config")
        val assetsDir = File(configLocation, "ChatTriggers/assets/").apply { mkdirs() }

        internal val gson = Gson()

        @JvmOverloads
        internal fun makeWebRequest(url: String, userAgent: String? = "Mozilla/5.0 (ChatTriggers)"): URLConnection = URL(url).openConnection().apply {
            setRequestProperty("User-Agent", userAgent)
            connectTimeout = 3000
            readTimeout = 3000
        }
    }
}
