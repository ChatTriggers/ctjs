package com.chattriggers.ctjs.api.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TabListOverrideStateTest {
    @Test
    fun `custom header and footer restore the latest server state`() {
        val state = TabListOverrideState<String>()
        assertFalse(state.observeServerHeader("server-header-1"))
        assertFalse(state.observeServerFooter("server-footer-1"))

        state.beginCustomHeader()
        assertFalse(state.observeServerHeader("custom-header"))
        state.endCustomHeader()
        state.beginCustomFooter()
        assertFalse(state.observeServerFooter("custom-footer"))
        state.endCustomFooter()

        assertTrue(state.observeServerHeader("server-header-2"))
        assertTrue(state.observeServerFooter("server-footer-2"))
        val restored = state.clear()

        assertEquals("server-header-2" to "server-footer-2", restored)
        assertFalse(state.customHeader)
        assertFalse(state.customFooter)
        assertFalse(state.observeServerHeader("server-header-3"))
    }

}
