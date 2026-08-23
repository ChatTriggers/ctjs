package com.chattriggers.ctjs.api.triggers

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StepTriggerTest {
    @Test
    fun `supported fps values use a finite positive interval`() {
        val cases = mapOf(
            1L to 1000L,
            60L to 16L,
            1000L to 1L,
            1001L to 1L,
            Long.MAX_VALUE to 1L,
        )

        cases.forEach { (fps, firstDueAt) ->
            val schedule = StepSchedule(0L)
            schedule.setFps(fps, 0L)

            assertTrue(schedule.poll(firstDueAt - 1L).isEmpty(), "fps=$fps fired early")
            assertContentEquals(longArrayOf(1L), schedule.poll(firstDueAt), "fps=$fps")
        }
    }

    @Test
    fun `sixty fps keeps its legacy integer millisecond cadence`() {
        val schedule = StepSchedule(0L)
        schedule.setFps(60L, 0L)

        assertContentEquals(longArrayOf(1L), schedule.poll(16L))
        assertTrue(schedule.poll(31L).isEmpty())
        assertContentEquals(longArrayOf(2L), schedule.poll(32L))
    }

    @Test
    fun `catch up work is bounded and resynchronized`() {
        listOf(1L, 60L, 1000L, 1001L, Long.MAX_VALUE).forEach { fps ->
            val schedule = StepSchedule(0L)
            schedule.setFps(fps, 0L)

            val callbacks = schedule.poll(1_000_000L)
            assertEquals(StepSchedule.MAX_CATCH_UP_STEPS, callbacks.size, "fps=$fps")
            assertContentEquals(LongArray(StepSchedule.MAX_CATCH_UP_STEPS) { it + 1L }, callbacks)
            assertTrue(schedule.poll(1_000_000L).isEmpty(), "fps=$fps was not resynchronized")
        }
    }

    @Test
    fun `maximum delay does not overflow`() {
        val schedule = StepSchedule(0L)
        schedule.setDelay(Long.MAX_VALUE, 0L)

        assertContentEquals(longArrayOf(1L), schedule.poll(0L))
        assertTrue(schedule.poll(Long.MAX_VALUE - 1L).isEmpty())
        assertContentEquals(longArrayOf(2L), schedule.poll(Long.MAX_VALUE))
    }
}
