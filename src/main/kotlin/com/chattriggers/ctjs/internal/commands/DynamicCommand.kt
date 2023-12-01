package com.chattriggers.ctjs.internal.commands

import com.chattriggers.ctjs.api.commands.DynamicCommands
import com.chattriggers.ctjs.api.commands.RootCommand
import com.chattriggers.ctjs.internal.CTClientCommandSource
import com.chattriggers.ctjs.internal.engine.JSLoader
import com.chattriggers.ctjs.internal.mixins.commands.CommandContextAccessor
import com.chattriggers.ctjs.internal.utils.asMixin
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.minecraft.command.CommandSource
import org.mozilla.javascript.Function
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.ScriptableObject

object DynamicCommand {
    sealed class Node(val parent: Node?) {
        var method: Function? = null
        var hasRedirect = false
        val children = mutableListOf<Node>()
        var builder: ArgumentBuilder<CommandSource, *>? = null

        open class Literal(parent: Node?, val name: String) : Node(parent)

        class Root(name: String) : Literal(null, name), RootCommand {
            var commandNode: LiteralCommandNode<CommandSource>? = null

            override fun register() {
                DynamicCommands.register(CommandImpl(this))
            }
        }

        class Argument(parent: Node?, val name: String, val type: ArgumentType<*>) : Node(parent)

        class Redirect(parent: Node?, val target: Root) : Node(parent)

        class RedirectToCommandNode(parent: Node?, val target: CommandNode<CommandSource>) : Node(parent)

        fun initialize(dispatcher: CommandDispatcher<CommandSource>) {
            if (this is Redirect) {
                check(method == null)
                check(children.isEmpty())
                target.initialize(dispatcher)

                parent!!.builder!!.redirect(target.commandNode) {
                    for ((name, arg) in it.asMixin<CommandContextAccessor>().arguments)
                        it.source.asMixin<CTClientCommandSource>().setContextValue(name, arg.result)
                    it.source
                }

                return
            }

            if (this is RedirectToCommandNode) {
                check(method == null)
                check(children.isEmpty())

                parent!!.builder!!.redirect(target) {
                    for ((name, arg) in it.asMixin<CommandContextAccessor>().arguments)
                        it.source.asMixin<CTClientCommandSource>().setContextValue(name, arg.result)
                    it.source
                }

                return
            }

            builder = when (this) {
                is Literal -> literal(name)
                is Argument -> argument(name, type)
                else -> throw IllegalStateException("unreachable")
            }

            // The call to .then() below builds a node which check the command, so we
            // need to call .execute() and child..initialize() before then if necessary
            if (method != null) {
                builder!!.executes { ctx ->
                    val obj = NativeObject()

                    for ((key, value) in ctx.source.asMixin<CTClientCommandSource>().contextValues)
                        ScriptableObject.putProperty(obj, key, value)
                    for ((key, arg) in ctx.asMixin<CommandContextAccessor>().arguments)
                        ScriptableObject.putProperty(obj, key, arg.result)

                    ctx.source.asMixin<CTClientCommandSource>().contextValues.clear()

                    JSLoader.invoke(method!!, arrayOf(obj))
                    1
                }
            }

            for (child in children)
                child.initialize(dispatcher)

            parent?.builder?.then(builder)
        }
    }

    class CommandImpl(private val node: Node.Root) : Command {
        override val overrideExisting = true
        override val name = node.name

        override fun registerImpl(dispatcher: CommandDispatcher<CommandSource>) {
            node.initialize(dispatcher)
            val builder = node.builder!! as LiteralArgumentBuilder<CommandSource>
            node.commandNode = dispatcher.register(builder)
        }
    }
}
