package com.chattriggers.ctjs.internal.lifecycle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GenerationCallbackSlotTest {
    @Test
    fun `old handler stops and current handler runs once`() {
        val controller = RuntimeGenerationController()
        val slot = GenerationCallbackSlot<String>()
        assertTrue(slot.publish("old", controller.currentOwner()))
        assertEquals("old", slot.invoke { it }.value)

        val old = controller.stopCurrent()
        assertFalse(slot.invoke { error("old handler ran") }.invoked)
        val next = assertNotNull(controller.publishNext(old))
        assertTrue(slot.publish("new", next))
        assertEquals("new", slot.invoke { it }.value)
        old.markDead()
    }

    @Test
    fun `stale and failed setup owners cannot publish`() {
        val controller = RuntimeGenerationController()
        val slot = GenerationCallbackSlot<String>()
        val stale = controller.currentOwner()
        val snapshot = controller.stopCurrent()

        assertFalse(slot.publish("stale", stale))
        assertFalse(slot.invoke { it }.invoked)

        val current = assertNotNull(controller.publishNext(snapshot))
        assertTrue(slot.publish("current", current))
        val failed = controller.stopCurrent()
        failed.markDead()
        assertFalse(slot.invoke { it }.invoked)
    }

    @Test
    fun `ten generation swaps remain bounded`() {
        val controller = RuntimeGenerationController()
        val slot = GenerationCallbackSlot<Int>()

        repeat(11) { index ->
            val owner = controller.currentOwner()
            assertTrue(slot.publish(index, owner))
            assertEquals(index, slot.invoke { it }.value)
            assertEquals(1, owner.ownedCount(OwnedKind.CALLBACK))
            if (index < 10) {
                val snapshot = controller.stopCurrent()
                assertFalse(slot.invoke { it }.invoked)
                assertNotNull(controller.publishNext(snapshot))
                snapshot.markDead()
            }
        }
    }
}
