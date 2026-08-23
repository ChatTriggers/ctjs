package com.chattriggers.ctjs.api.render

/**
 * Render-thread state with explicit, nestable restoration.
 *
 * This stores CTJS pipeline intent only. It never mutates Minecraft's global
 * render state, which makes it safe to use while vanilla and other mods are
 * building their own render pipelines.
 */
internal class ScopedRenderState<T>(initialValue: T) {
    private val scopes = ArrayDeque<Scope<T>>()

    var current: T = initialValue
        private set

    internal val depth: Int
        get() = scopes.size

    fun set(value: T) {
        current = value
    }

    fun push(value: T = current): Scope<T> {
        val scope = Scope(this, current)
        scopes.addLast(scope)
        current = value
        return scope
    }

    fun restore(scope: Scope<T>) {
        if (scope.closed)
            return
        require(scope.owner === this) { "Render state scope belongs to a different state" }
        check(scopes.lastOrNull() === scope) { "Render state scopes must be restored in LIFO order" }

        scopes.removeLast()
        current = scope.previous
        scope.closed = true
    }

    fun <R> withValue(value: T, block: () -> R): R {
        val scope = push(value)
        return try {
            block()
        } finally {
            restore(scope)
        }
    }

    internal class Scope<T> internal constructor(
        internal val owner: ScopedRenderState<T>,
        internal val previous: T,
    ) {
        internal var closed = false
    }
}
