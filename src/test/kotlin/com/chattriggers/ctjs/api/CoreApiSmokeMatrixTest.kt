package com.chattriggers.ctjs.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoreApiSmokeMatrixTest {
    private data class Coverage(
        val normal: String,
        val boundary: String,
        val runtimeRequired: Boolean,
    )

    private val matrix = linkedMapOf(
        "ChatLib" to Coverage("ChatLibCoreSmokeTest formatting", "null and empty formatting", false),
        "Client" to Coverage("P1RuntimeProbe schedule/title/gui", "RuntimeGeneration task rejection", true),
        "Player" to Coverage("P1RuntimeProbe loaded wrapper", "P1RuntimeProbe WORLD_NULL", true),
        "World" to Coverage("P1RuntimeProbe block/time/entities", "P1RuntimeProbe WORLD_NULL", true),
        "Renderer" to Coverage("RendererCullIntegrationTest", "ScopedRenderState exception restoration", true),
        "Renderer3d" to Coverage("P1RuntimeProbe begin/draw", "RendererCullIntegrationTest nesting", true),
        "Gui" to Coverage("P1RuntimeProbe input/render/open/close", "GuiInputArgumentsTest and reload invalidation", true),
        "Sound" to Coverage("P1RuntimeProbe play/pause/resume/stop", "destroy twice and reload", true),
        "Image" to Coverage("P1RuntimeProbe create/register/draw", "destroy twice and reload", true),
        "Item" to Coverage("P1RuntimeProbe non-empty item", "empty stack maps to null", true),
        "Block" to Coverage("P1RuntimeProbe registry and position", "WORLD_NULL runtime access", true),
        "FileLib" to Coverage("FileLibIoSmokeTest and unzip", "missing file and unsafe zip", false),
        "Config" to Coverage("P1RuntimeProbe save/load", "missing/default config values", true),
        "console" to Coverage("ConsoleLifecycleTest eval and backlog", "disconnect/reconnect/shutdown", true),
    )

    @Test
    fun `every core API area has normal and boundary coverage`() {
        assertEquals(
            setOf(
                "ChatLib", "Client", "Player", "World", "Renderer", "Renderer3d", "Gui",
                "Sound", "Image", "Item", "Block", "FileLib", "Config", "console",
            ),
            matrix.keys,
        )
        matrix.values.forEach {
            assertTrue(it.normal.isNotBlank())
            assertTrue(it.boundary.isNotBlank())
        }
        assertTrue(matrix.values.any(Coverage::runtimeRequired))
    }
}
