package com.chattriggers.ctjs.internal.engine

import com.chattriggers.ctjs.internal.lifecycle.GenerationState
import com.chattriggers.ctjs.internal.lifecycle.OwnedKind
import com.chattriggers.ctjs.internal.lifecycle.RuntimeGenerationController
import com.chattriggers.ctjs.internal.lifecycle.SystemRuntimeOwner
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import javax.tools.ToolProvider
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GenerationModuleClassLoaderTest {
    private val root = createTempDirectory("ctjs-generation-loader-test-")
    private val cache = root.resolve("cache")

    @AfterTest
    fun cleanup() {
        root.toFile().deleteRecursively()
    }

    @Test
    fun `same path and fqcn load v2 after generation reload`() {
        val jar = root.resolve("module.jar")
        writeProbeJar(jar, "probe.CompatProbe", "v1")
        val controller = RuntimeGenerationController()
        val first = activeLoader(controller, jar)
        val v1Class = first.loadClass("probe.CompatProbe")
        assertEquals("v1", invokeVersion(v1Class))

        val snapshot = controller.stopCurrent()
        snapshot.invalidate(OwnedKind.CLASS_LOADER)
        writeProbeJar(jar, "probe.CompatProbe", "v2")
        val next = assertNotNull(controller.publishNext(snapshot))
        val second = GenerationModuleClassLoader.stage(listOf(jar.toUri().toURL()), cacheRoot = cache)
        assertTrue(second.activate(next))
        val v2Class = second.loadClass("probe.CompatProbe")

        assertEquals("v2", invokeVersion(v2Class))
        assertNotSame(first, second)
        assertNotSame(v1Class, v2Class)
        controller.stopCurrent().invalidate(OwnedKind.CLASS_LOADER)
    }

    @Test
    fun `stopped generation releases jar and rejects further class loads`() {
        val jar = root.resolve("delete-me.jar")
        writeProbeJar(jar, "probe.DeleteProbe", "loaded")
        val controller = RuntimeGenerationController()
        val loader = activeLoader(controller, jar)
        assertEquals("loaded", invokeVersion(loader.loadClass("probe.DeleteProbe")))

        val snapshot = controller.stopCurrent()
        assertEquals(GenerationState.STOPPING, snapshot.owner.state)
        assertFailsWith<IllegalStateException> { loader.loadClass("probe.DeleteProbe") }
        snapshot.invalidate(OwnedKind.CLASS_LOADER)
        assertTrue(loader.isClosed)
        Files.delete(jar)
        assertFalse(Files.exists(jar))

        snapshot.markDead()
        assertEquals(GenerationState.DEAD, snapshot.owner.state)
        assertFailsWith<IllegalStateException> { loader.loadClass("probe.DeleteProbe") }
    }

    @Test
    fun `deleting module A source does not prevent adding and loading module B`() {
        val jarA = root.resolve("module-a.jar")
        val jarB = root.resolve("module-b.jar")
        writeProbeJar(jarA, "probe.ModuleA", "a")
        writeProbeJar(jarB, "probe.ModuleB", "b")
        val controller = RuntimeGenerationController()
        val loader = activeLoader(controller, jarA)
        assertEquals("a", invokeVersion(loader.loadClass("probe.ModuleA")))

        Files.delete(jarA)
        assertTrue(loader.addJars(listOf(jarB.toUri().toURL())))
        assertEquals("b", invokeVersion(loader.loadClass("probe.ModuleB")))

        controller.stopCurrent().invalidate(OwnedKind.CLASS_LOADER)
    }

    @Test
    fun `deleted module can be recreated and imported by a new generation`() {
        val jar = root.resolve("reimport.jar")
        writeProbeJar(jar, "probe.ReimportProbe", "old")
        val controller = RuntimeGenerationController()
        val first = activeLoader(controller, jar)
        assertEquals("old", invokeVersion(first.loadClass("probe.ReimportProbe")))

        Files.delete(jar)
        val snapshot = controller.stopCurrent()
        snapshot.invalidate(OwnedKind.CLASS_LOADER)
        writeProbeJar(jar, "probe.ReimportProbe", "new")

        val next = assertNotNull(controller.publishNext(snapshot))
        val second = GenerationModuleClassLoader.stage(listOf(jar.toUri().toURL()), cacheRoot = cache)
        assertTrue(second.activate(next))
        assertEquals("new", invokeVersion(second.loadClass("probe.ReimportProbe")))
        controller.stopCurrent().invalidate(OwnedKind.CLASS_LOADER)
    }

    @Test
    fun `failed setup closes staged loader and leaves source deletable`() {
        val jar = root.resolve("failed-setup.jar")
        writeProbeJar(jar, "probe.FailedProbe", "unused")
        val staged = GenerationModuleClassLoader.stage(listOf(jar.toUri().toURL()), cacheRoot = cache)

        staged.close()
        staged.close()
        Files.delete(jar)

        assertTrue(staged.isClosed)
        assertFalse(Files.exists(jar))
    }

    @Test
    fun `stale setup transition cannot activate over the latest generation`() {
        val staleJar = root.resolve("stale.jar")
        val latestJar = root.resolve("latest.jar")
        writeProbeJar(staleJar, "probe.StaleProbe", "stale")
        writeProbeJar(latestJar, "probe.LatestProbe", "latest")
        val staleLoader = GenerationModuleClassLoader.stage(listOf(staleJar.toUri().toURL()), cacheRoot = cache)
        val latestLoader = GenerationModuleClassLoader.stage(listOf(latestJar.toUri().toURL()), cacheRoot = cache)
        val controller = RuntimeGenerationController()
        val staleTransition = controller.stopCurrent()
        val latestTransition = controller.stopCurrent()

        assertNull(controller.publishNext(staleTransition))
        staleLoader.close()
        val owner = assertNotNull(controller.publishNext(latestTransition))
        assertTrue(latestLoader.activate(owner))

        assertTrue(staleLoader.isClosed)
        assertFalse(staleLoader.isActive)
        assertEquals("latest", invokeVersion(latestLoader.loadClass("probe.LatestProbe")))
        controller.stopCurrent().invalidate(OwnedKind.CLASS_LOADER)
    }

    @Test
    fun `module loader cannot be assigned to system owner`() {
        val jar = root.resolve("system.jar")
        writeProbeJar(jar, "probe.SystemProbe", "system")
        val loader = GenerationModuleClassLoader.stage(listOf(jar.toUri().toURL()), cacheRoot = cache)

        assertFailsWith<IllegalArgumentException> { loader.activate(SystemRuntimeOwner) }
        loader.close()
    }

    @Test
    fun `ten reloads keep one private cache and only the latest jar class`() {
        val jar = root.resolve("stress.jar")
        val controller = RuntimeGenerationController()
        val generationIds = mutableListOf<Long>()
        var previousClass: Class<*>? = null

        repeat(11) { generationIndex ->
            val version = when (generationIndex) {
                in 0..2 -> "v1"
                in 3..5 -> "v2"
                in 6..8 -> "v3"
                else -> "v4"
            }
            writeProbeJar(jar, "probe.StressProbe", version)

            val owner = controller.currentOwner()
            generationIds += owner.generationId
            val loader = activeLoader(controller, jar)
            val loadedClass = loader.loadClass("probe.StressProbe")
            assertEquals(version, invokeVersion(loadedClass))
            previousClass?.let { assertNotSame(it, loadedClass) }
            previousClass = loadedClass
            assertEquals(1L, cacheDirectoryCount())

            val snapshot = controller.stopCurrent()
            snapshot.invalidate(OwnedKind.CLASS_LOADER)
            assertTrue(loader.isClosed)
            assertEquals(0L, cacheDirectoryCount())

            val renamed = root.resolve("stress-renamed.jar")
            Files.move(jar, renamed)
            Files.move(renamed, jar)

            if (generationIndex < 10)
                assertNotNull(controller.publishNext(snapshot))
            snapshot.markDead()
        }

        assertEquals((1L..11L).toList(), generationIds)
        Files.delete(jar)
        assertFalse(Files.exists(jar))
    }

    private fun activeLoader(controller: RuntimeGenerationController, jar: Path): GenerationModuleClassLoader {
        val loader = GenerationModuleClassLoader.stage(listOf(jar.toUri().toURL()), cacheRoot = cache)
        assertTrue(loader.activate(controller.currentOwner()))
        return loader
    }

    private fun invokeVersion(clazz: Class<*>): String =
        clazz.getMethod("version").invoke(null) as String

    private fun cacheDirectoryCount(): Long {
        if (!Files.isDirectory(cache))
            return 0
        return Files.list(cache).use { entries -> entries.filter(Files::isDirectory).count() }
    }

    private fun writeProbeJar(jar: Path, fqcn: String, value: String) {
        Files.deleteIfExists(jar)
        jar.parent.createDirectories()
        val work = Files.createTempDirectory(root, "compile-")
        try {
            val packageName = fqcn.substringBeforeLast('.')
            val simpleName = fqcn.substringAfterLast('.')
            val source = work.resolve("src/${fqcn.replace('.', '/')}.java")
            source.parent.createDirectories()
            Files.writeString(
                source,
                "package $packageName; public final class $simpleName { " +
                    "public static String version() { return \"$value\"; } }",
            )
            val classes = work.resolve("classes").createDirectories()
            val compiler = assertNotNull(ToolProvider.getSystemJavaCompiler())
            assertEquals(0, compiler.run(null, null, null, "-d", classes.toString(), source.toString()))

            JarOutputStream(Files.newOutputStream(jar)).use { output ->
                Files.walk(classes).use { entries ->
                    entries.filter(Files::isRegularFile).forEach { file ->
                        val name = classes.relativize(file).toString().replace('\\', '/')
                        output.putNextEntry(JarEntry(name))
                        Files.copy(file, output)
                        output.closeEntry()
                    }
                }
            }
        } finally {
            work.toFile().deleteRecursively()
        }
    }
}
