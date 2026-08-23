package com.chattriggers.ctjs.api.message

import kotlin.test.Test
import kotlin.test.assertEquals

class ChatLibCoreSmokeTest {
    @Test
    fun `formatting conversion round trips`() {
        val colored = ChatLib.addColor("&aHello &lworld")
        assertEquals("§aHello §lworld", colored)
        assertEquals("&aHello &lworld", ChatLib.replaceFormatting(colored))
        assertEquals("Hello world", ChatLib.removeFormatting(colored))
    }

    @Test
    fun `empty escaped and null formatting inputs are stable`() {
        assertEquals("", ChatLib.addColor(""))
        assertEquals("\\&a", ChatLib.addColor("\\&a"))
        assertEquals("null", ChatLib.addColor(null))
    }
}
