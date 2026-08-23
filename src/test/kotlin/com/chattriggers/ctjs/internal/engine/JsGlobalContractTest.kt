package com.chattriggers.ctjs.internal.engine

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JsGlobalContractTest {
    private val classLoader = javaClass.classLoader
    private val script = resource("assets/ctjs/js/moduleProvidedLibs.js")
    private val manifest = Json.parseToJsonElement(resource("assets/ctjs/js-global-contract.json")).jsonObject

    @Test
    fun `manifest entries exist in module provided globals`() {
        assertEquals(1, manifest.getValue("schemaVersion").jsonPrimitive.content.toInt())
        val entries = manifest.getValue("globals").jsonArray + manifest.getValue("prototypeAliases").jsonArray
        val names = entries.map { it.jsonObject.getValue("name").jsonPrimitive.content }
        assertEquals(names.toSet().size, names.size)
        entries.forEach { entry ->
            val contract = entry.jsonObject
            val source = contract.getValue("source").jsonPrimitive.content
            assertTrue(source in script, "Missing JS global contract ${contract.getValue("name")}: $source")
        }
    }

    @Test
    fun `CommonJS fallback manifest matches executable behavior`() {
        val engine = manifest.getValue("engineContracts").jsonArray.single().jsonObject
        assertEquals("CommonJS default fallback", engine.getValue("name").jsonPrimitive.content)
        assertEquals(
            "com.chattriggers.ctjs.internal.engine.CommonJsExports.ensureDefault",
            engine.getValue("implementation").jsonPrimitive.content,
        )

        val context = Context.enter()
        try {
            val scope = context.initStandardObjects()
            val exports = context.newObject(scope)
            CommonJsExports.ensureDefault(exports)
            assertSame(exports, ScriptableObject.getProperty(exports, "default"))
        } finally {
            Context.exit()
        }
    }

    private fun resource(path: String): String =
        checkNotNull(classLoader.getResourceAsStream(path)) { "Missing resource $path" }
            .bufferedReader()
            .use { it.readText() }
}
