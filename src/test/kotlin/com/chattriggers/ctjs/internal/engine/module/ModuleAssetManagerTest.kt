package com.chattriggers.ctjs.internal.engine.module

import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModuleAssetManagerTest {
    private val root = createTempDirectory("ctjs-assets-").toFile()
    private val output = root.resolve("output")
    private val manifest = root.resolve("module-assets.json")

    @AfterTest
    fun cleanup() {
        root.deleteRecursively()
    }

    @Test
    fun `rename removes stale asset and copies replacement`() {
        val module = module("A", mapOf("a.png" to "a"))
        ModuleAssetManager.reconcile(listOf(module), output, manifest)
        assertEquals("a", output.resolve("a.png").readText())

        module.folder.resolve("assets/a.png").delete()
        module.folder.resolve("assets/b.png").writeText("b")
        ModuleAssetManager.reconcile(listOf(module), output, manifest)

        assertFalse(output.resolve("a.png").exists())
        assertEquals("b", output.resolve("b.png").readText())
    }

    @Test
    fun `removing colliding module restores remaining owner`() {
        val a = module("A", mapOf("shared.png" to "a"))
        val b = module("B", mapOf("shared.png" to "b"))
        val first = ModuleAssetManager.reconcile(listOf(b, a), output, manifest)
        assertEquals(listOf("B", "A"), first.collisions.getValue("shared.png"))
        assertEquals("a", output.resolve("shared.png").readText())

        ModuleAssetManager.reconcile(listOf(b), output, manifest)
        assertEquals("b", output.resolve("shared.png").readText())
    }

    @Test
    fun `modified former managed asset is not deleted`() {
        val module = module("A", mapOf("keep.txt" to "managed"))
        ModuleAssetManager.reconcile(listOf(module), output, manifest)
        output.resolve("keep.txt").writeText("user-modified")

        val result = ModuleAssetManager.reconcile(emptyList(), output, manifest)
        assertEquals(0, result.removed)
        assertTrue(output.resolve("keep.txt").exists())
    }

    private fun module(name: String, assets: Map<String, String>): Module {
        val folder = root.resolve(name)
        assets.forEach { (path, value) ->
            folder.resolve("assets/$path").apply {
                parentFile.mkdirs()
                writeText(value)
            }
        }
        return Module(name, ModuleMetadata(), folder)
    }
}
