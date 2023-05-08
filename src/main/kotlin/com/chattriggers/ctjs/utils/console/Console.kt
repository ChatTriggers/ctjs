package com.chattriggers.ctjs.utils.console

import java.awt.Color

interface Console {
    fun clear()

    fun println(obj: Any, logType: LogType, end: String, customColor: Color?)

    // TODO: Are these needed?
    fun println(obj: Any, logType: LogType, end: String) = println(obj, logType, end, null)
    fun println(obj: Any, logType: LogType) = println(obj, logType, "\n")
    fun println(obj: Any) = println(obj, LogType.INFO)

    fun printStackTrace(error: Throwable)

    fun show()

    // Invoked when the user changes any Console-related settings in the Config
    fun onConsoleSettingsChanged()
}

fun Any.printToConsole(console: Console = ConsoleManager.getConsole(), logType: LogType = LogType.INFO) {
    console.println(this, logType)
}

fun Throwable.printTraceToConsole(console: Console = ConsoleManager.getConsole()) {
    console.printStackTrace(this)
    this.printStackTrace()
}
