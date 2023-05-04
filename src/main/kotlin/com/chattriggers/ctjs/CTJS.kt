package com.chattriggers.ctjs

import com.chattriggers.ctjs.minecraft.libs.renderer.Image
import com.chattriggers.ctjs.utils.Initializer
import com.google.gson.Gson
import net.fabricmc.api.ClientModInitializer
import java.io.File
import java.net.URL
import java.net.URLConnection

class CTJS : ClientModInitializer {
    override fun onInitializeClient() {
        println("initialized!")
        Initializer.initializers.forEach(Initializer::init)
    }

    companion object {
        const val modulesFolder = "./config/ChatTriggers/modules"
        const val WEBSITE_ROOT = "https://www.chattriggers.com"
        internal val images = mutableListOf<Image>()

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
