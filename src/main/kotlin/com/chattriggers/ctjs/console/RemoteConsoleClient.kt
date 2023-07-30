package com.chattriggers.ctjs.console

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.Color
import java.io.*
import java.net.Socket
import java.util.concurrent.CompletableFuture
import kotlin.system.exitProcess

/**
 * Runs in a separate process and is responsible for rendering the CT consoles.
 *
 * Note that this should not reference any MC classes or CT classes outside of
 * this package.
 *
 * @see RemoteConsoleHost
 */
class RemoteConsoleClient(private val port: Int) {
    private var frame: ConsoleFrame? = null
    private val pendingEvalFutures = mutableMapOf<Int, CompletableFuture<String>>()
    private var nextEvalId = 0
    private var lastPingTime = System.currentTimeMillis()

    // If the users computer it slow, it may take more than 10 seconds for us to
    // receive the first ping
    private var hasBeenPinged = false

    fun start() {
        debug("Starting...\n")

        var socket: Socket? = null
        try {
            socket = Socket("127.0.0.1", port)
            val socketOut = PrintWriter(socket.outputStream, true, Charsets.UTF_8)
            val socketIn = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))

            while (true) {
                if (socket.isClosed) {
                    debug("Socket closed\n")
                    break
                }

                if (!socketIn.ready()) {
                    debug("Not ready, hasBeenPinged = $hasBeenPinged, diff = ${System.currentTimeMillis() - lastPingTime}\n")
                    if (hasBeenPinged && System.currentTimeMillis() - lastPingTime > TIMEOUT) {
                        debug("Exiting\n");
                        exitProcess(0)
                    }
                    Thread.sleep(50)
                    continue
                }

                val messageText = socketIn.readLine() ?: break
                debug("Received message \"${messageText.take(300)}\"\n")
                when (val message = Json.decodeFromString<H2CMessage>(messageText)) {
                    is InitMessage -> {
                        frame = ConsoleFrame(
                            this,
                            message,
                            onEval = { text ->
                                val future = CompletableFuture<String>()
                                val id = nextEvalId++
                                socketOut.println(Json.encodeToString<C2HMessage>(EvalTextMessage(id, text)))
                                pendingEvalFutures[id] = future
                                future
                            },
                            onReload = {
                                socketOut.println(Json.encodeToString<C2HMessage>(ReloadCTMessage))
                            }
                        )
                    }
                    is ConfigUpdateMessage -> {
                        frame?.setConfig(message) ?: error("Received ConfigUpdateMessage before InitMessage")
                    }
                    is EvalResultMessage -> {
                        pendingEvalFutures[message.id]?.complete(message.result)
                            ?: error("Unknown eval id ${message.id}")
                    }
                    OpenMessage -> {
                        frame?.showConsole() ?: error("Received OpenMessage before InitMessage")
                    }
                    TerminateMessage -> exitProcess(0)
                    ClearConsoleMessage -> {
                        frame?.clearConsole() ?: error("Received ClearConsoleMessage before InitMessage")
                    }
                    PingMessage -> {
                        debug("Received ping at ${System.currentTimeMillis()}\n")
                        lastPingTime = System.currentTimeMillis()
                        hasBeenPinged = true
                    }
                    is PrintMessage -> {
                        frame?.println(message.text, message.logType, message.end, message.color?.let(::Color))
                            ?: error("Received PrintMessage before InitMessage")
                    }
                    is PrintStackTraceMessage -> {
                        frame?.printStackTrace(message.message, message.trace)
                            ?: error("Received PrintStackTraceMessage before InitMessage")
                    }
                }
            }
        } catch (e: Throwable) {
            if (ENABLE_DEBUG)
                e.printStackTrace(PrintStream(debugOutput))
        } finally {
            socket?.close()
        }

        debug("<end>\n")
    }

    internal fun reportAsyncException(e: Throwable) {
        if (ENABLE_DEBUG)
            e.printStackTrace(PrintStream(debugOutput))
    }

    // Since this is a separate process, the easiest way to print debug output is to just
    // write it to a file. Use the port number in the name to ensure its unique.
    private val debugOutput = File("./console_output_$port.txt").also { it.writeText("") }
    private fun debug(message: String) {
        if (ENABLE_DEBUG)
            debugOutput.appendText(message)
    }

    companion object {
        private const val ENABLE_DEBUG = false
        private const val TIMEOUT = CONSOLE_PING_TIME * 2 + 1000

        @JvmStatic
        fun main(args: Array<String>) {
            val port = args.firstOrNull()?.toIntOrNull() ?: error("Expected port as first argument")

            // Give the host process time to start the socket
            Thread.sleep(100)
            RemoteConsoleClient(port).start()
        }
    }
}
