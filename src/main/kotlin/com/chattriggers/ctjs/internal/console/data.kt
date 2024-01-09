package com.chattriggers.ctjs.internal.console

import com.chattriggers.ctjs.api.Config
import com.chattriggers.ctjs.engine.LogType
import kotlinx.serialization.Serializable

@Serializable
sealed class H2CMessage

@Serializable
class ConfigUpdateMessage(
    val textColor: Int,
    val backgroundColor: Int,
    val warningColor: Int,
    val errorColor: Int,
    val openConsoleOnError: Boolean,
    val customTheme: Boolean,
    val theme: Int,
    val useFiraCode: Boolean,
    val fontSize: Int,
) : H2CMessage() {
    companion object {
        fun constructFromConfig(settings: Config.ConsoleSettings) = ConfigUpdateMessage(
            settings.consoleTextColor.rgb,
            settings.consoleBackgroundColor.rgb,
            settings.consoleWarningColor.rgb,
            settings.consoleErrorColor.rgb,
            settings.openConsoleOnError,
            settings.customTheme,
            settings.consoleTheme,
            settings.consoleFiraCodeFont,
            settings.consoleFontSize,
        )
    }
}

@Serializable
class InitMessage(
    val modVersion: String,
    val config: ConfigUpdateMessage,
    val firaFontBytes: ByteArray?,
) : H2CMessage()

@Serializable
data object OpenMessage : H2CMessage()

@Serializable
data object TerminateMessage : H2CMessage()

@Serializable
class EvalResultMessage(val id: Int, val result: String) : H2CMessage()

@Serializable
class PrintMessage(
    val text: String,
    val logType: LogType,
    val end: String,
    val color: Int?,
) : H2CMessage()

@Serializable
class PrintErrorMessage(val error: Error) : H2CMessage() {
    @Serializable
    data class Error(val message: String, val trace: List<StackTrace>, val cause: Error?)
}

@Serializable
data class StackTrace(
    val fileName: String?,
    val className: String,
    val methodName: String,
    val lineNumber: Int,
)

@Serializable
data object ClearConsoleMessage : H2CMessage()

@Serializable
sealed class C2HMessage

@Serializable
class EvalTextMessage(val id: Int, val string: String) : C2HMessage()

@Serializable
data object ReloadCTMessage : C2HMessage()

@Serializable
class FontSizeMessage(val delta: Int) : C2HMessage()
