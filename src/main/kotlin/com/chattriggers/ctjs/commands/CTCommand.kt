package com.chattriggers.ctjs.commands

import com.chattriggers.ctjs.Reference
import com.chattriggers.ctjs.commands.StaticCommand.Companion.onExecute
import com.chattriggers.ctjs.engine.module.ModuleManager
import com.chattriggers.ctjs.minecraft.libs.ChatLib
import com.chattriggers.ctjs.minecraft.listeners.ClientListener
import com.chattriggers.ctjs.minecraft.objects.Message
import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.utils.Config
import com.chattriggers.ctjs.console.ConsoleManager
import com.chattriggers.ctjs.console.printTraceToConsole
import com.chattriggers.ctjs.engine.js.JSLoader
import com.chattriggers.ctjs.engine.module.ModulesGui
import com.chattriggers.ctjs.minecraft.objects.TextComponent
import com.chattriggers.ctjs.utils.Initializer
import com.chattriggers.ctjs.utils.toVersion
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import gg.essential.universal.UDesktop
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import java.io.IOException
import kotlin.concurrent.thread

internal object CTCommand : Initializer {
    private const val idFixed = 90123 // ID for dumped chat
    private var idFixedOffset = -1 // ID offset (increments)

    override fun init() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            register(dispatcher)
        }
    }

    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        val command = literal("ct")
            .then(literal("load").onExecute { Reference.loadCT(asCommand = true) })
            .then(literal("unload").onExecute { Reference.unloadCT(asCommand = true) })
            .then(literal("files").onExecute { openFileLocation() })
            .then(
                literal("import")
                    .then(argument("module", StringArgumentType.string())
                        .onExecute { import(StringArgumentType.getString(it, "module")) })
            )
            .then(
                literal("delete")
                    .then(argument("module", StringArgumentType.string())
                        .onExecute {
                            val module = StringArgumentType.getString(it, "module")
                            if (ModuleManager.deleteModule(module)) {
                                ChatLib.chat("&aDeleted")
                            } else ChatLib.chat("&cFailed to delete $module")
                        })
            )
            .then(literal("modules").onExecute { Client.currentGui.set(ModulesGui) })
            .then(
                literal("console")
                    .then(literal("js").onExecute { JSLoader.console.show() })
                    .then(literal("general").onExecute { ConsoleManager.generalConsole.show() })
                    .onExecute { JSLoader.console.show() }
            )
            .then(literal("config").onExecute { Client.currentGui.set(Config.gui()!!) })
            .then(
                literal("simulate")
                    .then(
                        argument("message", StringArgumentType.greedyString())
                            .onExecute { ChatLib.simulateChat(StringArgumentType.getString(it, "message")) }
                    )
            )
            .then(
                literal("dump")
                    .then(
                        argument("type", StringArgumentType.word())
                            .then(
                                argument("amount", IntegerArgumentType.integer(0))
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
                    .onExecute { dump(DumpType.CHAT) }
            )
            .onExecute { ChatLib.chat(getUsage()) }

        dispatcher.register(command)
    }

    private fun import(moduleName: String) {
        if (ModuleManager.cachedModules.any { it.name.equals(moduleName, ignoreCase = true) }) {
            ChatLib.chat("&cModule $moduleName is already installed!")
        } else {
            ChatLib.chat("&cImporting ${moduleName}...")
            thread {
                val (module, dependencies) = ModuleManager.importModule(moduleName)
                if (module == null) {
                    ChatLib.chat("&cUnable to import module $moduleName")
                    return@thread
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
            UDesktop.browse(ModuleManager.modulesFolder.toURI())
        } catch (exception: IOException) {
            exception.printTraceToConsole()
            ChatLib.chat("&cCould not open file location")
        }
    }

    private fun dump(type: DumpType, lines: Int = 100) {
        clearOldDump()

        val messages = type.messageList()
        val toDump = lines.coerceAtMost(messages.size)
        Message("&6&m${ChatLib.getChatBreak()}").setChatLineId(idFixed).chat()

        for (i in 0 until toDump) {
            val msg = ChatLib.replaceFormatting(messages[messages.size - toDump + i].formattedText)
            Message(
                TextComponent(msg)
                    .setClick(ClickEvent.Action.COPY_TO_CLIPBOARD, msg)
                    .setHover(HoverEvent.Action.SHOW_TEXT, TextComponent("&eClick here to copy this message."))
                    .setFormatted(true)
            ).setFormatted(false)
                .setChatLineId(idFixed + i + 1)
                .chat()
        }

        Message("&6&m${ChatLib.getChatBreak()}").setChatLineId(idFixed + lines + 1).chat()

        idFixedOffset = idFixed + lines + 1
    }

    private fun clearOldDump() {
        if (idFixedOffset == -1) return
        while (idFixedOffset >= idFixed)
            ChatLib.deleteChat(idFixedOffset--)
        idFixedOffset = -1
    }

    enum class DumpType(val messageList: () -> List<TextComponent>) {
        CHAT(ClientListener::chatHistory),
        ACTION_BAR(ClientListener::actionBarHistory);

        companion object {
            fun fromString(str: String) = DumpType.values().first { it.name.equals(str, ignoreCase = true) }
        }
    }
}
