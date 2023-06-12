package com.chattriggers.ctjs.console

import kotlinx.serialization.Serializable

@Serializable
enum class LogType {
    INFO,
    WARN,
    ERROR,
}
