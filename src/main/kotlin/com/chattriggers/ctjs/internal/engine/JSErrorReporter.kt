package com.chattriggers.ctjs.internal.engine

import com.chattriggers.ctjs.engine.Console
import org.mozilla.javascript.ErrorReporter
import org.mozilla.javascript.EvaluatorException

class JSErrorReporter(private val console: Console) : ErrorReporter {
    override fun warning(message: String?, sourceName: String?, line: Int, lineSource: String?, lineOffset: Int) {
        reportErrorMessage(message, sourceName, line, lineSource, lineOffset, isWarning = true)
    }

    override fun error(message: String?, sourceName: String?, line: Int, lineSource: String?, lineOffset: Int) {
        reportErrorMessage(message, sourceName, line, lineSource, lineOffset, isWarning = false)
    }

    private fun reportErrorMessage(
        inputMessage: String?,
        sourceName: String?,
        line: Int,
        lineSource: String?,
        lineOffset: Int,
        isWarning: Boolean,
    ) {
        var message = if (line > 0) {
            if (sourceName == null) {
                "line $line: $inputMessage"
            } else "\"$sourceName\", line $line: $inputMessage"
        } else inputMessage

        if (isWarning)
            message = "warning: $message"

        console.println(MESSAGE_PREFIX + message)
        if (lineSource != null) {
            console.println(MESSAGE_PREFIX + lineSource)
            console.println(MESSAGE_PREFIX + buildIndicator(lineOffset))
        }
    }

    private fun buildIndicator(offset: Int) = buildString {
        repeat(offset) { append('.') }
        append('^')
    }

    override fun runtimeError(
        message: String?,
        sourceName: String?,
        line: Int,
        lineSource: String?,
        lineOffset: Int
    ): EvaluatorException {
        return EvaluatorException(message, sourceName, line, lineSource, lineOffset)
    }

    companion object {
        const val MESSAGE_PREFIX = "js: "
    }
}
