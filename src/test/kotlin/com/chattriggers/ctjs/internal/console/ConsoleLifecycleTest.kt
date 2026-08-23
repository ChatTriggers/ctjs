package com.chattriggers.ctjs.internal.console

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConsoleLifecycleTest {
    @Test
    fun `backlog flushes once and clears`() {
        val backlog = MessageBacklog<Int>()
        repeat(100) { backlog.add(it) }
        val delivered = mutableListOf<Int>()

        assertEquals(100, backlog.flush { delivered += it; true })
        assertEquals(0, backlog.size())
        assertEquals(0, backlog.flush { delivered += it; true })
        assertEquals((0 until 100).toList(), delivered)
    }

    @Test
    fun `failed send remains pending for reconnect`() {
        val backlog = MessageBacklog<Int>()
        backlog.add(1)
        backlog.add(2)
        assertEquals(0, backlog.flush { false })
        assertEquals(2, backlog.size())

        val delivered = mutableListOf<Int>()
        assertEquals(2, backlog.flush { delivered += it; true })
        assertEquals(listOf(1, 2), delivered)
    }

    @Test
    fun `completed eval futures are removed`() {
        val pending = PendingRequestRegistry<String>()
        val futures = (0 until 100).associateWith(pending::create)
        assertEquals(100, pending.size())
        futures.forEach { (id, future) ->
            assertTrue(pending.complete(id, "result-$id"))
            assertEquals("result-$id", future.get())
        }
        assertEquals(0, pending.size())
        assertFalse(pending.complete(0, "duplicate"))
    }
}
