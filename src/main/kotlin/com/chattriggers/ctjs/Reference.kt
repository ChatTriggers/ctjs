package com.chattriggers.ctjs

import com.chattriggers.ctjs.commands.DynamicCommands
import com.chattriggers.ctjs.commands.StaticCommand
import com.chattriggers.ctjs.console.ConsoleManager
import com.chattriggers.ctjs.engine.Register
import com.chattriggers.ctjs.engine.module.ModuleManager
import com.chattriggers.ctjs.minecraft.libs.ChatLib
import com.chattriggers.ctjs.minecraft.libs.renderer.Image
import com.chattriggers.ctjs.minecraft.objects.KeyBind
import com.chattriggers.ctjs.minecraft.objects.Sound
import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.minecraft.wrappers.World
import com.chattriggers.ctjs.triggers.TriggerType
import kotlin.concurrent.thread

object Reference {
    const val MOD_VERSION = "3.0.0-beta"
    const val MODULES_FOLDER = "./config/ChatTriggers/modules"

    var isLoaded = true
        private set

    fun unloadCT(asCommand: Boolean = true) {
        TriggerType.WORLD_UNLOAD.triggerAll()
        TriggerType.GAME_UNLOAD.triggerAll()

        isLoaded = false

        ModuleManager.teardown()
        KeyBind.clearKeyBinds()
        ConsoleManager.clearConsoles()
        Register.clearCustomTriggers()
        StaticCommand.unregisterAll()
        DynamicCommands.unregisterAll()

        Client.scheduleTask {
            CTJS.images.forEach(Image::destroy)
            CTJS.sounds.forEach(Sound::destroy)

            CTJS.images.clear()
            CTJS.sounds.clear()
        }

        if (asCommand)
            ChatLib.chat("&7Unloaded ChatTriggers")
    }

    fun loadCT(asCommand: Boolean = true) {
        Client.getMinecraft().options.write()
        unloadCT(asCommand = false)

        if (asCommand)
            ChatLib.chat("&cReloading ChatTriggers...")

        thread {
            ModuleManager.setup()
            ModuleManager.entryPass()

            Client.getMinecraft().options.load()
            if (asCommand)
                ChatLib.chat("&aDone reloading!")
            isLoaded = true

            TriggerType.GAME_LOAD.triggerAll()
            if (World.isLoaded())
                TriggerType.WORLD_LOAD.triggerAll()
        }
    }
}
