package com.chattriggers.ctjs.internal.utils

import com.chattriggers.ctjs.api.CustomCommand
import com.chattriggers.ctjs.internal.commands.CTCommand
import com.chattriggers.ctjs.internal.console.ConsoleHostProcess
import com.chattriggers.ctjs.internal.listeners.ClientListener

internal interface Initializer {
    fun init()

    companion object {
        internal val initializers = listOf(
            ClientListener,
            ConsoleHostProcess,
            CTCommand,
            CustomCommand
        )
    }
}
