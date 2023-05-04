package com.chattriggers.ctjs

import net.fabricmc.api.ClientModInitializer
import java.io.File

class CTJS : ClientModInitializer {
    override fun onInitializeClient() {
        println("initialized!")
    }

    companion object {
        val configLocation = File("./config")
        const val modulesFolder = "./config/ChatTriggers/modules"
    }
}
