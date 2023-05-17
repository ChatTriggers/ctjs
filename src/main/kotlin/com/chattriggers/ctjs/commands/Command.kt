package com.chattriggers.ctjs.commands

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.triggers.CommandTrigger
import com.chattriggers.ctjs.utils.console.LogType
import com.chattriggers.ctjs.utils.console.printToConsole
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal

// TODO: Tab completions
class Command(
    val trigger: CommandTrigger,
    val name: String,
    private val aliases: Set<String>,
    private val overrideExisting: Boolean,
) {
    private val registeredAliases = mutableSetOf<String>()

    fun register() {
        val dispatcher = CTJS.commandDispatcher
        if (dispatcher == null) {
            pendingCommands.add(this)
            return
        }

        val command = literal(name)
            .then(argument("args", StringArgumentType.greedyString())
                .onExecute {
                    trigger.trigger(StringArgumentType.getString(it, "args").split(" ").toTypedArray())
                })
            .onExecute { trigger.trigger(emptyArray()) }

        if (hasConflict(name)) {
            existingCommandWarning(name).printToConsole(trigger.loader.console, LogType.WARN)
            return
        }

        val registeredCommand = dispatcher.register(command)
        activeCommands.add(this)

        for (name in aliases) {
            if (hasConflict(name)) {
                existingCommandWarning(name).printToConsole(trigger.loader.console, LogType.WARN)
            } else {
                dispatcher.register(literal(name).redirect(registeredCommand))
                registeredAliases.add(name)
            }
        }
    }

    fun unregister() {
        CTJS.commandDispatcher!!.root.children.removeIf {
            it.name == name || it.name in registeredAliases
        }

        activeCommands.remove(this)
        registeredAliases.clear()
    }

    private fun hasConflict(name: String) = !overrideExisting && CTJS.commandDispatcher!!.root.getChild(name) != null

    companion object {
        internal val activeCommands = mutableSetOf<Command>()
        internal val pendingCommands = mutableSetOf<Command>()

        private fun existingCommandWarning(name: String) =
            """
                Command with name $name already exists! This will not override the 
                other command with the same name. To override the other command, set the 
                overrideExisting flag in setName() (the second argument) to true.
            """.trimIndent().replace("\n", "")

        fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.onExecute(block: (CommandContext<S>) -> Unit) = this.executes {
                block(it)
                1
            }
    }
}
