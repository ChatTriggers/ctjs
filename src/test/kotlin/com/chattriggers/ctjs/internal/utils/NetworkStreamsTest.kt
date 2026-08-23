package com.chattriggers.ctjs.internal.utils

import java.io.ByteArrayInputStream
import java.io.FilterInputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NetworkStreamsTest {
    @Test
    fun `input and HTTP connection close after success`() {
        val connection = TestConnection()
        assertEquals("payload", NetworkStreams.useInput(connection) { it.reader().readText() })
        assertTrue(connection.streamClosed)
        assertTrue(connection.disconnected)
    }

    @Test
    fun `input and HTTP connection close after failure`() {
        val connection = TestConnection()
        assertFailsWith<IllegalStateException> {
            NetworkStreams.useInput(connection) { error("failure") }
        }
        assertTrue(connection.streamClosed)
        assertTrue(connection.disconnected)
    }

    private class TestConnection : HttpURLConnection(URL("http://localhost/test")) {
        var streamClosed = false
        var disconnected = false

        override fun getInputStream() = object : FilterInputStream(ByteArrayInputStream("payload".toByteArray())) {
            override fun close() {
                streamClosed = true
                super.close()
            }
        }

        override fun disconnect() {
            disconnected = true
        }

        override fun usingProxy() = false
        override fun connect() = Unit
    }
}
