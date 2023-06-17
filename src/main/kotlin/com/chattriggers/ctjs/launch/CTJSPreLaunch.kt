package com.chattriggers.ctjs.launch

import com.chattriggers.ctjs.console.LogType
import com.chattriggers.ctjs.console.printToConsole
import com.chattriggers.ctjs.console.printTraceToConsole
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint

class CTJSPreLaunch : PreLaunchEntrypoint {
    override fun onPreLaunch() {
        val prevHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            "Uncaught exception in thread \"${thread.name}\"".printToConsole(logType = LogType.ERROR)
            exception.printTraceToConsole()
            prevHandler.uncaughtException(thread, exception)
        }

        try {
            DynamicMixinManager.applyMixins()
        } catch (e: Throwable) {
            IllegalStateException("Error generating dynamic mixins", e).printTraceToConsole()
        }
    }
}
