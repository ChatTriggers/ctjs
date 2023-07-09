package com.chattriggers.ctjs.commands

import com.chattriggers.ctjs.console.LogType
import com.chattriggers.ctjs.console.printToConsole
import com.chattriggers.ctjs.engine.js.JSLoader
import com.chattriggers.ctjs.utils.Initializer
import com.chattriggers.ctjs.utils.InternalApi
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import com.mojang.brigadier.tree.RootCommandNode
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

@InternalApi
interface Command {
    val overrideExisting: Boolean

    // Do not call this method! Instead, call the register() method in this command's CommandCollection
    fun registerImpl(): Registration

    data class Registration(
        val names: Set<String>,
        val builder: (String) -> LiteralArgumentBuilder<FabricClientCommandSource>,
    )
}

@InternalApi
abstract class CommandCollection : Initializer {
    private var dispatcher: CommandDispatcher<FabricClientCommandSource>? = null
    private val activeCommands = mutableMapOf<Command, Set<String>>()
    private val pendingCommands = mutableSetOf<Command>()

    override fun init() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            this.dispatcher = dispatcher
            activeCommands.keys.forEach(::register)
            pendingCommands.forEach(::register)
            pendingCommands.clear()
        }
    }

    fun register(command: Command) {
        if (dispatcher == null) {
            pendingCommands.add(command)
            return
        }

        val registeredNames = mutableSetOf<String>()
        val (names, builder) = command.registerImpl()
        for (name in names) {
            if (command.hasConflict(name)) {
                existingCommandWarning(name).printToConsole(JSLoader.console, LogType.WARN)
            } else {
                val node = dispatcher!!.register(builder(name))

                registeredNames.add(name)
            }
        }

        activeCommands[command] = registeredNames
    }

    fun unregister(command: Command) {
        if (command in pendingCommands) {
            pendingCommands.remove(command)
            return
        }

        val names = activeCommands[command]!!
        dispatcher!!.root.children.removeIf {
            it.name in names
        }

        activeCommands.remove(command)
    }

    fun unregisterAll() {
        pendingCommands.clear()
        activeCommands.keys.forEach(::unregister)
        activeCommands.clear()
    }

    fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.onExecute(block: (CommandContext<S>) -> Unit): T = this.executes {
        block(it)
        1
    }

    private fun Command.hasConflict(name: String) = !overrideExisting && dispatcher!!.root.getChild(name) != null

    private fun existingCommandWarning(name: String) =
        """
                Command with name $name already exists! This will not override the 
                other command with the same name. To override the other command, set the 
                overrideExisting flag in setName() (the second argument) to true.
            """.trimIndent().replace("\n", "")
}
