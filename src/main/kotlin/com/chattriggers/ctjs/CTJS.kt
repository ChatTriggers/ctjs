package com.chattriggers.ctjs

import com.chattriggers.ctjs.commands.CTCommand
import com.chattriggers.ctjs.minecraft.libs.renderer.Image
import com.chattriggers.ctjs.minecraft.objects.Sound
import com.chattriggers.ctjs.utils.Initializer
import com.google.gson.Gson
import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import java.io.File
import java.net.URL
import java.net.URLConnection

class CTJS : ClientModInitializer {
    override fun onInitializeClient() {
        Initializer.initializers.forEach(Initializer::init)

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            CTCommand.register(dispatcher)
        }
    }

    companion object {
        const val modulesFolder = "./config/ChatTriggers/modules"
        const val WEBSITE_ROOT = "https://www.chattriggers.com"
        internal val images = mutableListOf<Image>()
        internal val sounds = mutableListOf<Sound>()

        val configLocation = File("./config")
        val assetsDir = File(configLocation, "ChatTriggers/images/").apply { mkdirs() }

        internal val gson = Gson()

        internal fun makeWebRequest(url: String): URLConnection = URL(url).openConnection().apply {
            setRequestProperty("User-Agent", "Mozilla/5.0 (ChatTriggers)")
            connectTimeout = 3000
            readTimeout = 3000
        }
    }
}
