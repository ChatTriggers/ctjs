package com.chattriggers.ctjs.internal.console

import com.chattriggers.ctjs.engine.LogType
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rsyntaxtextarea.Theme
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.WindowEvent
import java.io.ByteArrayInputStream
import java.util.concurrent.CompletableFuture
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import javax.swing.text.DefaultCaret

class ConsoleFrame(
    private val client: ConsoleClientProcess,
    private val init: InitMessage,
    var onEval: (String) -> CompletableFuture<String>,
    var onReload: () -> Unit,
    var fontSizeListener: (Int) -> Unit,
) {
    private lateinit var currentConfig: ConfigUpdateMessage
    private var firaFont: Font? = init.firaFontBytes?.let {
        Font.createFont(Font.TRUETYPE_FONT, ByteArrayInputStream(it))
            .deriveFont(9f)
            .also(GraphicsEnvironment.getLocalGraphicsEnvironment()::registerFont)
    }

    private val frame = JFrame(
        "ChatTriggers ${init.modVersion} JS Console"
    )

    private val textArea = JTextPane()
    private val inputField = RSyntaxTextArea(5, 1).apply {
        syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT
        Theme.load(javaClass.getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml")).apply(this)
        margin = Insets(5, 5, 5, 5)
        isCodeFoldingEnabled = true
    }

    private val components = mutableSetOf<Component>(textArea)
    private val history = mutableListOf<String>()
    private var historyOffset = 0

    val writer = TextAreaWriter(textArea, ::currentConfig)

    init {
        setConfig(init.config)

        frame.defaultCloseOperation = JFrame.HIDE_ON_CLOSE

        textArea.isEditable = false

        textArea.margin = Insets(5, 5, 5, 5)
        textArea.autoscrolls = true
        val caret = textArea.caret as DefaultCaret
        caret.updatePolicy = DefaultCaret.ALWAYS_UPDATE

        inputField.addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent) {}

            override fun keyPressed(e: KeyEvent) {}

            override fun keyReleased(e: KeyEvent) {
                if (!e.isControlDown) return

                when (e.keyCode) {
                    KeyEvent.VK_ENTER -> {
                        val command = inputField.text
                        inputField.text = ""
                        history.add(command)
                        historyOffset = 0

                        if (command == "help") {
                            writer.println(
                                """
                                -------------- ChatTriggers Console Help --------------
                                 Shortcuts:
                                  Control + Enter: Run code in the textbox
                                  Control + UP / DOWN: Cycle between ran code history
                                  Control + L: Clear console
                                  Control + R: Reload ChatTriggers
                                  Control +: Increase console font size
                                  Control -: Decreate console font size
                                -------------------------------------------------------
                            """.trimIndent()
                            )
                        } else {
                            writer.println("eval> ${command.prependIndent("    > ").substring(6)}")

                            onEval(command).thenAccept(writer::println)
                        }
                    }
                    KeyEvent.VK_UP -> {
                        historyOffset++

                        try {
                            val message = history[history.size - historyOffset]
                            inputField.text = message
                        } catch (exception: Exception) {
                            historyOffset--
                        }
                    }
                    KeyEvent.VK_DOWN -> {
                        historyOffset--

                        if (historyOffset < 0) historyOffset = 0

                        try {
                            val message = history[history.size - historyOffset]
                            inputField.text = message
                        } catch (exception: Exception) {
                            historyOffset = 0
                            inputField.text = ""
                        }
                    }
                    KeyEvent.VK_L -> clearConsole()
                    KeyEvent.VK_R -> onReload()
                    KeyEvent.VK_MINUS -> fontSizeListener(-1)
                    KeyEvent.VK_PLUS, KeyEvent.VK_EQUALS -> fontSizeListener(1)
                }
            }
        })

        frame.add(JScrollPane(textArea))
        frame.add(inputField, BorderLayout.SOUTH)
        frame.pack()
        frame.isVisible = false
        frame.setSize(800, 600)
    }

    fun clearConsole() {
        invokeLater { writer.clear() }
    }

    fun close() {
        invokeLater { frame.dispatchEvent(WindowEvent(frame, WindowEvent.WINDOW_CLOSING)) }
    }

    @JvmOverloads
    fun println(obj: Any, logType: LogType, end: String = "\n", customColor: Color? = null) {
        invokeLater {
            writer.println(obj.toString(), logType, end, customColor)
        }
    }

    fun printError(error: PrintErrorMessage.Error) {
        fun StringBuilder.appendError(err: PrintErrorMessage.Error) {
            val index = err.trace.indexOfFirst {
                it.fileName?.lowercase()?.contains("jsloader") ?: false
            }

            val trimmedTrace = if (index != -1) {
                err.trace.dropLast(err.trace.size - index - 1).map {
                    val fileNameIndex = it.fileName?.indexOf("ChatTriggers/modules/") ?: return@map it
                    val classNameIndex = it.className.indexOf("ChatTriggers_modules_")

                    if (fileNameIndex != -1) {
                        StackTrace(
                            it.className.substring(classNameIndex + 21),
                            it.methodName,
                            it.fileName.substring(fileNameIndex + 21),
                            it.lineNumber
                        )
                    } else it
                }
            } else err.trace

            appendLine(err.message)
            for (trace in trimmedTrace) {
                append("\tat ${trace.className}.${trace.methodName} (${trace.fileName}")
                if (trace.lineNumber != -1)
                    append(":${trace.lineNumber}")
                appendLine(")")
            }

            if (err.cause != null) {
                append("Caused by: ")
                appendError(err.cause)
            }
        }

        invokeLater {
            if (currentConfig.openConsoleOnError)
                showConsole()

            writer.println(buildString {
                appendError(error)
            }, LogType.ERROR)
        }
    }

    fun setConfig(config: ConfigUpdateMessage) {
        currentConfig = config

        val (fg, bg) = if (config.customTheme) {
            Color(config.textColor) to Color(config.backgroundColor)
        } else {
            when (config.theme) {
                0 -> Color(208, 208, 208) to Color(41, 49, 52)
                1 -> Color(199, 204, 209) to Color(28, 32, 35)
                2 -> Color(199, 204, 209) to Color(28, 32, 35)
                3 -> Color(208, 208, 208) to Color(0, 0, 0)
                4 -> Color(126, 162, 180) to Color(22, 27, 29)
                5 -> Color(152, 209, 206) to Color(10, 15, 20)
                6 -> Color(197, 200, 198) to Color(29, 31, 33)
                7 -> Color(74, 69, 67) to Color(247, 247, 247)
                8 -> Color(48, 48, 48) to Color(245, 245, 245)
                9 -> Color(221, 223, 235) to Color(15, 18, 32)
                10 -> Color(193, 199, 208) to Color(33, 36, 41)
                11 -> Color(231, 210, 212) to Color(26, 9, 11)
                12 -> Color(47, 227, 149) to Color(6, 10, 10)
                13 -> Color(192, 20, 214) to Color(251, 251, 28)
                else -> Color(208, 208, 208) to Color(21, 21, 21)
            }
        }

        for (comp in components) {
            comp.background = bg
            comp.foreground = fg
        }

        val chosenFont = if (firaFont != null && config.useFiraCode) {
            firaFont!!
        } else {
            Font(
                "DejaVu Sans Mono",
                Font.PLAIN,
                15
            )
        }.deriveFont(config.fontSize.toFloat())

        textArea.font = chosenFont
        inputField.font = chosenFont

        frame.toFront()
        frame.repaint()
    }

    fun showConsole() {
        frame.isVisible = true
    }

    private fun invokeLater(block: () -> Unit) {
        SwingUtilities.invokeLater {
            try {
                block()
            } catch (e: Throwable) {
                client.reportException(e)
            }
        }
    }
}
