package com.chattriggers.ctjs.internal.engine

import com.chattriggers.ctjs.internal.lifecycle.GenerationState
import com.chattriggers.ctjs.internal.lifecycle.OwnedHandle
import com.chattriggers.ctjs.internal.lifecycle.OwnedKind
import com.chattriggers.ctjs.internal.lifecycle.RuntimeOwner
import com.chattriggers.ctjs.internal.lifecycle.SystemRuntimeOwner
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Collections
import java.util.Enumeration
import java.util.concurrent.atomic.AtomicLong

/**
 * A closeable snapshot of the JARs visible to one module runtime generation.
 *
 * Source JARs are copied before they are added to the URLClassLoader. This keeps
 * module folders replaceable on Windows while the active generation is running;
 * only generation-private copies can be locked by the VM.
 */
internal class GenerationModuleClassLoader private constructor(
    parent: ClassLoader,
    private val cacheRoot: Path,
) : URLClassLoader(emptyArray(), parent), OwnedHandle {
    override val kind = OwnedKind.CLASS_LOADER

    private val lifecycleLock = Any()
    private val sourceUrls = linkedSetOf<String>()
    private var state = LoaderState.STAGED
    private var owner: RuntimeOwner? = null
    private var snapshotDirectory: Path? = null
    private var onClosed: (() -> Unit)? = null

    val isActive: Boolean
        get() = synchronized(lifecycleLock) {
            state == LoaderState.ACTIVE && owner?.state == GenerationState.ACTIVE
        }

    val isClosed: Boolean
        get() = synchronized(lifecycleLock) { state == LoaderState.CLOSED }

    val generationId: Long?
        get() = synchronized(lifecycleLock) { owner?.generationId }

    fun activate(runtimeOwner: RuntimeOwner, closeListener: () -> Unit = {}): Boolean {
        require(runtimeOwner !== SystemRuntimeOwner) { "Module JAR loaders cannot use the SYSTEM owner" }

        synchronized(lifecycleLock) {
            if (state != LoaderState.STAGED)
                return false
            state = LoaderState.ACTIVATING
            owner = runtimeOwner
            onClosed = closeListener
        }

        if (!runtimeOwner.register(this))
            return false

        val activated = synchronized(lifecycleLock) {
            if (state == LoaderState.ACTIVATING && runtimeOwner.state == GenerationState.ACTIVE) {
                state = LoaderState.ACTIVE
                true
            } else {
                false
            }
        }
        if (!activated) {
            runtimeOwner.unregister(this)
            close()
        }
        return activated
    }

    fun addJars(urls: Collection<URL>): Boolean {
        val runtimeOwner = synchronized(lifecycleLock) {
            when (state) {
                LoaderState.STAGED -> {
                    snapshotAndAdd(urls)
                    return true
                }
                LoaderState.ACTIVE -> owner
                LoaderState.ACTIVATING, LoaderState.CLOSED -> null
            }
        } ?: return false

        var added = false
        val executed = runtimeOwner.execute(this) {
            synchronized(lifecycleLock) {
                if (state == LoaderState.ACTIVE) {
                    snapshotAndAdd(urls)
                    added = true
                }
            }
        }
        return executed && added
    }

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        check(isActive) { "Module classloader generation is not active" }
        return super.loadClass(name, resolve)
    }

    override fun getResource(name: String): URL? {
        if (!isActive)
            return null
        return super.getResource(name)
    }

    override fun getResources(name: String): Enumeration<URL> {
        if (!isActive)
            return Collections.emptyEnumeration()
        return super.getResources(name)
    }

    override fun invalidate() = close()

    override fun close() {
        val cleanup = synchronized(lifecycleLock) {
            if (state == LoaderState.CLOSED)
                return
            state = LoaderState.CLOSED
            val registeredOwner = owner
            owner = null
            CloseCleanup(registeredOwner, snapshotDirectory, onClosed).also {
                snapshotDirectory = null
                onClosed = null
            }
        }

        cleanup.owner?.unregister(this)
        try {
            super.close()
        } finally {
            cleanup.snapshotDirectory?.toFile()?.deleteRecursively()
            cleanup.listener?.invoke()
        }
    }

    private fun snapshotAndAdd(urls: Collection<URL>) {
        urls.forEach { source ->
            val sourceKey = source.toExternalForm()
            if (!sourceUrls.add(sourceKey))
                return@forEach

            val directory = snapshotDirectory ?: createSnapshotDirectory().also { snapshotDirectory = it }
            val sourceName = runCatching { Path.of(source.toURI()).fileName.toString() }
                .getOrDefault("module.jar")
                .replace(UNSAFE_FILE_NAME, "_")
            val snapshot = directory.resolve("${nextSnapshotId.incrementAndGet()}-$sourceName")
            try {
                source.openStream().use { input ->
                    Files.copy(input, snapshot, StandardCopyOption.REPLACE_EXISTING)
                }
                snapshot.toFile().deleteOnExit()
                super.addURL(snapshot.toUri().toURL())
            } catch (e: Throwable) {
                sourceUrls.remove(sourceKey)
                Files.deleteIfExists(snapshot)
                throw e
            }
        }
    }

    private fun createSnapshotDirectory(): Path {
        Files.createDirectories(cacheRoot)
        return Files.createTempDirectory(cacheRoot, "generation-").also {
            // Normal generation cleanup removes this eagerly. The JVM hook is a
            // fallback for a client shutdown that does not run another reload.
            it.toFile().deleteOnExit()
        }
    }

    private data class CloseCleanup(
        val owner: RuntimeOwner?,
        val snapshotDirectory: Path?,
        val listener: (() -> Unit)?,
    )

    private enum class LoaderState {
        STAGED,
        ACTIVATING,
        ACTIVE,
        CLOSED,
    }

    companion object {
        private val UNSAFE_FILE_NAME = Regex("[^A-Za-z0-9._-]")
        private val nextSnapshotId = AtomicLong()
        private val defaultCacheRoot = Path.of("./config/ChatTriggers/classloader-cache")

        fun stage(
            urls: Collection<URL>,
            parent: ClassLoader = GenerationModuleClassLoader::class.java.classLoader,
            cacheRoot: Path = defaultCacheRoot,
        ): GenerationModuleClassLoader {
            val loader = GenerationModuleClassLoader(parent, cacheRoot)
            try {
                check(loader.addJars(urls)) { "Failed to stage module JARs" }
                return loader
            } catch (e: Throwable) {
                loader.close()
                throw e
            }
        }
    }
}
