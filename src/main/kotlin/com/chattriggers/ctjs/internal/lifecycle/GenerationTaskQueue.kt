package com.chattriggers.ctjs.internal.lifecycle

internal class GenerationTaskQueue {
    private val tasks = mutableListOf<ScheduledTaskHandle>()

    fun schedule(delay: Int, owner: RuntimeOwner, callback: () -> Unit) {
        val handle = ScheduledTaskHandle(delay, owner, callback)
        if (!owner.register(handle))
            return

        synchronized(tasks) {
            tasks += handle
        }
    }

    fun tick(submit: (() -> Unit) -> Unit) {
        val due = mutableListOf<ScheduledTaskHandle>()
        synchronized(tasks) {
            tasks.removeAll { task ->
                when {
                    task.isInvalidated() -> true
                    task.tick() -> {
                        due += task
                        true
                    }
                    else -> false
                }
            }
        }
        due.forEach { task -> submit(task::execute) }
    }

    fun pruneInvalidated() {
        synchronized(tasks) {
            tasks.removeAll(ScheduledTaskHandle::isInvalidated)
        }
    }

    fun pendingCount(): Int = synchronized(tasks) { tasks.size }
}

internal class ScheduledTaskHandle(
    private var delay: Int,
    private val owner: RuntimeOwner,
    callback: () -> Unit,
) : OwnedHandle {
    override val kind = OwnedKind.TASK

    @Volatile
    private var callback: (() -> Unit)? = callback

    fun tick(): Boolean = delay-- <= 0

    fun execute() {
        owner.execute(this, consume = true) {
            val action = callback
            callback = null
            action?.invoke()
        }
    }

    fun isInvalidated(): Boolean = callback == null

    override fun invalidate() {
        callback = null
    }
}
