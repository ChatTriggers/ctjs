package com.chattriggers.ctjs.internal.lifecycle

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal enum class GenerationState {
    ACTIVE,
    STOPPING,
    DEAD,
}

internal enum class OwnedKind {
    TASK,
    TIMEOUT,
    CLASS_LOADER,
    RESOURCE,
    UI,
    CALLBACK,
}

internal interface OwnedHandle {
    val kind: OwnedKind
    fun invalidate()
}

internal sealed interface RuntimeOwner {
    val generationId: Long
    val state: GenerationState

    fun register(handle: OwnedHandle): Boolean
    fun unregister(handle: OwnedHandle)
    fun execute(handle: OwnedHandle, consume: Boolean = false, callback: () -> Unit): Boolean
}

internal object SystemRuntimeOwner : RuntimeOwner {
    override val generationId = 0L
    override val state = GenerationState.ACTIVE

    override fun register(handle: OwnedHandle) = true
    override fun unregister(handle: OwnedHandle) = Unit

    override fun execute(handle: OwnedHandle, consume: Boolean, callback: () -> Unit): Boolean {
        callback()
        return true
    }
}

internal class ModuleRuntimeOwner internal constructor(
    override val generationId: Long,
) : RuntimeOwner {
    private val lock = Any()
    private val handles = linkedSetOf<OwnedHandle>()

    @Volatile
    override var state = GenerationState.ACTIVE
        private set

    override fun register(handle: OwnedHandle): Boolean {
        val accepted = synchronized(lock) {
            if (state == GenerationState.ACTIVE) {
                handles += handle
                true
            } else {
                false
            }
        }

        if (!accepted)
            handle.invalidate()
        return accepted
    }

    override fun unregister(handle: OwnedHandle) {
        synchronized(lock) {
            handles -= handle
        }
    }

    override fun execute(handle: OwnedHandle, consume: Boolean, callback: () -> Unit): Boolean = synchronized(lock) {
        if (state != GenerationState.ACTIVE || handle !in handles)
            return@synchronized false

        try {
            callback()
            true
        } finally {
            if (consume)
                handles -= handle
        }
    }

    internal fun stop(): GenerationSnapshot {
        val detached = synchronized(lock) {
            if (state == GenerationState.ACTIVE)
                state = GenerationState.STOPPING
            if (state == GenerationState.DEAD)
                return@synchronized emptyList()

            handles.toList().also { handles.clear() }
        }
        return GenerationSnapshot(this, detached)
    }

    internal fun markDead() {
        val remaining = synchronized(lock) {
            state = GenerationState.DEAD
            handles.toList().also { handles.clear() }
        }
        remaining.forEach(OwnedHandle::invalidate)
    }

    internal fun ownedCount(kind: OwnedKind? = null): Int = synchronized(lock) {
        if (kind == null) handles.size else handles.count { it.kind == kind }
    }
}

internal class GenerationSnapshot internal constructor(
    val owner: ModuleRuntimeOwner,
    handles: List<OwnedHandle>,
) {
    internal var transitionToken = 0L
    private val detached = handles.groupBy(OwnedHandle::kind)
    private val invalidatedKinds = mutableSetOf<OwnedKind>()

    fun invalidate(kind: OwnedKind) {
        val handles = synchronized(invalidatedKinds) {
            if (!invalidatedKinds.add(kind))
                return
            detached[kind].orEmpty()
        }
        handles.forEach(OwnedHandle::invalidate)
    }

    fun invalidateAll() {
        OwnedKind.entries.forEach(::invalidate)
    }

    fun markDead() {
        invalidateAll()
        owner.markDead()
    }

    fun count(kind: OwnedKind): Int = detached[kind]?.size ?: 0
}

internal class RuntimeGenerationController(initialGenerationId: Long = 1L) {
    private val lock = Any()
    private var nextGenerationId = initialGenerationId
    private var transitionToken = 0L

    @Volatile
    private var current = ModuleRuntimeOwner(initialGenerationId)

    fun currentOwner(): ModuleRuntimeOwner = current

    fun stopCurrent(): GenerationSnapshot = synchronized(lock) {
        current.stop().also { it.transitionToken = ++transitionToken }
    }

    fun publishNext(snapshot: GenerationSnapshot): ModuleRuntimeOwner? = synchronized(lock) {
        val previous = snapshot.owner
        if (
            current !== previous ||
            snapshot.transitionToken != transitionToken ||
            previous.state == GenerationState.ACTIVE
        )
            return@synchronized null

        ModuleRuntimeOwner(++nextGenerationId).also { current = it }
    }
}

internal object RuntimeGenerations {
    private val controller = RuntimeGenerationController()

    fun currentOwner(): ModuleRuntimeOwner = controller.currentOwner()
    fun systemOwner(): RuntimeOwner = SystemRuntimeOwner
    fun stopCurrent(): GenerationSnapshot = controller.stopCurrent()
    fun publishNext(snapshot: GenerationSnapshot): ModuleRuntimeOwner? = controller.publishNext(snapshot)
}

internal class GenerationGuard(
    override val kind: OwnedKind,
    val owner: RuntimeOwner = RuntimeGenerations.currentOwner(),
    private val onInvalidate: () -> Unit = {},
) : OwnedHandle {
    private val valid = AtomicBoolean(true)
    private val registered = AtomicBoolean(false)

    fun register(): Boolean {
        if (!registered.compareAndSet(false, true))
            return valid.get()
        return owner.register(this)
    }

    fun isActive(): Boolean = valid.get() && owner.state == GenerationState.ACTIVE

    fun execute(callback: () -> Unit): Boolean {
        if (!valid.get())
            return false
        return owner.execute(this) {
            if (valid.get())
                callback()
        }
    }

    fun close() {
        owner.unregister(this)
        invalidate()
    }

    override fun invalidate() {
        if (valid.compareAndSet(true, false))
            onInvalidate()
    }
}

internal data class CallbackInvocation<out T>(
    val invoked: Boolean,
    val value: T?,
)

/**
 * Atomically publishes one callback owned by a runtime generation.
 *
 * The slot itself is stable and may safely be retained by injected/bootstrap code. The
 * published value is only visible while its owner is ACTIVE, and is detached as part of
 * normal generation invalidation.
 */
internal class GenerationCallbackSlot<T> {
    private data class Entry<T>(
        val value: T,
        val guard: GenerationGuard,
    )

    private val current = AtomicReference<Entry<T>?>(null)

    fun publish(value: T, owner: RuntimeOwner = RuntimeGenerations.currentOwner()): Boolean {
        lateinit var entry: Entry<T>
        val guard = GenerationGuard(OwnedKind.CALLBACK, owner) {
            current.compareAndSet(entry, null)
        }
        entry = Entry(value, guard)

        if (!guard.register())
            return false

        val previous = current.getAndSet(entry)
        previous?.guard?.close()

        if (!guard.isActive()) {
            current.compareAndSet(entry, null)
            guard.close()
            return false
        }
        return true
    }

    fun isActive(): Boolean = current.get()?.guard?.isActive() == true

    fun <R> invoke(callback: (T) -> R): CallbackInvocation<R> {
        val entry = current.get() ?: return CallbackInvocation(false, null)
        var result: R? = null
        val invoked = entry.guard.execute {
            result = callback(entry.value)
        }
        return CallbackInvocation(invoked, result)
    }

    fun clear() {
        current.getAndSet(null)?.guard?.close()
    }
}
