package com.chattriggers.ctjs.commands

import com.chattriggers.ctjs.Reference
import com.chattriggers.ctjs.engine.module.ModuleManager
import com.chattriggers.ctjs.minecraft.libs.ChatLib
import com.chattriggers.ctjs.minecraft.listeners.ClientListener
import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.utils.Config
import com.chattriggers.ctjs.utils.console.Console.Companion.printTraceToConsole
import com.chattriggers.ctjs.utils.toVersion
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import gg.essential.universal.wrappers.message.UMessage
import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import java.awt.Desktop
import java.io.File
import java.io.IOException

object CTCommand {
    private const val idFixed = 90123 // ID for dumped chat
    private var idFixedOffset = -1 // ID offset (increments)

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        // TODO(breaking): Remove copy command and a bunch of aliases
        val command = CommandManager.literal("ct")
            .then(CommandManager.literal("load").onExecute { Reference.loadCT() })
            .then(CommandManager.literal("unload").onExecute { Reference.unloadCT() })
            .then(CommandManager.literal("files").onExecute { openFileLocation() })
            .then(
                CommandManager.literal("import")
                    .then(CommandManager.argument("module", StringArgumentType.string())
                        .onExecute { import(StringArgumentType.getString(it, "module")) })
            )
            .then(
                CommandManager.literal("delete")
                    .then(CommandManager.argument("module", StringArgumentType.string())
                        .onExecute {
                            val module = StringArgumentType.getString(it, "module")
                            if (ModuleManager.deleteModule(module)) {
                                ChatLib.chat("&aDeleted")
                            } else ChatLib.chat("&cFailed to delete $module")
                        })
            )
            .then(CommandManager.literal("modules").onExecute { TODO() })
            .then(
                CommandManager.literal("console")
                    .then(CommandManager.argument("type", StringArgumentType.word())
                        .onExecute {
                            ModuleManager.getConsole(StringArgumentType.getString(it, "type")).showConsole()
                        })
                    .onExecute { ModuleManager.generalConsole.showConsole() }
            )
            .then(CommandManager.literal("config").onExecute { Client.Companion.currentGui.set(Config.gui()!!) })
            .then(
                CommandManager.literal("simulate")
                    .then(
                        CommandManager.argument("message", StringArgumentType.greedyString())
                            .onExecute { ChatLib.simulateChat(StringArgumentType.getString(it, "message")) }
                    )
            )
            .then(
                CommandManager.literal("dump")
                    .then(
                        CommandManager.argument("type", StringArgumentType.word())
                            .then(
                                CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                    .onExecute {
                                        dump(
                                            DumpType.fromString(StringArgumentType.getString(it, "type")),
                                            IntegerArgumentType.getInteger(it, "amount")
                                        )
                                    }
                            ).onExecute {
                                dump(DumpType.fromString(StringArgumentType.getString(it, "type")))
                            }
                    )
            )

        dispatcher.register(command)
    }

    private fun import(moduleName: String) {
        if (ModuleManager.cachedModules.any { it.name.equals(moduleName, ignoreCase = true) }) {
            ChatLib.chat("&cModule $moduleName is already installed!")
        } else {
            ChatLib.chat("&cImporting ${moduleName}...")
            Reference.conditionalThread {
                val (module, dependencies) = ModuleManager.importModule(moduleName)
                if (module == null) {
                    ChatLib.chat("&cUnable to import module $moduleName")
                    return@conditionalThread
                }

                val allModules = listOf(module) + dependencies
                val modVersion = Reference.MOD_VERSION.toVersion()
                allModules.forEach {
                    val version = it.targetModVersion ?: return@forEach
                    if (version.majorVersion < modVersion.majorVersion)
                        ModuleManager.tryReportOldVersion(it)
                }

                ChatLib.chat("&aSuccessfully imported ${module.metadata.name ?: module.name}")
                if (Config.moduleImportHelp && module.metadata.helpMessage != null) {
                    ChatLib.chat(module.metadata.helpMessage.toString().take(383))
                }
            }
        }
    }

    private fun getUsage() = """
        &b&m${ChatLib.getChatBreak()}
        &c/ct load &7- &oReloads all of the ChatTriggers modules.
        &c/ct import [module] &7- &oImports a module.
        &c/ct delete [module] &7- &oDeletes a module.
        &c/ct files &7- &oOpens the ChatTriggers folder.
        &c/ct modules &7- &oOpens the modules GUI.
        &c/ct console [language] &7- &oOpens the ChatTriggers console.
        &c/ct simulate [message] &7- &oSimulates a received chat message.
        &c/ct dump &7- &oDumps previous chat messages into chat.
        &c/ct settings &7- &oOpens the ChatTriggers settings.
        &c/ct &7- &oDisplays this help dialog.
        &b&m${ChatLib.getChatBreak()}
    """.trimIndent()

    private fun openFileLocation() {
        try {
            Desktop.getDesktop().open(File("./config/ChatTriggers"))
        } catch (exception: IOException) {
            exception.printTraceToConsole()
            ChatLib.chat("&cCould not open file location")
        }
    }

    private fun dump(type: DumpType, lines: Int = 100) {
        clearOldDump()

        val messages = type.messageList()
        var toDump = lines
        if (toDump > messages.size) toDump = messages.size
        UMessage("&6&m${ChatLib.getChatBreak()}").apply { chatLineId = idFixed }.chat()
        var msg: String
        for (i in 0 until toDump) {
            msg = messages[messages.size - toDump + i].unformattedText
            UMessage(
                UTextComponent(msg)
                    .setClick(ClickEvent.Action.RUN_COMMAND, "/ct copy $msg")
                    .setHover(HoverEvent.Action.SHOW_TEXT, ChatLib.addColor("&eClick here to copy this message."))
                    .apply { formatted = false }
            ).apply {
                isFormatted = false
                chatLineId = idFixed + i + 1
            }
        }
        UMessage("&6&m${ChatLib.getChatBreak()}").apply { chatLineId = idFixed + lines + 1 }.chat()

        idFixedOffset = idFixed + lines + 1
    }

    private fun clearOldDump() {
        if (idFixedOffset == -1) return
        while (idFixedOffset >= idFixed)
            ChatLib.clearChat(idFixedOffset--)
        idFixedOffset = -1
    }

    private fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.onExecute(block: (CommandContext<S>) -> Unit) =
        apply {
            this.executes {
                block(it)
                1
            }
        }

    enum class DumpType(val messageList: () -> List<UTextComponent>) {
        Chat(ClientListener::chatHistory),
        ActionBar(ClientListener::actionBarHistory);

        companion object {
            fun fromString(str: String) = DumpType.values().first { it.name.lowercase() == str }
        }
    }
}
