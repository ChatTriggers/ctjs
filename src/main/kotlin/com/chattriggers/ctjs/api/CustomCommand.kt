package com.chattriggers.ctjs.api

import com.chattriggers.ctjs.engine.LogType
import com.chattriggers.ctjs.engine.printToConsole
import com.chattriggers.ctjs.internal.engine.JSLoader
import com.chattriggers.ctjs.internal.mixins.CommandContextAccessor
import com.chattriggers.ctjs.internal.mixins.CommandNodeAccessor
import com.chattriggers.ctjs.internal.utils.Initializer
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.commands.CommandSource
import org.mozilla.javascript.Function
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.ScriptableObject

object CustomCommand : Initializer {
    private var commands: MutableSet<Pair<String, (NodeBuilder) -> Any>> = mutableSetOf()
    private var clientDispatcher: CommandDispatcher<CommandSource>? = null
    private var networkDispatcher: CommandDispatcher<CommandSource>? = null

    @Suppress("UNCHECKED_CAST")
    override fun init() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            this.clientDispatcher = dispatcher as CommandDispatcher<CommandSource>
            registerAll(dispatcher)
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            clientDispatcher = null
            networkDispatcher = null
        }
    }

    internal fun registerNetwork(dispatcher: CommandDispatcher<CommandSource>) {
        networkDispatcher = dispatcher
        registerAll(dispatcher)
    }

    internal fun registerAll(dispatcher: CommandDispatcher<CommandSource>) {
        for ((name, builder) in commands) {
            val cmd = CommandBuilder(name).apply { builder.invoke(builder()) }
            dispatcher.register(cmd.build())
        }
    }

    internal fun unregisterAll() {
        for (dispatcher in listOfNotNull(clientDispatcher, networkDispatcher)) {
            for ((name, _) in commands) {
                (dispatcher.root as CommandNodeAccessor).let {
                    it.children.remove(name)
                    it.literals.remove(name)
                }
            }
        }

        commands.clear()
    }

    @JvmStatic
    fun register(name: String, callback: (NodeBuilder) -> Any) {
        commands.add(name to callback)

        if (clientDispatcher?.root?.getChild(name) != null || networkDispatcher?.root?.getChild(name) != null) {
            "Command with $name already exists".printToConsole(LogType.WARN)
        } else {
            val cmd = CommandBuilder(name).apply { callback.invoke(builder()) }
            clientDispatcher?.register(cmd.build())
            networkDispatcher?.register(cmd.build())
        }
    }

    class CommandBuilder(val name: String) {
        private val root = LiteralArgumentBuilder.literal<CommandSource>(name)

        fun builder() = NodeBuilder(root)

        fun build(): LiteralArgumentBuilder<CommandSource> = root
    }

    open class NodeBuilder(val node: ArgumentBuilder<CommandSource, *>) {
        fun literal(s: String, callback: Function): NodeBuilder {
            val next = LiteralArgumentBuilder.literal<CommandSource>(s)
            JSLoader.invoke(callback, arrayOf(NodeBuilder(next)))
            node.then(next)
            return this
        }

        fun <T> argument(name: String, type: ArgumentType<T>, callback: Function): NodeBuilder {
            val next = RequiredArgumentBuilder.argument<CommandSource, T>(name, type)
            JSLoader.invoke(callback, arrayOf(NodeBuilder(next)))
            node.then(next)
            return this
        }

        fun exec(callback: Function) {
            node.executes { ctx ->
                val obj = NativeObject()

                (ctx as CommandContextAccessor).arguments.forEach { (string, argument) ->
                    ScriptableObject.putProperty(obj, string, argument.result)
                }

                JSLoader.invoke(callback, arrayOf(ctx, obj))
                1
            }
        }
    }
}
