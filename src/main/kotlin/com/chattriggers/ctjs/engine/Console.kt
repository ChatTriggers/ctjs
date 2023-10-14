package com.chattriggers.ctjs.engine

import com.chattriggers.ctjs.api.Config
import com.chattriggers.ctjs.internal.console.ConsoleHostProcess
import com.chattriggers.ctjs.internal.console.LogType
import com.chattriggers.ctjs.internal.utils.Initializer
import java.awt.Color

// A wrapper object so that we can hide away the implementation in the
// internal package
object Console : Initializer {
    override fun init() = ConsoleHostProcess.init()
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

fun Any.printToConsole(logType: LogType = LogType.INFO) {
    Console.println(this, logType)
}

fun Throwable.printTraceToConsole() {
    this.printStackTrace()
    Console.printStackTrace(this)
}
