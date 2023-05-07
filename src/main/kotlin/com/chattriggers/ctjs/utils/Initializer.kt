package com.chattriggers.ctjs.utils

import com.chattriggers.ctjs.engine.module.ModuleUpdater
import com.chattriggers.ctjs.minecraft.listeners.ClientListener
import com.chattriggers.ctjs.minecraft.listeners.WorldListener

internal interface Initializer {
    fun init()

    companion object {
        internal val initializers = listOf(ModuleUpdater, ClientListener, WorldListener)
    }
}
