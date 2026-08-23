package com.chattriggers.ctjs.internal.commands

import com.chattriggers.ctjs.engine.LogType
import com.chattriggers.ctjs.engine.printToConsole
import com.chattriggers.ctjs.internal.engine.CTEvents
import com.chattriggers.ctjs.internal.utils.Initializer
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.commands.SharedSuggestionProvider

abstract class CommandCollection : Initializer {
    private val allCommands = mutableSetOf<Command>()

    private var clientDispatcher: CommandDispatcher<SharedSuggestionProvider>? = null
    private var networkDispatcher: CommandDispatcher<SharedSuggestionProvider>? = null

    @Suppress("UNCHECKED_CAST")
    override fun init() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            attachClientDispatcher(dispatcher as CommandDispatcher<SharedSuggestionProvider>)
        }

        CTEvents.NETWORK_COMMAND_DISPATCHER_REGISTER.register { dispatcher ->
            attachNetworkDispatcher(dispatcher as CommandDispatcher<SharedSuggestionProvider>)
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            detachDispatchers()
        }
    }

    fun register(command: Command) {
        if (!allCommands.add(command))
            return
        listOfNotNull(clientDispatcher, networkDispatcher).forEach { dispatcher ->
            if (dispatcher.hasConflict(command))
                warnConflict(command.name)
            else
                command.registerImpl(dispatcher)
        }
    }

    fun unregister(command: Command) {
        allCommands.remove(command)
        for (dispatcher in listOfNotNull(clientDispatcher, networkDispatcher))
            command.unregisterImpl(dispatcher)
    }

    fun unregisterAll() {
        val commands = allCommands.toList()
        allCommands.clear()
        for (dispatcher in listOfNotNull(clientDispatcher, networkDispatcher))
            commands.forEach { it.unregisterImpl(dispatcher) }
    }

    internal fun registeredCount(): Int = synchronized(allCommands) { allCommands.size }

    internal fun attachClientDispatcher(dispatcher: CommandDispatcher<SharedSuggestionProvider>) {
        clientDispatcher = dispatcher
        registerExisting(dispatcher)
    }

    internal fun attachNetworkDispatcher(dispatcher: CommandDispatcher<SharedSuggestionProvider>) {
        networkDispatcher = dispatcher
        registerExisting(dispatcher)
    }

    internal fun detachDispatchers() {
        clientDispatcher = null
        networkDispatcher = null
    }

    private fun registerExisting(dispatcher: CommandDispatcher<SharedSuggestionProvider>) {
        allCommands.forEach { command ->
            if (dispatcher.hasConflict(command)) {
                warnConflict(command.name)
            } else {
                command.registerImpl(dispatcher)
            }
        }
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

    protected open fun warnConflict(name: String) {
        existingCommandWarning(name).printToConsole(LogType.WARN)
    }
}
