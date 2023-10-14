package com.chattriggers.ctjs.internal.commands

import com.chattriggers.ctjs.internal.console.LogType
import com.chattriggers.ctjs.engine.printToConsole
import com.chattriggers.ctjs.internal.engine.CTEvents
import com.chattriggers.ctjs.internal.engine.JSLoader
import com.chattriggers.ctjs.internal.utils.Initializer
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents

abstract class CommandCollection : Initializer {
    private val allCommands = mutableSetOf<Command>()

    private var clientDispatcher: CommandDispatcher<FabricClientCommandSource>? = null
    private var networkDispatcher: CommandDispatcher<FabricClientCommandSource>? = null

    override fun init() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            clientDispatcher = dispatcher
            allCommands.forEach { it.registerImpl(dispatcher) }
        }

        CTEvents.NETWORK_COMMAND_DISPATCHER_REGISTER.register { dispatcher ->
            networkDispatcher = dispatcher
            allCommands.forEach { it.registerImpl(dispatcher) }
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            clientDispatcher = null
            networkDispatcher = null
        }
    }

    fun register(command: Command) {
        allCommands.add(command)
        if (clientDispatcher.hasConflict(command) || networkDispatcher.hasConflict(command)) {
            existingCommandWarning(command.name).printToConsole(LogType.WARN)
        } else {
            clientDispatcher?.let { command.registerImpl(it) }
            networkDispatcher?.let { command.registerImpl(it) }
        }
    }

    fun unregister(command: Command) {
        for (dispatcher in listOfNotNull(clientDispatcher, networkDispatcher))
            command.unregisterImpl(dispatcher)
    }

    fun unregisterAll() {
        allCommands.forEach(::unregister)
        allCommands.clear()
    }

    fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.onExecute(block: (CommandContext<S>) -> Unit): T =
        executes {
            block(it)
            1
        }

    private fun CommandDispatcher<*>?.hasConflict(command: Command) =
        !command.overrideExisting && (this?.root?.getChild(command.name) != null)

    private fun existingCommandWarning(name: String) =
        """
        Command with name $name already exists! This will not override the 
        other command with the same name. To override the other command, set the 
        overrideExisting flag in setName() (the second argument) to true.
        """.trimIndent().replace("\n", "")
}
