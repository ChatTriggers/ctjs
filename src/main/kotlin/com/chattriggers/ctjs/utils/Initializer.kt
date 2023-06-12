package com.chattriggers.ctjs.utils

import com.chattriggers.ctjs.engine.module.ModuleUpdater
import com.chattriggers.ctjs.minecraft.listeners.ClientListener
import com.chattriggers.ctjs.minecraft.listeners.MouseListener
import com.chattriggers.ctjs.minecraft.listeners.WorldListener
import com.chattriggers.ctjs.minecraft.objects.KeyBind
import com.chattriggers.ctjs.minecraft.objects.display.Display
import com.chattriggers.ctjs.minecraft.wrappers.CPS
import com.chattriggers.ctjs.console.ConsoleManager

internal interface Initializer {
    fun init()

    companion object {
        internal val initializers = listOf(
            ClientListener,
            ConsoleManager,
            CPS,
            Display.Companion,
            KeyBind.Companion,
            ModuleUpdater,
            MouseListener,
            WorldListener,
        )
    }
}
