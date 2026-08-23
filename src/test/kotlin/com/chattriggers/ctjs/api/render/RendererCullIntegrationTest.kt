package com.chattriggers.ctjs.api.render

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RendererCullIntegrationTest {
    @Test
    fun `public facade changes only CTJS pipeline intent`() {
        val renderer = classNode("com.chattriggers.ctjs.api.render.Renderer")
        listOf("enableCull", "disableCull").forEach { name ->
            val method = renderer.methods.single { it.name == name }
            assertEquals(1, method.invocations(SCOPED_STATE, "set"))
            assertEquals(0, method.invocations(RENDER_SYSTEM, "enableCull"))
            assertEquals(0, method.invocations(RENDER_SYSTEM, "disableCull"))
        }
    }

    @Test
    fun `Renderer3d pipeline key and pipeline both include culling`() {
        val renderer3d = classNode("com.chattriggers.ctjs.api.render.Renderer3d")
        val draw = renderer3d.methods.single { it.name == "draw" }
        val pipeline = renderer3d.methods.single { it.name == "getPipeline" }
        val key = classNode("com.chattriggers.ctjs.api.render.Renderer3d\$PipelineKey")

        assertEquals(1, draw.invocations(RENDERER, "isCullEnabled\$ctjs"))
        assertEquals(1, pipeline.invocations(PIPELINE_BUILDER, "setCulling"))
        assertTrue(key.fields.any { it.name == "culling" && it.desc == "Z" })
    }

    @Test
    fun `Renderer3d frame snapshots and restores cull intent on every exit`() {
        val renderer3d = classNode("com.chattriggers.ctjs.api.render.Renderer3d")
        val begin = renderer3d.methods.single {
            it.name == "begin" && it.desc.contains("Renderer\$VertexFormat")
        }
        val draw = renderer3d.methods.single { it.name == "draw" }
        val cleanupLambdas = renderer3d.methods.filter { it.name.startsWith("cleanupFrame\$lambda") }

        assertEquals(1, begin.invocations(RENDERER, "pushCullState\$ctjs"))
        assertEquals(1, cleanupLambdas.sumOf { it.invocations(RENDERER, "restoreCullState\$ctjs") })
        assertTrue(draw.tryCatchBlocks.any { it.type == null })
        assertTrue(draw.invocations(RENDERER_3D, "cleanupFrame") >= 2)
    }

    @Test
    fun `line helper restores outer cull state instead of forcing enabled`() {
        val renderer3d = classNode("com.chattriggers.ctjs.api.render.Renderer3d")
        val line = renderer3d.methods.single { it.name == "drawLine" }

        val lineCleanupLambdas = renderer3d.methods.filter { it.name.startsWith("drawLine\$lambda") }

        assertEquals(1, line.invocationsStartingWith(RENDERER, "pushCullState\$ctjs"))
        assertEquals(1, lineCleanupLambdas.sumOf { it.invocations(RENDERER, "restoreCullState\$ctjs") })
        assertEquals(0, line.invocations(RENDERER, "enableCull"))
        assertTrue(line.tryCatchBlocks.isNotEmpty())
    }

    @Test
    fun `Shape remains routed through Renderer begin and draw`() {
        val shape = classNode("com.chattriggers.ctjs.api.render.Shape")
        val draw = shape.methods.single { it.name == "draw" }

        assertEquals(1, draw.invocations(RENDERER, "begin"))
        assertEquals(1, draw.invocations(RENDERER, "draw"))
    }

    private fun classNode(binaryName: String): ClassNode {
        val resource = binaryName.replace('.', '/') + ".class"
        val stream = checkNotNull(javaClass.classLoader.getResourceAsStream(resource)) { "Missing $resource" }
        return stream.use { ClassNode().also { node -> ClassReader(it).accept(node, 0) } }
    }

    private fun MethodNode.invocations(owner: String, name: String): Int =
        instructions.iterator().asSequence().filterIsInstance<MethodInsnNode>().count {
            it.owner == owner && it.name == name
        }

    private fun MethodNode.invocationsStartingWith(owner: String, name: String): Int =
        instructions.iterator().asSequence().filterIsInstance<MethodInsnNode>().count {
            it.owner == owner && it.name.startsWith(name)
        }

    private companion object {
        const val RENDERER = "com/chattriggers/ctjs/api/render/Renderer"
        const val RENDERER_3D = "com/chattriggers/ctjs/api/render/Renderer3d"
        const val SCOPED_STATE = "com/chattriggers/ctjs/api/render/ScopedRenderState"
        const val RENDER_SYSTEM = "com/mojang/blaze3d/systems/RenderSystem"
        const val PIPELINE_BUILDER = "gg/essential/universal/render/URenderPipeline\$Builder"
    }
}
