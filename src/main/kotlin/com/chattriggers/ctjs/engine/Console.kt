package com.chattriggers.ctjs.engine

import com.chattriggers.ctjs.api.Config
import com.chattriggers.ctjs.internal.console.ConsoleHostProcess
import kotlinx.serialization.Serializable
import org.mozilla.javascript.WrappedException
import java.awt.Color

// A wrapper object so that we can hide away the implementation in the
// internal package
object Console {
    fun clear() = ConsoleHostProcess.clear()
    fun println(obj: Any, logType: LogType, end: String, customColor: Color?) =
        ConsoleHostProcess.println(obj, logType, end, customColor)
    fun println(obj: Any, logType: LogType, end: String) = ConsoleHostProcess.println(obj, logType, end, null)
    fun println(obj: Any, logType: LogType) = println(obj, logType, "\n")
    fun println(obj: Any) = println(obj, LogType.INFO)

    fun printStackTrace(error: Throwable) = ConsoleHostProcess.printStackTrace(error)
    fun show() = ConsoleHostProcess.show()
    fun close() = ConsoleHostProcess.close()
    fun onConsoleSettingsChanged(settings: Config.ConsoleSettings) =
        ConsoleHostProcess.onConsoleSettingsChanged(settings)
}

@Serializable
enum class LogType {
    INFO,
    WARN,
    ERROR,
}

fun Any.printToConsole(logType: LogType = LogType.INFO) {
    Console.println(this, logType)
}

fun Throwable.printTraceToConsole(): Unit = if (this is WrappedException) {
    wrappedException.printTraceToConsole()
} else {
    this.printStackTrace()
    Console.printStackTrace(this)
}
