package com.chattriggers.ctjs.console

import com.chattriggers.ctjs.utils.Config
import java.awt.Color

interface Console {
    fun clear()

    fun println(obj: Any, logType: LogType, end: String, customColor: Color?)

    fun println(obj: Any, logType: LogType, end: String) = println(obj, logType, end, null)
    fun println(obj: Any, logType: LogType) = println(obj, logType, "\n")
    fun println(obj: Any) = println(obj, LogType.INFO)

    fun printStackTrace(error: Throwable)

    fun show()

    fun close()

    // Invoked when the user changes any Console-related settings in the Config
    fun onConsoleSettingsChanged(settings: Config.ConsoleSettings)
}

fun Any.printToConsole(console: Console = ConsoleManager.getConsole(), logType: LogType = LogType.INFO) {
    console.println(this, logType)
}

fun Throwable.printTraceToConsole(console: Console = ConsoleManager.getConsole()) {
    this.printStackTrace()
    console.printStackTrace(this)
}
