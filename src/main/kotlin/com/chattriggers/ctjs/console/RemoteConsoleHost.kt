package com.chattriggers.ctjs.console

import com.chattriggers.ctjs.Reference
import com.chattriggers.ctjs.engine.ILoader
import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.utils.Config
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.Color
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import kotlin.concurrent.thread

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
class RemoteConsoleHost(private val loader: ILoader?) : Console {
    private val port = NEXT_PORT++
    private var running = true
    private lateinit var socketOut: PrintWriter

    // We will buffer all message that are attempted to be sent until we connect to a
    // client socket. This allows us to handle early events (e.g. errors that happen during
    // dynamic mixin application)
    private var connected = false
    private val pendingMessages = mutableListOf<H2CMessage>()

    init {
        thread { hostMain() }
    }

    private fun hostMain() {
        // Spawn the Client process. This remote process can be easily debugged in IntelliJ by
        // adding the Remote JVM Debug command line arguments after the class path argument, and
        // then simply placing a breakpoint anywhere in the RemoteConsoleClient class.
        ProcessBuilder()
            .directory(File("."))
            .command(
                "java",
                "-cp",
                System.getProperty("java.class.path", "."),
                RemoteConsoleClient::class.qualifiedName,
                port.toString(),
            )
            .start()

        ServerSocket(port).accept().use { socket ->
            socketOut = PrintWriter(socket.outputStream, true)
            val socketIn = BufferedReader(InputStreamReader(socket.inputStream))
            connected = true

            val initMessage = InitMessage(
                Reference.MOD_VERSION,
                loader?.getLanguage(),
                ConfigUpdateMessage.constructFromConfig(Config.ConsoleSettings.make()),
                this::class.java.getResourceAsStream("/assets/chattriggers/FiraCode-Regular.otf")?.readAllBytes(),
            )

            synchronized(socketOut) {
                socketOut.println(Json.encodeToString<H2CMessage>(initMessage))
                pendingMessages.forEach { socketOut.println(Json.encodeToString<H2CMessage>(it)) }
            }

            while (running) {
                val messageText = socketIn.readLine() ?: error("Unexpected socket close")
                when (val message = Json.decodeFromString<C2HMessage>(messageText)) {
                    is EvalTextMessage -> {
                        // If this is the general console, we effectively just ignore this message
                        try {
                            val result = loader?.eval(message.string) ?: continue
                            synchronized(socketOut) {
                                socketOut.println(Json.encodeToString<H2CMessage>(EvalResultMessage(message.id, result)))
                            }
                        } catch (e: Throwable) {
                            printStackTrace(e)
                        }
                    }
                    ReloadCTMessage -> Client.scheduleTask { Reference.loadCT() }
                }
            }
        }
    }

    override fun clear() = trySendMessage(ClearConsoleMessage)

    override fun println(obj: Any, logType: LogType, end: String, customColor: Color?) {
        trySendMessage(PrintMessage(obj.toString(), logType, end, customColor?.rgb))
    }

    override fun printStackTrace(error: Throwable) {
        val trace = error.stackTrace.map {
            StackTrace(it.fileName, it.className, it.methodName, it.lineNumber)
        }
        trySendMessage(PrintStackTraceMessage(error.message.orEmpty(), trace))
    }

    override fun show() = trySendMessage(OpenMessage)

    override fun close() = trySendMessage(CloseMessage)

    override fun onConsoleSettingsChanged(settings: Config.ConsoleSettings) =
        trySendMessage(ConfigUpdateMessage.constructFromConfig(settings))

    private fun trySendMessage(message: H2CMessage) {
        if (connected) {
            synchronized(socketOut) {
                socketOut.println(Json.encodeToString(message))
            }
        } else {
            pendingMessages.add(message)
        }
    }

    companion object {
        private var NEXT_PORT = 9002
    }
}
