package com.chattriggers.ctjs.internal.console

import java.util.ArrayDeque
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

internal class MessageBacklog<T> {
    private val lock = Any()
    private val messages = ArrayDeque<T>()

    fun add(message: T) {
        synchronized(lock) { messages.addLast(message) }
    }

    fun flush(send: (T) -> Boolean): Int = synchronized(lock) {
        var sent = 0
        while (messages.isNotEmpty()) {
            val message = messages.first()
            if (!send(message))
                break
            messages.removeFirst()
            sent++
        }
        sent
    }

    fun clear() = synchronized(lock) { messages.clear() }

    fun size(): Int = synchronized(lock) { messages.size }
}

internal class PendingRequestRegistry<T> {
    private val futures = ConcurrentHashMap<Int, CompletableFuture<T>>()

    fun create(id: Int): CompletableFuture<T> = CompletableFuture<T>().also {
        check(futures.putIfAbsent(id, it) == null) { "Duplicate pending request id $id" }
    }

    fun complete(id: Int, value: T): Boolean = futures.remove(id)?.complete(value) == true

    fun failAll(cause: Throwable) {
        futures.entries.toList().forEach { (id, future) ->
            if (futures.remove(id, future))
                future.completeExceptionally(cause)
        }
    }

    fun size(): Int = futures.size
}
