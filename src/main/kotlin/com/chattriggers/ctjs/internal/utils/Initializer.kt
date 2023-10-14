package com.chattriggers.ctjs.internal.utils

import com.chattriggers.ctjs.api.client.CPS
import com.chattriggers.ctjs.api.client.KeyBind
import com.chattriggers.ctjs.api.commands.DynamicCommands
import com.chattriggers.ctjs.internal.commands.CTCommand
import com.chattriggers.ctjs.internal.commands.StaticCommand
import com.chattriggers.ctjs.engine.Console
import com.chattriggers.ctjs.internal.engine.module.ModuleUpdater
import com.chattriggers.ctjs.internal.listeners.ClientListener
import com.chattriggers.ctjs.internal.listeners.MouseListener
import com.chattriggers.ctjs.internal.listeners.WorldListener

internal interface Initializer {
    fun init()

    companion object {
        internal val initializers = listOf(
            ClientListener,
            Console,
            CPS,
            CTCommand,
            DynamicCommands,
            KeyBind,
            ModuleUpdater,
            MouseListener,
            StaticCommand,
            WorldListener,
        )
    }
}
