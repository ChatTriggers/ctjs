package com.chattriggers.ctjs.internal.console

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.engine.LogType
import com.chattriggers.ctjs.internal.engine.JSLoader
import com.chattriggers.ctjs.internal.listeners.ClientListener
import com.chattriggers.ctjs.internal.utils.Initializer
import com.mojang.blaze3d.platform.InputConstants
import kotlinx.serialization.json.Json
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier
import net.minecraft.util.Util
import org.lwjgl.glfw.GLFW
import java.awt.Color
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.URLClassLoader
import java.net.URLDecoder
import java.nio.charset.Charset
import kotlin.concurrent.thread
import kotlin.io.path.Path


/**
 * Responsible for spawning and managing a separate Java console process (which uses AWT)
 *
 * As this console runs in a completely separate process, we use sockets to communicate
 * back and forth. The remote process is referred to as the "Client", and this process
 * (the main MC process) is referred to as the "Host". The semantics line up with the
 * naming scheme of the messages passed in the socket connection (H2C/C2H).
 *
 * Why a separate process? AWT is unfortunately incompatible with GLFW, which is an issue
 * on newer versions of MC that use LWJGL 3. So much so that [net.minecraft.client.main.Main]
 * sets the AWT headless property to prevent its use on the render thread. Spawning a
 * new process gives us a new main thread without GLFW.
 *
 * Each console gets its own Host/Client pair running on their own port, so there will be
 * `<number of loaders> + 1` sockets (the extra 1 is for the generic console).
 */
object ConsoleHostProcess : Initializer {
    private var PORT = 9002
    private var running = true
    private lateinit var socketOut: PrintWriter
    private lateinit var process: Process

    // We will buffer all message that are attempted to be sent until we connect to a
    // client socket. This allows us to handle early events (e.g. errors that happen during
    // dynamic mixin application)
    private var connected = false
    private val pendingMessages = mutableListOf<H2CMessage>()

    init {
        thread { hostMain() }
    }

    override fun init() {
        val keybind = KeyMappingHelper.registerKeyMapping(
            KeyMapping(
                "ctjs.key.binding.console",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                KeyMapping.Category(Identifier.fromNamespaceAndPath("ctjs", "key.category")),
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick {
            while (keybind.consumeClick()) {
                show()
            }
        })
    }

    private fun hostMain() {
        // Spawn the Client process. This remote process can be easily debugged in IntelliJ by
        // adding the Remote JVM Debug command line arguments after the class path argument, and
        // then simply placing a breakpoint anywhere in the RemoteConsoleClient class.

        val urlObjects = (Thread.currentThread().contextClassLoader.parent as URLClassLoader).urLs
        val urls = urlObjects.joinToString(File.pathSeparator) {
            val str = if (Util.getPlatform().toString().contains("windows", true)) it.toString()
                .replace("file:/", "") else it.toString()
            URLDecoder.decode(str, Charset.defaultCharset())
        }

        process = ProcessBuilder()
            .directory(File("."))
            .command(
                Path(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp",
                urls,
                ConsoleClientProcess::class.qualifiedName,
                PORT.toString(),
                ProcessHandle.current().pid().toString(),
            )
            .start()

        while (running) {
            ServerSocket(PORT).accept().use { socket ->
                socketOut = PrintWriter(socket.outputStream, true, Charsets.UTF_8)
                val socketIn = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))
                connected = true

                val initMessage = InitMessage(
                    CTJS.MOD_VERSION,
                    ConfigUpdateMessage.constructFromDefaults(),
                    this::class.java.getResourceAsStream("/assets/ctjs/FiraCode-Regular.otf")?.readAllBytes(),
                )

                synchronized(socketOut) {
                    socketOut.println(Json.encodeToString<H2CMessage>(initMessage))
                    pendingMessages.forEach { socketOut.println(Json.encodeToString<H2CMessage>(it)) }
                }

                while (running) {
                    val messageText = try {
                        socketIn.readLine()
                    } catch (_: Throwable) {
                        println("Received error, reopening the connection")
                        return@use
                    }

                    if (messageText == null) {
                        Thread.sleep(50)
                        continue
                    }

                    when (val message = Json.decodeFromString<C2HMessage>(messageText)) {
                        is EvalTextMessage -> {
                            val result = JSLoader.eval(message.string) ?: continue
                            trySendMessage(EvalResultMessage(message.id, result))
                        }
                        is FontSizeMessage -> {
                            onConsoleSettingsChanged()
                        }
                        ReloadCTMessage -> ClientListener.addTask(0) { CTJS.load() }
                    }
                }
            }

            connected = false
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
        trySendMessage(TerminateMessage)
        process.destroy()
    }

    fun onConsoleSettingsChanged() =
        trySendMessage(ConfigUpdateMessage.constructFromDefaults())

    private fun trySendMessage(message: H2CMessage) {
        if (connected) {
            synchronized(socketOut) {
                socketOut.println(Json.encodeToString(message))
            }
        } else {
            pendingMessages.add(message)
        }
    }
}
