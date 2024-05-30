package com.chattriggers.ctjs.internal.commands

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.api.Config
import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.message.ChatLib
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.internal.commands.StaticCommand.Companion.onExecute
import com.chattriggers.ctjs.engine.Console
import com.chattriggers.ctjs.engine.printTraceToConsole
import com.chattriggers.ctjs.internal.engine.module.Module
import com.chattriggers.ctjs.internal.engine.module.ModuleManager
import com.chattriggers.ctjs.internal.engine.module.ModulesGui
import com.chattriggers.ctjs.internal.listeners.ClientListener
import com.chattriggers.ctjs.internal.utils.Initializer
import com.chattriggers.ctjs.internal.utils.toVersion
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import gg.essential.universal.UDesktop
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Util
import java.io.File
import java.io.IOException
import java.lang.IllegalArgumentException
import kotlin.concurrent.thread

internal object CTCommand : Initializer {
    private const val idFixed = 90123 // ID for dumped chat
    private var idFixedOffset = -1 // ID offset (increments)
    private var pendingImport: String? = null

    override fun init() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            register(dispatcher)
        }
    }

    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        val command = literal("ct")
            .then(literal("load").onExecute { CTJS.load(asCommand = true) })
            .then(literal("unload").onExecute { CTJS.unload(asCommand = true) })
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
            .then(literal("console").onExecute { Console.show() })
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
            .then(
                literal("migrate")
                    .then(
                        argument("input", FileArgumentType(File(CTJS.MODULES_FOLDER)))
                            .then(
                                argument("output", FileArgumentType(File(CTJS.MODULES_FOLDER)))
                                    .onExecute {
                                        val input = FileArgumentType.getFile(it, "input")
                                        val output = FileArgumentType.getFile(it, "output")
                                        Migration.migrate(input, output)
                                    }
                            )
                            .onExecute {
                                val input = FileArgumentType.getFile(it, "input")
                                Migration.migrate(input, input)
                            }
                    )
            )
            .onExecute { ChatLib.chat(getUsage()) }

        dispatcher.register(command)
    }

    private fun import(moduleName: String) {
        if (ModuleManager.cachedModules.any { it.name.equals(moduleName, ignoreCase = true) }) {
            ChatLib.chat("&cModule $moduleName is already installed!")
            return
        }
        
        thread {
            val (module, dependencies) = ModuleManager.importModule(moduleName)
            if (module == null) {
                ChatLib.chat("&cUnable to import module $moduleName")
                return@thread
            }

            if (pendingImport == moduleName) {
                // User has confirmed the import, proceed with the import
                pendingImport = null
                performImport(module, dependencies)
            } else if(Config.dependenciesConfirmation) {
                // User has not confirmed the import yet, fetch the dependencies and ask for confirmation
                val dependencyNames = dependencies.map { it.metadata.name ?: it.name }
                ChatLib.chat("&cImporting $moduleName... This will also import ${dependencyNames.joinToString(", ")}. Are you sure?")
                TextComponent(Text.literal("Type `/ct import $moduleName` again or click [Import] to confirm.").styled {
                    it.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ct import $moduleName"))
                }).chat()

                pendingImport = moduleName
            } else {
                ChatLib.chat("&cImporting $moduleName...")
                performImport(module, dependencies)
            }
        }
    }

    private fun performImport(module: Module, dependencies: List<Module>) {
        val allModules = listOf(module) + dependencies
        val modVersion = CTJS.MOD_VERSION.toVersion()
        allModules.forEach {
            val version = it.targetModVersion ?: return@forEach
            if (version.majorVersion < modVersion.majorVersion)
                ModuleManager.tryReportOldVersion(it)
        }

        ChatLib.chat("&aSuccessfully imported ${module.metadata.name ?: module.name}")

        if (dependencies.isNotEmpty()) {
            val dependencyNames = dependencies.map { it.metadata.name ?: it.name }
            ChatLib.chat("&eAlso imported ${dependencyNames.joinToString(", ")}")
        }

        if (Config.moduleImportHelp && module.metadata.helpMessage != null) {
            ChatLib.chat(module.metadata.helpMessage.toString().take(383))
        }
    }

    private fun getUsage() = """
        &b&m${ChatLib.getChatBreak()}
        &c/ct load &7- &oReloads all of the ChatTriggers modules.
        &c/ct import <module> &7- &oImports a module.
        &c/ct delete <module> &7- &oDeletes a module.
        &c/ct files &7- &oOpens the ChatTriggers folder.
        &c/ct modules &7- &oOpens the modules GUI.
        &c/ct console [language] &7- &oOpens the ChatTriggers console.
        &c/ct simulate <message> &7- &oSimulates a received chat message.
        &c/ct dump &7- &oDumps previous chat messages into chat.
        &c/ct settings &7- &oOpens the ChatTriggers settings.
        &c/ct migrate <input> [output]&7 - &oMigrate a module from version 2.X to 3.X 
        &c/ct &7- &oDisplays this help dialog.
        &b&m${ChatLib.getChatBreak()}
    """.trimIndent()

    private fun openFileLocation() {
        try {
            if (UDesktop.isMac) {
                Util.getOperatingSystem().open(ModuleManager.modulesFolder.toURI())
            } else {
                UDesktop.browse(ModuleManager.modulesFolder.toURI())
            }
        } catch (exception: IOException) {
            exception.printTraceToConsole()
            ChatLib.chat("&cCould not open file location")
        }
    }

    private fun dump(type: DumpType, lines: Int = 100) {
        clearOldDump()

        val messages = type.messageList()
        val toDump = lines.coerceAtMost(messages.size)
        TextComponent("&6&m${ChatLib.getChatBreak()}").withChatLineId(idFixed).chat()

        for (i in 0 until toDump) {
            val msg = ChatLib.replaceFormatting(messages[messages.size - toDump + i].formattedText)
            TextComponent(Text.literal(msg).styled {
                it.withClickEvent(ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, msg))
                    .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent("&eClick here to copy this message.")))
            })
                .withChatLineId(idFixed + i + 1)
                .chat()
        }

        TextComponent("&6&m${ChatLib.getChatBreak()}").withChatLineId(idFixed + lines + 1).chat()

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
            fun fromString(str: String) = DumpType.entries.first { it.name.equals(str, ignoreCase = true) }
        }
    }

    private class FileArgumentType(private val relativeTo: File) : ArgumentType<File> {
        override fun parse(reader: StringReader): File {
            val isquoted = StringReader.isQuotedStringStart(reader.peek())
            val path = if (isquoted) {
                reader.readQuotedString()
            } else reader.readStringUntilOrEof(' ')
            return File(relativeTo, path)
        }

        override fun getExamples(): MutableCollection<String> {
            return mutableListOf(
                "/foo/bar/baz",
                "C:\\foo\\bar\\baz",
                "\"/path/with/spaces in the name\"",
            )
        }

        // Copy and pasted from StringReader, but doesn't throw on EOF
        fun StringReader.readStringUntilOrEof(terminator: Char): String {
            val result = StringBuilder()
            var escaped = false
            while (canRead()) {
                val c = read()
                when {
                    escaped -> {
                        escaped = if (c == terminator || c == '\\') {
                            result.append(c)
                            false
                        } else {
                            cursor -= 1
                            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidEscape()
                                .createWithContext(this, c.toString())
                        }
                    }
                    c == '\\' -> escaped = true
                    c == terminator -> {
                        cursor -= 1
                        return result.toString()
                    }
                    else -> result.append(c)
                }
            }

            return result.toString()
        }

        companion object {
            fun getFile(ctx: CommandContext<*>, name: String) = ctx.getArgument(name, File::class.java)
        }
    }
}
