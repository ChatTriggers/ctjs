package com.chattriggers.ctjs.internal.console

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.api.Config
import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.engine.LogType
import com.chattriggers.ctjs.internal.engine.CTEvents
import com.chattriggers.ctjs.internal.engine.JSLoader
import com.chattriggers.ctjs.internal.utils.Initializer
import com.mojang.blaze3d.platform.InputConstants
import gg.essential.universal.UDesktop
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW
import java.awt.Color
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URLClassLoader
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.io.path.Path

/** Manages the process-global external console and its reconnectable socket. */
object ConsoleHostProcess : Initializer {
    private const val PORT = 9002
    private val running = AtomicBoolean(true)
    private val stateLock = Any()
    private val pendingMessages = MessageBacklog<H2CMessage>()

    @Volatile
    private var connected = false
    private var socketOut: PrintWriter? = null
    private var clientSocket: Socket? = null
    private var serverSocket: ServerSocket? = null
    private var process: Process? = null

    private val hostThread = thread(name = "CTJS console host") { hostMain() }

    override fun init() {
        val keybind = KeyMappingHelper.registerKeyMapping(
            KeyMapping(
                "ctjs.key.binding.console",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("ctjs", "key.category")),
            )
        )

        CTEvents.RENDER_GAME.register {
            if (keybind.consumeClick())
                show()
        }
    }

    private fun hostMain() {
        try {
            val classpath = buildConsoleClasspath()
            val server = ServerSocket(PORT)
            synchronized(stateLock) { serverSocket = server }
            startClientProcess(classpath)

            while (running.get()) {
                val socket = try {
                    server.accept()
                } catch (e: SocketException) {
                    if (!running.get()) break else throw e
                }
                serveClient(socket)
            }
        } catch (e: Throwable) {
            if (running.get())
                e.printStackTrace()
        } finally {
            synchronized(stateLock) {
                connected = false
                socketOut = null
                clientSocket = null
                serverSocket = null
            }
        }
    }

    private fun buildConsoleClasspath(): String {
        val urls = (Thread.currentThread().contextClassLoader.parent as URLClassLoader).urLs
        return urls.joinToString(File.pathSeparator) {
            val value = if (UDesktop.isWindows) it.toString().replace("file:/", "") else it.toString()
            URLDecoder.decode(value, Charset.defaultCharset())
        }
    }

    private fun startClientProcess(classpath: String) {
        synchronized(stateLock) {
            if (process != null || !running.get())
                return
            process = ProcessBuilder()
                .directory(File("."))
                .command(
                    Path(System.getProperty("java.home"), "bin", "java").toString(),
                    "-cp",
                    classpath,
                    ConsoleClientProcess::class.qualifiedName,
                    PORT.toString(),
                    ProcessHandle.current().pid().toString(),
                )
                .start()
        }
    }

    private fun serveClient(socket: Socket) {
        synchronized(stateLock) { clientSocket = socket }
        socket.use {
            val writer = PrintWriter(socket.outputStream, true, Charsets.UTF_8)
            val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))
            synchronized(stateLock) {
                socketOut = writer
                connected = true
            }

            val font = javaClass.getResourceAsStream("/assets/ctjs/FiraCode-Regular.otf")?.use {
                it.readAllBytes()
            }
            send(
                writer,
                InitMessage(
                    CTJS.MOD_VERSION,
                    ConfigUpdateMessage.constructFromConfig(Config.ConsoleSettings.make()),
                    font,
                )
            )
            pendingMessages.flush { send(writer, it) }

            while (running.get()) {
                val messageText = try {
                    reader.readLine()
                } catch (_: Throwable) {
                    break
                } ?: break

                when (val message = Json.decodeFromString<C2HMessage>(messageText)) {
                    is EvalTextMessage -> {
                        val result = JSLoader.eval(message.string) ?: continue
                        trySendMessage(EvalResultMessage(message.id, result))
                    }
                    is FontSizeMessage -> {
                        Config.consoleFontSize = (Config.consoleFontSize + message.delta).coerceIn(6..32)
                        onConsoleSettingsChanged(Config.ConsoleSettings.make())
                    }
                    ReloadCTMessage -> Client.scheduleSystemTask { CTJS.load() }
                }
            }
        }

        synchronized(stateLock) {
            connected = false
            socketOut = null
            clientSocket = null
        }
    }

    fun clear() = trySendMessage(ClearConsoleMessage)

    fun println(obj: Any, logType: LogType, end: String, customColor: Color?) {
        trySendMessage(PrintMessage(obj.toString(), logType, end, customColor?.rgb))
        print(obj.toString() + end)
    }

    fun printStackTrace(error: Throwable) {
        fun makeError(err: Throwable): PrintErrorMessage.Error = PrintErrorMessage.Error(
            err::class.qualifiedName?.let { "$it: " } + err.message.orEmpty(),
            err.stackTrace.map {
                StackTrace(it.fileName, it.className, it.methodName, it.lineNumber)
            },
            err.cause?.let(::makeError),
        )
        trySendMessage(PrintErrorMessage(makeError(error)))
    }

    fun show() = trySendMessage(OpenMessage)

    fun close() {
        if (!running.compareAndSet(true, false))
            return

        synchronized(stateLock) {
            socketOut?.let { send(it, TerminateMessage) }
            clientSocket?.close()
            serverSocket?.close()
            process?.destroy()
        }
        if (Thread.currentThread() !== hostThread)
            hostThread.join(2_000)
        process?.takeIf(Process::isAlive)?.destroyForcibly()
        pendingMessages.clear()
    }

    fun onConsoleSettingsChanged(settings: Config.ConsoleSettings) =
        trySendMessage(ConfigUpdateMessage.constructFromConfig(settings))

    private fun trySendMessage(message: H2CMessage) {
        synchronized(stateLock) {
            val writer = socketOut
            if (running.get() && connected && writer != null) {
                if (!send(writer, message)) {
                    connected = false
                    pendingMessages.add(message)
                }
            } else if (running.get()) {
                pendingMessages.add(message)
            }
        }
    }

    private fun send(writer: PrintWriter, message: H2CMessage): Boolean {
        writer.println(Json.encodeToString(message))
        return !writer.checkError()
    }

    internal fun pendingMessageCount(): Int = pendingMessages.size()
}
