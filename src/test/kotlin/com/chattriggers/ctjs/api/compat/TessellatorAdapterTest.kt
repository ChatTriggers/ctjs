package com.chattriggers.ctjs.api.compat

import com.chattriggers.ctjs.api.vec.Vec2f
import com.chattriggers.ctjs.api.vec.Vec3f
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TessellatorAdapterTest {
    @Test
    fun `selects the four supported generic formats`() {
        assertEquals(
            CompatVertexFormat.POSITION,
            format().format,
        )
        assertEquals(
            CompatVertexFormat.POSITION_COLOR,
            format(CompatVertexAttribute.COLOR).format,
        )
        assertEquals(
            CompatVertexFormat.POSITION_TEXTURE,
            format(CompatVertexAttribute.UV0).format,
        )
        assertEquals(
            CompatVertexFormat.POSITION_TEXTURE_COLOR,
            format(CompatVertexAttribute.UV0, CompatVertexAttribute.COLOR).format,
        )
    }

    @Test
    fun `texture hint and texture state independently suppress uv`() {
        val attributes = setOf(CompatVertexAttribute.UV0)
        val hintOff = TessellatorAdapter.selectFormat(false, true, attributes)
        val stateOff = TessellatorAdapter.selectFormat(true, false, attributes)

        assertEquals(CompatVertexFormat.POSITION, hintOff.format)
        assertFalse(hintOff.useUv)
        assertEquals(CompatVertexFormat.POSITION, stateOff.format)
        assertFalse(stateOff.useUv)
    }

    @Test
    fun `normal combinations downgrade without changing color or uv selection`() {
        val normalOnly = format(CompatVertexAttribute.NORMAL)
        val all = format(
            CompatVertexAttribute.UV0,
            CompatVertexAttribute.COLOR,
            CompatVertexAttribute.NORMAL,
        )

        assertEquals(CompatVertexFormat.POSITION, normalOnly.format)
        assertTrue(normalOnly.normalDowngraded)
        assertEquals(CompatVertexFormat.POSITION_TEXTURE_COLOR, all.format)
        assertTrue(all.normalDowngraded)
    }

    @Test
    fun `mixed vertices receive uv and color defaults required by the selected format`() {
        val format = format(CompatVertexAttribute.UV0, CompatVertexAttribute.COLOR)
        val source = listOf(
            vertex(0, uv = Vec2f(0.25f, 0.5f), color = CompatColor(1, 2, 3, 4)),
            vertex(1),
        )

        val completed = TessellatorAdapter.completeVertices(source, format)
        assertEquals(Vec2f(0.25f, 0.5f), completed[0].uv)
        assertEquals(CompatColor(1, 2, 3, 4), completed[0].color)
        assertEquals(Vec2f(0f, 0f), completed[1].uv)
        assertEquals(CompatColor(255, 255, 255, 255), completed[1].color)
    }

    @Test
    fun `suppressed uv remains buffered when texture is disabled`() {
        val source = listOf(vertex(0, uv = Vec2f(0.25f, 0.5f)))
        val format = TessellatorAdapter.selectFormat(
            texturedHint = true,
            textureEnabled = false,
            activeAttributes = setOf(CompatVertexAttribute.UV0),
        )

        val completed = TessellatorAdapter.completeVertices(source, format)
        assertEquals(CompatVertexFormat.POSITION, format.format)
        assertEquals(Vec2f(0.25f, 0.5f), completed.single().uv)
    }

    @Test
    fun `session retains mixed attributes and immutable vertex snapshots`() {
        val session = CompatDrawSession(7, true)
        session.pos(Vec3f(1f, 2f, 3f))
        session.tex(Vec2f(0.25f, 0.5f))
        session.color(CompatColor(1, 2, 3, 4))
        session.normal(Vec3f(0f, 1f, 0f))
        session.pos(Vec3f(4f, 5f, 6f))

        val vertices = session.finishVertices()
        assertEquals(2, vertices.size)
        assertEquals(Vec2f(0.25f, 0.5f), vertices[0].uv)
        assertEquals(CompatColor(1, 2, 3, 4), vertices[0].color)
        assertEquals(Vec3f(0f, 1f, 0f), vertices[0].normal)
        assertNull(vertices[1].uv)
        assertNull(vertices[1].color)
        assertNull(vertices[1].normal)
        assertEquals(
            setOf(
                CompatVertexAttribute.UV0,
                CompatVertexAttribute.COLOR,
                CompatVertexAttribute.NORMAL,
            ),
            session.activeAttributes,
        )
    }

    @Test
    fun `attributes require a positioned current vertex`() {
        val session = CompatDrawSession(7, true)

        assertFailsWith<IllegalStateException> { session.tex(Vec2f()) }
        assertFailsWith<IllegalStateException> { session.color(CompatColor(1, 2, 3, 4)) }
        assertFailsWith<IllegalStateException> { session.normal(Vec3f()) }
    }

    @Test
    fun `line loop appends a deep copy of the first complete vertex`() {
        val first = vertex(0, uv = Vec2f(0.1f, 0.2f), color = CompatColor(1, 2, 3, 4))
        val conversion = TessellatorAdapter.convertDrawMode(2, listOf(first, vertex(1), vertex(2)))

        assertEquals(CompatDrawMode.LINE_STRIP, conversion.mode)
        assertEquals(listOf(0f, 1f, 2f, 0f), conversion.vertices.map { it.position.x })
        assertEquals(first, conversion.vertices.last())
        assertNotSame(first, conversion.vertices.last())
        assertNotSame(first.position, conversion.vertices.last().position)
        assertNotSame(first.uv, conversion.vertices.last().uv)
        assertNotSame(first.color, conversion.vertices.last().color)
    }

    @Test
    fun `polygon maps to triangle fan without reordering`() {
        val conversion = TessellatorAdapter.convertDrawMode(9, vertices(4))

        assertEquals(CompatDrawMode.TRIANGLE_FAN, conversion.mode)
        assertEquals(listOf(0f, 1f, 2f, 3f), conversion.vertices.map { it.position.x })
        assertNull(conversion.warningKey)
    }

    @Test
    fun `polygon with fewer than three vertices is rejected`() {
        val conversion = TessellatorAdapter.convertDrawMode(9, vertices(2))

        assertNull(conversion.mode)
        assertTrue(conversion.vertices.isEmpty())
        assertEquals("tessellator:polygon:too-few-vertices", conversion.warningKey)
    }

    @Test
    fun `quad strip converts four and six vertices with legacy winding`() {
        val four = TessellatorAdapter.convertDrawMode(8, vertices(4))
        val six = TessellatorAdapter.convertDrawMode(8, vertices(6))

        assertEquals(listOf(0f, 1f, 3f, 2f), four.vertices.map { it.position.x })
        assertEquals(listOf(0f, 1f, 3f, 2f, 2f, 3f, 5f, 4f), six.vertices.map { it.position.x })
        assertEquals(CompatDrawMode.QUADS, six.mode)
    }

    @Test
    fun `quad strip draws complete quads and warns about an unmatched vertex`() {
        val conversion = TessellatorAdapter.convertDrawMode(8, vertices(5))

        assertEquals(CompatDrawMode.QUADS, conversion.mode)
        assertEquals(listOf(0f, 1f, 3f, 2f), conversion.vertices.map { it.position.x })
        assertEquals("tessellator:quad-strip:incomplete", conversion.warningKey)
    }

    @Test
    fun `points and unknown modes never fall back to quads`() {
        for (mode in listOf(0, 42)) {
            val conversion = TessellatorAdapter.convertDrawMode(mode, vertices(4))
            assertNull(conversion.mode)
            assertTrue(conversion.vertices.isEmpty())
            assertEquals("tessellator:draw-mode:$mode", conversion.warningKey)
        }
    }

    @Test
    fun `session state rejects nested begin and preserves the active session`() {
        val state = TessellatorSessionState()
        val first = state.begin(3, true)

        assertFailsWith<IllegalStateException> { state.begin(7, false) }
        assertSame(first, state.current)
    }

    @Test
    fun `detach without begin is a no-op and sessions do not retain state`() {
        val state = TessellatorSessionState()
        assertNull(state.detach())

        val first = state.begin(3, true)
        first.pos(Vec3f(1f, 0f, 0f))
        assertSame(first, state.detach())
        assertNull(state.current)

        val second = state.begin(7, false)
        assertTrue(second.vertices.isEmpty())
        assertTrue(second.activeAttributes.isEmpty())
        assertFalse(second.texturedHint)
    }

    @Test
    fun `color conversions clamp and match Renderer3d packed semantics`() {
        assertEquals(CompatColor(0, 128, 255, 255), TessellatorAdapter.floatColor(-1f, 0.5f, 2f, 1f))
        assertEquals(CompatColor(0, 20, 255, 255), TessellatorAdapter.intColor(-10, 20, 300, 999))
        assertEquals(CompatColor(0x34, 0x56, 0x78, 255), TessellatorAdapter.packedColor(0x12345678L))
    }

    @Test
    fun `blend state and factors remain attached to the delayed session`() {
        val session = CompatDrawSession(7, true)
        session.blendEnabled = false
        session.blendFactors = BlendFactors(1, 2, 3, 4)

        assertFalse(session.blendEnabled)
        assertEquals(BlendFactors(1, 2, 3, 4), session.blendFactors)
    }

    private fun format(vararg attributes: CompatVertexAttribute) = TessellatorAdapter.selectFormat(
        texturedHint = true,
        textureEnabled = true,
        activeAttributes = attributes.toSet(),
    )

    private fun vertices(count: Int) = List(count) { vertex(it) }

    private fun vertex(
        index: Int,
        uv: Vec2f? = null,
        color: CompatColor? = null,
        normal: Vec3f? = null,
    ) = BufferedVertex(Vec3f(index.toFloat(), 0f, 0f), uv, color, normal)
}
