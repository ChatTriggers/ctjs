package com.chattriggers.ctjs.internal.engine.module

import com.chattriggers.ctjs.internal.engine.CommonJsExports
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ModuleContractTest {
    @Test
    fun `dependencies enter once in topological order`() {
        val root = createTempDirectory("ctjs-modules-").toFile()
        try {
            val core = module(root, "Core")
            val library = module(root, "Library", "Core")
            val app = module(root, "App", "Library", "Core")

            assertEquals(
                listOf("Core", "Library", "App"),
                ModuleDependencyResolver.sort(listOf(app, library, core)).map(Module::name),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `dependency cycles and missing dependencies fail closed with clear error`() {
        val root = createTempDirectory("ctjs-module-cycle-").toFile()
        try {
            val a = module(root, "A", "B")
            val b = module(root, "B", "A")
            val cycle = assertFailsWith<IllegalStateException> {
                ModuleDependencyResolver.sort(listOf(a, b))
            }
            assertTrue(cycle.message.orEmpty().contains("A -> B -> A"))

            val missing = assertFailsWith<IllegalStateException> {
                ModuleDependencyResolver.sort(listOf(module(root, "C", "Missing")))
            }
            assertTrue(missing.message.orEmpty().contains("requires missing module Missing"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `CommonJS default fallback preserves named exports and explicit default`() {
        val context = Context.enter()
        try {
            val scope = context.initStandardObjects()
            val exports = context.newObject(scope)
            ScriptableObject.putProperty(exports, "named", 42)
            CommonJsExports.ensureDefault(exports)
            assertSame(exports, ScriptableObject.getProperty(exports, "default"))
            assertEquals(42, ScriptableObject.getProperty(exports, "named"))

            val explicit = context.newObject(scope)
            ScriptableObject.putProperty(explicit, "default", "explicit")
            CommonJsExports.ensureDefault(explicit)
            assertEquals("explicit", ScriptableObject.getProperty(explicit, "default"))
        } finally {
            Context.exit()
        }
    }

    @Test
    fun `delete and same-name reimport use new module folder contents`() {
        val root = createTempDirectory("ctjs-module-reimport-").toFile()
        try {
            val firstFolder = root.resolve("A").apply { mkdirs(); resolve("index.js").writeText("v1") }
            val first = Module("A", ModuleMetadata(entry = "index.js"), firstFolder)
            assertEquals("v1", first.folder.resolve(first.metadata.entry!!).readText())
            assertTrue(firstFolder.deleteRecursively())

            val nextFolder = root.resolve("A").apply { mkdirs(); resolve("index.js").writeText("v2") }
            val next = Module("A", ModuleMetadata(entry = "index.js"), nextFolder)
            assertEquals("v2", next.folder.resolve(next.metadata.entry!!).readText())
        } finally {
            root.deleteRecursively()
        }
    }

    private fun module(root: java.io.File, name: String, vararg dependencies: String): Module = Module(
        name,
        ModuleMetadata(name = name, entry = "index.js", requires = ArrayList(dependencies.toList())),
        root.resolve(name).apply { mkdirs() },
    )
}
