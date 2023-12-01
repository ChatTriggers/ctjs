package com.chattriggers.ctjs.internal.commands

import com.chattriggers.ctjs.api.triggers.CommandTrigger
import com.chattriggers.ctjs.internal.mixins.commands.CommandNodeAccessor
import com.chattriggers.ctjs.internal.utils.asMixin
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.command.CommandSource

internal class StaticCommand(
    val trigger: CommandTrigger,
    override val name: String,
    private val aliases: Set<String>,
    override val overrideExisting: Boolean,
    private val staticSuggestions: List<String>,
    private val dynamicSuggestions: ((List<String>) -> List<String>)?,
) : Command {
    override fun registerImpl(dispatcher: CommandDispatcher<CommandSource>) {
        val builder = literal(name)
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

        val node = dispatcher.register(builder)

        // Can't use .redirect() since it doesn't work without arguments
        for (alias in aliases) {
            val aliasNode = literal(alias)
            node.children.forEach {
                aliasNode.then(it)
            }
            aliasNode.executes(node.command)

            dispatcher.register(aliasNode)
        }
    }

    override fun unregisterImpl(dispatcher: CommandDispatcher<CommandSource>) {
        super.unregisterImpl(dispatcher)
        dispatcher.root.asMixin<CommandNodeAccessor>().apply {
            for (alias in aliases) {
                childNodes.remove(alias)
                literals.remove(alias)
            }
        }
    }

    companion object : CommandCollection()
}
