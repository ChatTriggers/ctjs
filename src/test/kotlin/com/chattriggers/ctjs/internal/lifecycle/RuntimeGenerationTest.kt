package com.chattriggers.ctjs.internal.lifecycle

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RuntimeGenerationTest {
    @Test
    fun `generation ids increase and old owners reach dead`() {
        val controller = RuntimeGenerationController(40L)
        val first = controller.currentOwner()
        val snapshot = controller.stopCurrent()

        assertEquals(GenerationState.STOPPING, first.state)
        val second = assertNotNull(controller.publishNext(snapshot))
        assertEquals(41L, second.generationId)
        assertTrue(second.generationId > first.generationId)

        snapshot.markDead()
        assertEquals(GenerationState.DEAD, first.state)
        assertEquals(GenerationState.ACTIVE, second.state)
    }

    @Test
    fun `only the latest reload transition can publish`() {
        val controller = RuntimeGenerationController()
        val stale = controller.stopCurrent()
        val latest = controller.stopCurrent()

        assertNull(controller.publishNext(stale))
        assertNotNull(controller.publishNext(latest))
    }

    @Test
    fun `a deliberate load can publish after an unload reached dead`() {
        val controller = RuntimeGenerationController()
        val unload = controller.stopCurrent()
        unload.markDead()

        val load = controller.stopCurrent()
        assertNotNull(controller.publishNext(load))
    }

    @Test
    fun `stopping and dead owners reject new work`() {
        val owner = RuntimeGenerationController().currentOwner()
        val snapshot = owner.stop()
        var invalidations = 0
        val stoppingHandle = TestHandle(OwnedKind.TASK) { invalidations++ }

        assertFalse(owner.register(stoppingHandle))
        assertEquals(1, invalidations)
        snapshot.markDead()

        val deadHandle = TestHandle(OwnedKind.UI) { invalidations++ }
        assertFalse(owner.register(deadHandle))
        assertEquals(2, invalidations)
    }

    @Test
    fun `resource snapshots destroy old only and are idempotent`() {
        val controller = RuntimeGenerationController()
        var oldDestroyed = 0
        var newDestroyed = 0
        val old = GenerationGuard(OwnedKind.RESOURCE, controller.currentOwner()) { oldDestroyed++ }
        assertTrue(old.register())

        val snapshot = controller.stopCurrent()
        val next = assertNotNull(controller.publishNext(snapshot))
        val fresh = GenerationGuard(OwnedKind.RESOURCE, next) { newDestroyed++ }
        assertTrue(fresh.register())

        snapshot.invalidate(OwnedKind.RESOURCE)
        snapshot.invalidate(OwnedKind.RESOURCE)
        assertEquals(1, oldDestroyed)
        assertEquals(0, newDestroyed)
        assertTrue(fresh.isActive())
    }

    @Test
    fun `old ui callbacks stop and new callbacks execute`() {
        val controller = RuntimeGenerationController()
        var oldCalls = 0
        var newCalls = 0
        val old = GenerationGuard(OwnedKind.UI, controller.currentOwner())
        assertTrue(old.register())
        assertTrue(old.execute { oldCalls++ })

        val snapshot = controller.stopCurrent()
        snapshot.invalidate(OwnedKind.UI)
        assertFalse(old.execute { oldCalls++ })

        val next = assertNotNull(controller.publishNext(snapshot))
        val fresh = GenerationGuard(OwnedKind.UI, next)
        assertTrue(fresh.register())
        assertTrue(fresh.execute { newCalls++ })
        assertEquals(1, oldCalls)
        assertEquals(1, newCalls)
    }

    @Test
    fun `queued and delayed tasks obey generation while system tasks survive`() {
        val controller = RuntimeGenerationController()
        val queue = GenerationTaskQueue()
        var oldCalls = 0
        var newCalls = 0
        var systemCalls = 0

        queue.schedule(0, controller.currentOwner()) { oldCalls++ }
        queue.schedule(1, SystemRuntimeOwner) { systemCalls++ }
        val snapshot = controller.stopCurrent()
        snapshot.invalidate(OwnedKind.TASK)
        queue.pruneInvalidated()

        val next = assertNotNull(controller.publishNext(snapshot))
        queue.schedule(1, next) { newCalls++ }
        queue.tick { it() }
        assertEquals(0, oldCalls)
        assertEquals(0, newCalls)
        assertEquals(0, systemCalls)

        queue.tick { it() }
        assertEquals(1, newCalls)
        assertEquals(1, systemCalls)
    }

    @Test
    fun `already submitted task is rejected if reload wins`() {
        val controller = RuntimeGenerationController()
        val queue = GenerationTaskQueue()
        var calls = 0
        var submitted: (() -> Unit)? = null
        queue.schedule(0, controller.currentOwner()) { calls++ }
        queue.tick { submitted = it }
        assertNotNull(submitted)

        val snapshot = controller.stopCurrent()
        snapshot.invalidate(OwnedKind.TASK)
        submitted.invoke()
        assertEquals(0, calls)
    }

    @Test
    fun `timeouts before reload are cancelled and new timeouts run`() {
        val controller = RuntimeGenerationController()
        val executor = Executors.newSingleThreadScheduledExecutor()
        val scheduler = GenerationTimeoutScheduler(controller::currentOwner, executor, Runnable::run)
        try {
            val oldCalls = AtomicInteger()
            val newCalls = AtomicInteger()
            val old = scheduler.schedule(200L) { oldCalls.incrementAndGet() }
            assertNotNull(old)

            val snapshot = controller.stopCurrent()
            snapshot.invalidate(OwnedKind.TIMEOUT)
            assertNotNull(controller.publishNext(snapshot))
            val fresh = scheduler.schedule(0L) { newCalls.incrementAndGet() }
            assertNotNull(fresh)

            executor.shutdown()
            assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS))
            assertEquals(0, oldCalls.get())
            assertEquals(1, newCalls.get())
        } finally {
            scheduler.close()
        }
    }

    @Test
    fun `timeout callback queued at reload boundary is rejected`() {
        val controller = RuntimeGenerationController()
        val owner = controller.currentOwner()
        var calls = 0
        val handle = GenerationTimeoutHandle(owner, Runnable { calls++ }, Runnable::run)
        assertTrue(owner.register(handle))

        val snapshot = controller.stopCurrent()
        snapshot.invalidate(OwnedKind.TIMEOUT)
        handle.execute()
        assertEquals(0, calls)
    }

    private class TestHandle(
        override val kind: OwnedKind,
        private val invalidator: () -> Unit,
    ) : OwnedHandle {
        override fun invalidate() = invalidator()
    }
}
