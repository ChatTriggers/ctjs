package com.chattriggers.ctjs.internal.launch

import com.chattriggers.ctjs.engine.LogType
import com.chattriggers.ctjs.engine.printToConsole
import com.chattriggers.ctjs.engine.printTraceToConsole
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint

class CTJSPreLaunch : PreLaunchEntrypoint {
    override fun onPreLaunch() {
        val prevHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            "Uncaught exception in thread \"${thread.name}\"".printToConsole(LogType.ERROR)
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
