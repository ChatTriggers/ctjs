package com.chattriggers.ctjs.utils

import com.chattriggers.ctjs.commands.CTCommand
import com.chattriggers.ctjs.commands.StaticCommand
import com.chattriggers.ctjs.console.ConsoleManager
import com.chattriggers.ctjs.engine.module.ModuleUpdater
import com.chattriggers.ctjs.minecraft.libs.renderer.Image
import com.chattriggers.ctjs.minecraft.listeners.ClientListener
import com.chattriggers.ctjs.minecraft.listeners.MouseListener
import com.chattriggers.ctjs.minecraft.listeners.WorldListener
import com.chattriggers.ctjs.minecraft.objects.KeyBind
import com.chattriggers.ctjs.minecraft.wrappers.CPS

internal interface Initializer {
    fun init()

    companion object {
        internal val initializers = listOf(
            ClientListener,
            ConsoleManager,
            CPS,
            CTCommand,
            Image.Companion,
            KeyBind.Companion,
            ModuleUpdater,
            MouseListener,
            StaticCommand,
            WorldListener,
        )
    }
}
