package com.chattriggers.ctjs

import com.chattriggers.ctjs.engine.module.ModuleManager
import com.chattriggers.ctjs.minecraft.libs.ChatLib
import com.chattriggers.ctjs.minecraft.libs.renderer.Image
import com.chattriggers.ctjs.minecraft.objects.KeyBind
import com.chattriggers.ctjs.minecraft.objects.Sound
import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.minecraft.wrappers.World
import com.chattriggers.ctjs.triggers.TriggerType
import com.chattriggers.ctjs.utils.Config
import com.chattriggers.ctjs.utils.console.ConsoleManager
import com.chattriggers.ctjs.utils.console.printTraceToConsole
import kotlin.concurrent.thread

object Reference {
    // TODO: Figure out how to substitute these at build time
    const val MOD_ID = "chattriggers"
    const val MOD_NAME = "ChatTriggers"
    const val MOD_VERSION = "3.0.0"

    const val DEFAULT_MODULES_FOLDER = "./config/ChatTriggers/modules"

    var isLoaded = true

    @JvmStatic
    fun unloadCT(asCommand: Boolean = true) {
        TriggerType.WorldUnload.triggerAll()
        TriggerType.GameUnload.triggerAll()

        isLoaded = false

        ModuleManager.teardown()
        KeyBind.clearKeyBinds()
        ConsoleManager.clearConsoles()

        Client.scheduleTask {
            CTJS.images.forEach(Image::destroy)
            CTJS.sounds.forEach(Sound::destroy)
        }

        if (asCommand)
            ChatLib.chat("&7Unloaded ChatTriggers")
    }

    @JvmStatic
    fun loadCT(asCommand: Boolean = true) {
        Client.getMinecraft().options.write()
        unloadCT(asCommand = false)

        if (asCommand)
            ChatLib.chat("&cReloading ChatTriggers...")

        conditionalThread {
            ModuleManager.setup()
            ModuleManager.entryPass()

            Client.getMinecraft().options.load()
            if (asCommand)
                ChatLib.chat("&aDone reloading!")
            isLoaded = true

            TriggerType.GameLoad.triggerAll()
            if (World.isLoaded())
                TriggerType.WorldLoad.triggerAll()
        }
    }

    @JvmStatic
    fun conditionalThread(block: () -> Unit) {
        if (Config.threadedLoading) {
            thread {
                try {
                    block()
                } catch (e: Throwable) {
                    e.printTraceToConsole()
                }
            }
        } else {
            block()
        }
    }
}
