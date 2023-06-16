package com.chattriggers.ctjs.launch

import com.chattriggers.ctjs.console.LogType
import com.chattriggers.ctjs.console.printToConsole
import com.chattriggers.ctjs.console.printTraceToConsole
import com.chattriggers.ctjs.engine.module.ModuleManager
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint

class CTJSPreLaunch : PreLaunchEntrypoint {
    override fun onPreLaunch() {
        val prevHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            "Uncaught exception in thread \"${thread.name}\"".printToConsole(logType = LogType.ERROR)
            exception.printTraceToConsole()
            prevHandler.uncaughtException(thread, exception)
        }

        Mappings.initialize()

        // TODO: Threaded setup?
        ModuleManager.setup()

        try {
            // TODO: Dynamic mixins
        } catch (e: Throwable) {
            IllegalStateException("Error generating dynamic mixins", e).printTraceToConsole()
        }
    }
}
