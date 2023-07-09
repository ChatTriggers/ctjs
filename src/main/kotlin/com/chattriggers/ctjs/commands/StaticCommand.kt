package com.chattriggers.ctjs.commands

import com.chattriggers.ctjs.triggers.CommandTrigger
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal

internal class StaticCommand(
    val trigger: CommandTrigger,
    private val names: Set<String>,
    override val overrideExisting: Boolean,
    private val staticSuggestions: List<String>,
    private val dynamicSuggestions: ((List<String>) -> List<String>)?,
) : Command {
    override fun registerImpl(): Command.Registration {
        return Command.Registration(names) {
            literal(it)
                .then(argument("args", StringArgumentType.greedyString())
                    .suggests { ctx, builder ->
                        val suggestions = if (dynamicSuggestions != null) {
                            val args = try {
                                StringArgumentType.getString(ctx, "args").split(" ")
                            } catch (e: IllegalArgumentException) {
                                emptyList()
                            }

                            // Kotlin compiler bug: Without this null assert, it complains that the receiver is
                            // nullable, but with it, it says it's unnecessary.
                            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
                            dynamicSuggestions!!(args)
                        } else staticSuggestions

                        for (suggestion in suggestions)
                            builder.suggest(suggestion)

                        builder.buildFuture()
                    }
                    .onExecute {
                        trigger.trigger(StringArgumentType.getString(it, "args").split(" ").toTypedArray())
                    })
                .onExecute { trigger.trigger(emptyArray()) }
        }
    }

    companion object : CommandCollection()
}
