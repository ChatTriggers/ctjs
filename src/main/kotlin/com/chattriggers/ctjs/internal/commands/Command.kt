package com.chattriggers.ctjs.internal.commands

import com.chattriggers.ctjs.internal.mixins.commands.CommandNodeAccessor
import com.chattriggers.ctjs.internal.utils.asMixin
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.minecraft.command.CommandSource

interface Command {
    val overrideExisting: Boolean
    val name: String

    fun registerImpl(dispatcher: CommandDispatcher<CommandSource>)

    fun unregisterImpl(dispatcher: CommandDispatcher<CommandSource>) {
        dispatcher.root.asMixin<CommandNodeAccessor>().apply {
            childNodes.remove(name)
            literals.remove(name)
        }
    }
}

fun literal(name: String) = LiteralArgumentBuilder.literal<CommandSource>(name)

fun <T> argument(name: String, argument: ArgumentType<T>) =
    RequiredArgumentBuilder.argument<CommandSource, T>(name, argument)
