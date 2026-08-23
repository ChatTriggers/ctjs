package com.chattriggers.ctjs.internal.lifecycle

import com.chattriggers.ctjs.engine.printTraceToConsole
import com.chattriggers.ctjs.internal.engine.JSContextFactory
import org.mozilla.javascript.Context
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

internal class GenerationTimeoutScheduler(
    private val ownerProvider: () -> RuntimeOwner,
    private val executor: ScheduledExecutorService,
    private val callbackRunner: (Runnable) -> Unit,
) : AutoCloseable {
    fun schedule(delayMillis: Long, callback: Runnable): GenerationTimeoutHandle? {
        val owner = ownerProvider()
        val handle = GenerationTimeoutHandle(owner, callback, callbackRunner)
        if (!owner.register(handle))
            return null

        val future = executor.schedule(handle::execute, delayMillis.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
        handle.attach(future)
        return handle
    }

    override fun close() {
        executor.shutdownNow()
    }
}

internal class GenerationTimeoutHandle(
    private val owner: RuntimeOwner,
    callback: Runnable,
    private val callbackRunner: (Runnable) -> Unit,
) : OwnedHandle {
    override val kind = OwnedKind.TIMEOUT
    private val future = AtomicReference<ScheduledFuture<*>?>()

    @Volatile
    private var callback: Runnable? = callback

    fun attach(scheduledFuture: ScheduledFuture<*>) {
        if (!future.compareAndSet(null, scheduledFuture) || callback == null)
            scheduledFuture.cancel(false)
    }

    fun execute() {
        owner.execute(this, consume = true) {
            val action = callback
            callback = null
            future.set(null)
            if (action != null)
                callbackRunner(action)
        }
    }

    override fun invalidate() {
        callback = null
        future.getAndSet(null)?.cancel(false)
    }

    fun cancel() {
        owner.unregister(this)
        invalidate()
    }
}

internal object GenerationTimeouts {
    private val scheduler = GenerationTimeoutScheduler(
        ownerProvider = RuntimeGenerations::currentOwner,
        executor = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "CTJS generation timeout").apply { isDaemon = true }
        },
        callbackRunner = { callback ->
            var enteredContext = false
            try {
                JSContextFactory.enterContext()
                enteredContext = true
                callback.run()
            } catch (e: Throwable) {
                e.printTraceToConsole()
            } finally {
                if (enteredContext)
                    Context.exit()
            }
        },
    )

    @JvmStatic
    fun schedule(delayMillis: Long, callback: Runnable): GenerationTimeoutHandle? =
        scheduler.schedule(delayMillis, callback)

    @JvmStatic
    fun cancel(handle: Any?) {
        (handle as? GenerationTimeoutHandle)?.cancel()
    }
}
