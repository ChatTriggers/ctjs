package com.chattriggers.ctjs.internal.console

import kotlinx.serialization.Serializable

@Serializable
enum class LogType {
    INFO,
    WARN,
    ERROR,
}
