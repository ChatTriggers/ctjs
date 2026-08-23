package com.chattriggers.ctjs.api.triggers

import com.chattriggers.ctjs.api.client.Client

class StepTrigger(method: Any) : Trigger(method, TriggerType.STEP) {
    private var schedule: StepSchedule? = null

    init {
        schedule = StepSchedule(Client.getSystemTime())
    }

    /**
     * Sets the frames per second that the trigger activates.
     * Values above 1000 use the minimum one millisecond interval. If rendering
     * falls behind, at most [StepSchedule.MAX_CATCH_UP_STEPS] callbacks are run
     * in one frame before the schedule is resynchronized.
     * @param fps the frames per second to set
     * @return the trigger for method chaining
     */
    fun setFps(fps: Long) = apply {
        schedule!!.setFps(fps, Client.getSystemTime())
    }

    /**
     * Sets the delay in seconds between the trigger activation.
     * This has a minimum of one step every second. This will override [setFps].
     * @param delay The delay in seconds
     * @return the trigger for method chaining
     */
    fun setDelay(delay: Long) = apply {
        schedule!!.setDelay(delay, Client.getSystemTime())
    }

    override fun register(): Trigger {
        // Trigger's constructor calls this override before subclass fields are initialized.
        schedule?.reset(Client.getSystemTime())
        return super.register()
    }

    override fun trigger(args: Array<out Any?>) {
        schedule!!.poll(Client.getSystemTime()).forEach {
            callMethod(arrayOf(it))
        }
    }
}

internal class StepSchedule(initialTime: Long) {
    private var intervalMillis = DEFAULT_INTERVAL_MILLIS
    private var nextRunAt = initialTime
    private var elapsed = 0L

    fun setFps(fps: Long, now: Long) {
        val safeFps = fps.coerceAtLeast(1L)
        intervalMillis = (MILLIS_PER_SECOND / safeFps).coerceAtLeast(1L)
        nextRunAt = saturatingAdd(now, intervalMillis)
    }

    fun setDelay(delay: Long, now: Long) {
        intervalMillis = saturatingMultiply(delay.coerceAtLeast(1L), MILLIS_PER_SECOND)
        // A delay controls the spacing between callbacks; the first callback remains immediate.
        nextRunAt = now
    }

    fun reset(now: Long) {
        nextRunAt = now
    }

    fun poll(now: Long): LongArray {
        if (now < nextRunAt)
            return LongArray(0)

        val totalDue = (now - nextRunAt) / intervalMillis + 1L
        val due = totalDue.coerceAtMost(MAX_CATCH_UP_STEPS.toLong()).toInt()
        val wasCapped = totalDue > MAX_CATCH_UP_STEPS

        nextRunAt = if (wasCapped) {
            saturatingAdd(now, intervalMillis)
        } else {
            saturatingAdd(nextRunAt, saturatingMultiply(intervalMillis, due.toLong()))
        }

        return LongArray(due) { ++elapsed }
    }

    companion object {
        internal const val MAX_CATCH_UP_STEPS = 10
        private const val MILLIS_PER_SECOND = 1000L
        private const val DEFAULT_INTERVAL_MILLIS = MILLIS_PER_SECOND / 60L

        private fun saturatingAdd(left: Long, right: Long): Long =
            if (right > 0L && left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right

        private fun saturatingMultiply(left: Long, right: Long): Long =
            if (left > Long.MAX_VALUE / right) Long.MAX_VALUE else left * right
    }
}
