package com.chattriggers.ctjs.utils.console

import com.chattriggers.ctjs.engine.module.ModuleManager
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

    fun hide()

    // Invoked when the user changes any Console-related settings in the Config
    fun onConsoleSettingsChanged()
}

fun Any.printToConsole(console: Console = ModuleManager.generalConsole, logType: LogType = LogType.INFO) {
    console.println(this, logType)
}

fun Throwable.printTraceToConsole(console: Console = ModuleManager.generalConsole) {
    console.printStackTrace(this)
    this.printStackTrace()
}

class DummyConsole : Console {
    override fun clear() {}

    override fun println(obj: Any, logType: LogType, end: String, customColor: Color?) {
        kotlin.io.println("$obj$end")
    }

    override fun printStackTrace(error: Throwable) {
        error.printStackTrace()
    }

    override fun show() {}

    override fun hide() {}

    override fun onConsoleSettingsChanged() {}
}
