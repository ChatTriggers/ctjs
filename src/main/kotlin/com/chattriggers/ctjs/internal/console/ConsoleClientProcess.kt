package com.chattriggers.ctjs.internal.console

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.Color
import java.io.*
import java.net.Socket
import java.util.concurrent.CompletableFuture

/**
 * Runs in a separate process and is responsible for rendering the CT consoles.
 *
 * Note that this should not reference any MC classes or CT classes outside of
 * this package.
 *
 * @see Console
 */
class ConsoleClientProcess(private val port: Int, private val hostPid: Long) {
    private var frame: ConsoleFrame? = null
    private val pendingEvalFutures = mutableMapOf<Int, CompletableFuture<String>>()
    private var nextEvalId = 0
    private var running = true

    fun start() {
        while (running) {
            socketLoop()

            // Only stop trying to connect if the host process is no longer available.
            // Otherwise, attempt to reconnect
            if (ProcessHandle.of(hostPid).isEmpty)
                break
        }

        debug("Closing console process...")
    }

    private fun socketLoop() {
        var socket: Socket? = null
        debug("Connecting...")
        try {
            socket = Socket("127.0.0.1", port)

            while (!socket.isConnected)
                Thread.sleep(200)

            if (!socket.isConnected) {
                debug("Socket not able to reconnect, breaking from connect loop...")
                return
            }

            debug("Connected!")
            val socketOut = PrintWriter(socket.outputStream, true, Charsets.UTF_8)
            val socketIn = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))

            while (true) {
                if (socket.isClosed || !socket.isConnected) {
                    debug("Socket closed\n")
                    return
                }

                if (!socketIn.ready()) {
                    Thread.sleep(50)
                    continue
                }

                val messageText = socketIn.readLine() ?: break
                debug("Received message \"${messageText.take(300)}\"\n")
                when (val message = Json.decodeFromString<H2CMessage>(messageText)) {
                    is InitMessage -> {
                        // The frame won't be null if this message is a result of a reconnection to
                        // the socket (i.e. this is not the first InitMessage we've received)
                        if (frame == null) {
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
                                },
                                fontSizeListener = { delta ->
                                    socketOut.println(Json.encodeToString<C2HMessage>(FontSizeMessage(delta)))
                                }
                            )
                        }
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
                    TerminateMessage -> {
                        running = false
                        return
                    }
                    ClearConsoleMessage -> {
                        frame?.clearConsole() ?: error("Received ClearConsoleMessage before InitMessage")
                    }
                    is PrintMessage -> {
                        frame?.println(message.text, message.logType, message.end, message.color?.let(::Color))
                            ?: error("Received PrintMessage before InitMessage")
                    }
                    is PrintErrorMessage -> {
                        frame?.printError(message.error)
                            ?: error("Received PrintStackTraceMessage before InitMessage")
                    }
                }
            }
        } catch (e: Throwable) {
            reportException(e)
        } finally {
            socket?.close()
        }

        return
    }

    internal fun reportException(e: Throwable) {
        if (ENABLE_DEBUG) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            debugOutput.appendText(sw.toString())
        }
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

        @JvmStatic
        fun main(args: Array<String>) {
            val port = args.getOrNull(0)?.toIntOrNull() ?: error("Expected port as first argument")
            val hostPid = args.getOrNull(1)?.toLongOrNull() ?: error("Expected host PID as second argument")

            // Give the host process time to start the socket
            Thread.sleep(100)
            ConsoleClientProcess(port, hostPid).start()
        }
    }
}
