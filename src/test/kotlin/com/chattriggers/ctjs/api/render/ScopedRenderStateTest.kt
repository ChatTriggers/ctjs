package com.chattriggers.ctjs.api.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScopedRenderStateTest {
    @Test
    fun `renderer cull facade starts enabled and updates intent`() {
        Renderer.enableCull()
        try {
            assertTrue(Renderer.isCullEnabled())
            Renderer.disableCull()
            assertFalse(Renderer.isCullEnabled())
            Renderer.enableCull()
            assertTrue(Renderer.isCullEnabled())
        } finally {
            Renderer.enableCull()
        }
    }

    @Test
    fun `nested scope restores the outer disabled state`() {
        val state = ScopedRenderState(initialValue = true)
        val outer = state.push(false)
        assertFalse(state.current)

        val inner = state.push(true)
        assertTrue(state.current)
        state.restore(inner)
        assertFalse(state.current)

        state.restore(outer)
        assertTrue(state.current)
    }

    @Test
    fun `exception restores state`() {
        val state = ScopedRenderState(initialValue = true)

        assertFailsWith<IllegalStateException> {
            state.withValue(false) {
                assertFalse(state.current)
                error("draw failure")
            }
        }

        assertTrue(state.current)
    }

    @Test
    fun `scope close is idempotent and out of order restore is rejected`() {
        val state = ScopedRenderState(initialValue = "default")
        val outer = state.push("outer")
        val inner = state.push("inner")

        assertFailsWith<IllegalStateException> { state.restore(outer) }
        assertEquals("inner", state.current)
        state.restore(inner)
        state.restore(inner)
        assertEquals("outer", state.current)
        state.restore(outer)
        assertEquals("default", state.current)
    }
}
