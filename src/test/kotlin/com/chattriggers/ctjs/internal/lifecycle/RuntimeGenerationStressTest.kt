package com.chattriggers.ctjs.internal.lifecycle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RuntimeGenerationStressTest {
    @Test
    fun `ten reloads leave only the latest generation active and bounded`() {
        val controller = RuntimeGenerationController()
        val queue = GenerationTaskQueue()
        val generationIds = mutableListOf<Long>()
        val handleCounts = mutableListOf<Int>()
        var activeCalls = 0
        var oldMarkerLeaks = 0
        var systemCalls = 0

        queue.schedule(20, SystemRuntimeOwner) { systemCalls++ }

        repeat(11) { generationIndex ->
            val owner = controller.currentOwner()
            generationIds += owner.generationId

            val ui = GenerationGuard(OwnedKind.UI, owner)
            val resource = GenerationGuard(OwnedKind.RESOURCE, owner)
            val loader = GenerationGuard(OwnedKind.CLASS_LOADER, owner)
            val timeout = GenerationGuard(OwnedKind.TIMEOUT, owner)
            listOf(ui, resource, loader, timeout).forEach { assertTrue(it.register()) }

            queue.schedule(0, owner) { activeCalls++ }
            queue.schedule(100, owner) { oldMarkerLeaks++ }
            handleCounts += owner.ownedCount()

            queue.tick { it() }
            assertEquals(generationIndex + 1, activeCalls)

            if (generationIndex == 10)
                return@repeat

            val snapshot = controller.stopCurrent()
            snapshot.invalidateAll()
            queue.pruneInvalidated()

            assertFalse(ui.execute { oldMarkerLeaks++ })
            assertFalse(resource.execute { oldMarkerLeaks++ })
            assertFalse(loader.execute { oldMarkerLeaks++ })
            assertFalse(timeout.execute { oldMarkerLeaks++ })
            assertEquals(1, queue.pendingCount())

            val next = assertNotNull(controller.publishNext(snapshot))
            snapshot.markDead()
            assertEquals(GenerationState.DEAD, owner.state)
            assertEquals(GenerationState.ACTIVE, next.state)
        }

        repeat(21) { queue.tick { it() } }

        assertEquals((1L..11L).toList(), generationIds)
        assertTrue(handleCounts.all { it == handleCounts.first() })
        assertEquals(6, handleCounts.first())
        assertEquals(0, oldMarkerLeaks)
        assertEquals(1, systemCalls)
        assertEquals(GenerationState.ACTIVE, controller.currentOwner().state)
    }
}
