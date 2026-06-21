package com.chattriggers.ctjs.internal.commands

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.api.FileLib
import com.chattriggers.ctjs.engine.Console
import com.chattriggers.ctjs.internal.engine.module.ModuleListScreen
import com.chattriggers.ctjs.internal.engine.module.ModuleManager
import com.chattriggers.ctjs.internal.listeners.ClientListener
import com.chattriggers.ctjs.internal.utils.Initializer
import com.chattriggers.ctjs.internal.utils.onExecute
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component
import java.util.concurrent.CompletableFuture

internal object CTCommand : Initializer {
    private val mc = Minecraft.getInstance()

    override fun init() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            register(dispatcher)
        }
    }

    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        val command = literal("ct")
            .then(literal("load").onExecute { CTJS.load() })
            .then(literal("unload").onExecute { CTJS.unload() })
            .then(literal("files").onExecute { FileLib.openModulesFolder() })
            .then(
                literal("delete")
                    .then(
                        argument("module", ModuleArgumentType)
                            .onExecute {
                                val module = ModuleArgumentType.getModule(it, "module")
                                if (ModuleManager.deleteModule(module)) {
                                    mc.player?.sendSystemMessage(Component.nullToEmpty("&aDeleted $module"))
                                } else mc.player?.sendSystemMessage(Component.nullToEmpty("&cFailed to delete $module"))
                            })
            )
            .then(literal("console").onExecute { Console.show() })
            .then(
                literal("simulate")
                    .then(
                        argument("message", StringArgumentType.greedyString())
                            .onExecute {
                                val msg = StringArgumentType.getString(it, "message")
                                mc.chatListener.handleSystemMessage(Component.literal(msg), false)
                            }
                    )
            )
            .then(literal("modules").onExecute {
                ClientListener.addTask(0) {
                    mc.setScreen(ModuleListScreen())
                }
            })

        dispatcher.register(command)
    }

    private object ModuleArgumentType : ArgumentType<String> {
        override fun parse(reader: StringReader): String {
            val string = reader.readUnquotedString()
            val modules = ModuleManager.cachedModules.map { it.name }

            return modules.find {
                it.equals(string, ignoreCase = true)
            } ?: throw SimpleCommandExceptionType(Component.literal("No modules found with name \"$string\""))
                .createWithContext(reader)
        }

        override fun <S : Any?> listSuggestions(
            context: CommandContext<S>?,
            builder: SuggestionsBuilder
        ): CompletableFuture<Suggestions> {
            return SharedSuggestionProvider.suggest(ModuleManager.cachedModules.map { it.name }, builder)
        }

        fun getModule(ctx: CommandContext<FabricClientCommandSource>, module: String): String {
            return ctx.getArgument(module, String::class.java)
        }
    }
}
