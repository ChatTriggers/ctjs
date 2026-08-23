package com.chattriggers.ctjs.internal.utils

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URLConnection

internal object NetworkStreams {
    fun <T> useInput(connection: URLConnection, block: (InputStream) -> T): T {
        return try {
            connection.getInputStream().use(block)
        } finally {
            (connection as? HttpURLConnection)?.disconnect()
        }
    }
}
