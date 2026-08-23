package com.chattriggers.ctjs.api.compat

import com.chattriggers.ctjs.api.render.Image
import com.chattriggers.ctjs.api.render.Renderer
import com.chattriggers.ctjs.api.render.Renderer3d
import com.chattriggers.ctjs.api.vec.Vec2f
import com.chattriggers.ctjs.api.vec.Vec3f
import java.awt.Color
import kotlin.math.roundToInt

internal enum class CompatVertexAttribute {
    UV0,
    COLOR,
    NORMAL,
}

internal enum class CompatVertexFormat {
    POSITION,
    POSITION_COLOR,
    POSITION_TEXTURE,
    POSITION_TEXTURE_COLOR,
}

internal enum class CompatDrawMode {
    LINES,
    LINE_STRIP,
    TRIANGLES,
    TRIANGLE_STRIP,
    TRIANGLE_FAN,
    QUADS,
}

internal data class CompatColor(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int,
)

internal data class BufferedVertex(
    val position: Vec3f,
    val uv: Vec2f?,
    val color: CompatColor?,
    val normal: Vec3f?,
) {
    fun deepCopy() = BufferedVertex(
        position.copy(),
        uv?.copy(),
        color?.copy(),
        normal?.copy(),
    )
}

internal data class BlendFactors(
    val sourceFactor: Int = 770,
    val destFactor: Int = 771,
    val sourceFactorAlpha: Int = 1,
    val destFactorAlpha: Int = 0,
)

internal class CompatDrawSession(
    val legacyMode: Int,
    val texturedHint: Boolean,
) {
    val vertices = mutableListOf<BufferedVertex>()
    var currentPosition: Vec3f? = null
        private set
    var currentUv: Vec2f? = null
        private set
    var currentColor: CompatColor? = null
        private set
    var currentNormal: Vec3f? = null
        private set
    val activeAttributes = linkedSetOf<CompatVertexAttribute>()
    var blendEnabled = true
    var blendFactors = BlendFactors()

    fun pos(position: Vec3f) {
        commitCurrentVertex()
        currentPosition = position
        currentUv = null
        currentColor = null
        currentNormal = null
    }

    fun tex(uv: Vec2f) {
        requireCurrentVertex("tex")
        currentUv = uv
        activeAttributes += CompatVertexAttribute.UV0
    }

    fun color(color: CompatColor) {
        requireCurrentVertex("color")
        currentColor = color
        activeAttributes += CompatVertexAttribute.COLOR
    }

    fun normal(normal: Vec3f) {
        requireCurrentVertex("normal")
        currentNormal = normal
        activeAttributes += CompatVertexAttribute.NORMAL
    }

    fun finishVertices(): List<BufferedVertex> {
        commitCurrentVertex()
        return vertices.map(BufferedVertex::deepCopy)
    }

    private fun commitCurrentVertex() {
        val position = currentPosition ?: return
        vertices += BufferedVertex(
            position.copy(),
            currentUv?.copy(),
            currentColor?.copy(),
            currentNormal?.copy(),
        )
        currentPosition = null
        currentUv = null
        currentColor = null
        currentNormal = null
    }

    private fun requireCurrentVertex(operation: String) {
        check(currentPosition != null) {
            "Tessellator.$operation requires pos() to define the current vertex"
        }
    }
}

internal class TessellatorSessionState {
    var current: CompatDrawSession? = null
        private set

    fun begin(legacyMode: Int, texturedHint: Boolean): CompatDrawSession {
        check(current == null) { "Tessellator is already drawing" }
        return CompatDrawSession(legacyMode, texturedHint).also { current = it }
    }

    fun detach(): CompatDrawSession? = current.also { current = null }
}

internal data class CompatFormatPlan(
    val format: CompatVertexFormat,
    val useUv: Boolean,
    val useColor: Boolean,
    val normalDowngraded: Boolean,
)

internal data class CompatDrawConversion(
    val mode: CompatDrawMode?,
    val vertices: List<BufferedVertex>,
    val warningKey: String? = null,
    val warningMessage: String? = null,
)

internal object TessellatorAdapter {
    fun selectFormat(
        texturedHint: Boolean,
        textureEnabled: Boolean,
        activeAttributes: Set<CompatVertexAttribute>,
    ): CompatFormatPlan {
        val useUv = texturedHint && textureEnabled && CompatVertexAttribute.UV0 in activeAttributes
        val useColor = CompatVertexAttribute.COLOR in activeAttributes
        return CompatFormatPlan(
            format = when {
                useUv && useColor -> CompatVertexFormat.POSITION_TEXTURE_COLOR
                useUv -> CompatVertexFormat.POSITION_TEXTURE
                useColor -> CompatVertexFormat.POSITION_COLOR
                else -> CompatVertexFormat.POSITION
            },
            useUv = useUv,
            useColor = useColor,
            normalDowngraded = CompatVertexAttribute.NORMAL in activeAttributes,
        )
    }

    fun convertDrawMode(legacyMode: Int, sourceVertices: List<BufferedVertex>): CompatDrawConversion {
        val vertices = sourceVertices.map(BufferedVertex::deepCopy)
        return when (legacyMode) {
            0 -> unsupportedMode(legacyMode, "POINTS is not supported")
            1 -> CompatDrawConversion(CompatDrawMode.LINES, vertices)
            2 -> CompatDrawConversion(
                CompatDrawMode.LINE_STRIP,
                if (vertices.size >= 2) vertices + vertices.first().deepCopy() else vertices,
            )
            3 -> CompatDrawConversion(CompatDrawMode.LINE_STRIP, vertices)
            4 -> CompatDrawConversion(CompatDrawMode.TRIANGLES, vertices)
            5 -> CompatDrawConversion(CompatDrawMode.TRIANGLE_STRIP, vertices)
            6 -> CompatDrawConversion(CompatDrawMode.TRIANGLE_FAN, vertices)
            7 -> CompatDrawConversion(CompatDrawMode.QUADS, vertices)
            8 -> convertQuadStrip(vertices)
            9 -> if (vertices.size >= 3) {
                CompatDrawConversion(CompatDrawMode.TRIANGLE_FAN, vertices)
            } else {
                CompatDrawConversion(
                    mode = null,
                    vertices = emptyList(),
                    warningKey = "tessellator:polygon:too-few-vertices",
                    warningMessage = "[CTJS Compatibility] Tessellator POLYGON requires at least 3 vertices; skipping draw.",
                )
            }
            else -> unsupportedMode(legacyMode, "unknown draw mode")
        }
    }

    fun floatColor(red: Float, green: Float, blue: Float, alpha: Float): CompatColor = CompatColor(
        floatChannel(red),
        floatChannel(green),
        floatChannel(blue),
        floatChannel(alpha),
    )

    fun intColor(red: Int, green: Int, blue: Int, alpha: Int): CompatColor = CompatColor(
        red.coerceIn(0, 255),
        green.coerceIn(0, 255),
        blue.coerceIn(0, 255),
        alpha.coerceIn(0, 255),
    )

    fun packedColor(color: Long): CompatColor {
        val unpacked = Color(color.toInt())
        return CompatColor(unpacked.red, unpacked.green, unpacked.blue, unpacked.alpha)
    }

    fun completeVertices(vertices: List<BufferedVertex>, format: CompatFormatPlan): List<BufferedVertex> {
        return vertices.map { vertex ->
            vertex.copy(
                position = vertex.position.copy(),
                uv = if (format.useUv) (vertex.uv ?: DEFAULT_UV).copy() else vertex.uv?.copy(),
                color = if (format.useColor) (vertex.color ?: DEFAULT_COLOR).copy() else vertex.color?.copy(),
                normal = vertex.normal?.copy(),
            )
        }
    }

    private fun floatChannel(value: Float) = (value.coerceIn(0f, 1f) * 255f).roundToInt()

    private fun convertQuadStrip(vertices: List<BufferedVertex>): CompatDrawConversion {
        val converted = mutableListOf<BufferedVertex>()
        var index = 0
        while (index + 3 < vertices.size) {
            converted += vertices[index].deepCopy()
            converted += vertices[index + 1].deepCopy()
            converted += vertices[index + 3].deepCopy()
            converted += vertices[index + 2].deepCopy()
            index += 2
        }

        val hasIncompletePair = vertices.size < 4 || vertices.size % 2 != 0
        return CompatDrawConversion(
            mode = if (converted.isEmpty()) null else CompatDrawMode.QUADS,
            vertices = converted,
            warningKey = if (hasIncompletePair) "tessellator:quad-strip:incomplete" else null,
            warningMessage = if (hasIncompletePair) {
                "[CTJS Compatibility] Tessellator QUAD_STRIP has an incomplete vertex pair; drawing complete quads only."
            } else null,
        )
    }

    private fun unsupportedMode(legacyMode: Int, reason: String) = CompatDrawConversion(
        mode = null,
        vertices = emptyList(),
        warningKey = "tessellator:draw-mode:$legacyMode",
        warningMessage = "[CTJS Compatibility] Unsupported legacy Tessellator draw mode $legacyMode ($reason); skipping draw.",
    )

    private val DEFAULT_UV = Vec2f(0f, 0f)
    private val DEFAULT_COLOR = CompatColor(255, 255, 255, 255)
}

/**
 * CTJS 2.x Tessellator facade backed by the modern Renderer3d/Renderer APIs.
 */
@Deprecated("Use Renderer3d and Renderer")
object Tessellator {
    private val sessions = TessellatorSessionState()
    private var textureEnabled = true

    @JvmStatic
    var partialTicks: Float
        get() = Renderer.partialTicks
        set(value) {
            Renderer.partialTicks = value
        }

    @JvmStatic
    fun disableAlpha() = apply { }

    @JvmStatic
    fun enableAlpha() = apply { }

    @JvmStatic
    fun alphaFunc(func: Int, ref: Float) = apply { }

    @JvmStatic
    fun enableLighting() = apply { Renderer.enableLighting() }

    @JvmStatic
    fun disableLighting() = apply { Renderer.disableLighting() }

    @JvmStatic
    fun disableDepth() = apply { Renderer.disableDepth() }

    @JvmStatic
    fun enableDepth() = apply { Renderer.enableDepth() }

    @JvmStatic
    fun depthFunc(depthFunc: Int) = apply { Renderer.depthFunc(depthFunc) }

    @JvmStatic
    fun depthMask(flagIn: Boolean) = apply { Renderer.depthMask(flagIn) }

    @JvmStatic
    fun disableBlend() = apply {
        Renderer.disableBlend()
        sessions.current?.blendEnabled = false
    }

    @JvmStatic
    fun enableBlend() = apply {
        Renderer.enableBlend()
        sessions.current?.blendEnabled = true
    }

    @JvmStatic
    fun blendFunc(sourceFactor: Int, destFactor: Int) = apply {
        val factors = BlendFactors(sourceFactor, destFactor, 1, 0)
        Renderer.tryBlendFuncSeparate(
            factors.sourceFactor,
            factors.destFactor,
            factors.sourceFactorAlpha,
            factors.destFactorAlpha,
        )
        sessions.current?.blendFactors = factors
    }

    @JvmStatic
    fun tryBlendFuncSeparate(
        sourceFactor: Int,
        destFactor: Int,
        sourceFactorAlpha: Int,
        destFactorAlpha: Int,
    ) = apply {
        val factors = BlendFactors(sourceFactor, destFactor, sourceFactorAlpha, destFactorAlpha)
        Renderer.tryBlendFuncSeparate(
            factors.sourceFactor,
            factors.destFactor,
            factors.sourceFactorAlpha,
            factors.destFactorAlpha,
        )
        sessions.current?.blendFactors = factors
    }

    @JvmStatic
    fun enableTexture2D() = apply {
        textureEnabled = true
    }

    @JvmStatic
    fun disableTexture2D() = apply {
        textureEnabled = false
    }

    @JvmStatic
    fun bindTexture(texture: Image) = apply { Renderer.bindTexture(texture) }

    @JvmStatic
    fun deleteTexture(texture: Image) = apply { Renderer.deleteTexture(texture) }

    @JvmStatic
    fun pushMatrix() = apply { Renderer.pushMatrix() }

    @JvmStatic
    fun popMatrix() = apply { Renderer.popMatrix() }

    @JvmStatic
    @JvmOverloads
    fun begin(drawMode: Int = 7, textured: Boolean = true) = apply {
        sessions.begin(drawMode, textured)
        var matrixPushed = false
        try {
            Renderer.pushMatrix()
            matrixPushed = true
            Renderer.enableBlend().disableCull()
            Renderer.tryBlendFuncSeparate(770, 771, 1, 0)
        } catch (failure: Throwable) {
            sessions.detach()
            if (matrixPushed) {
                try {
                    closeRendererFrame()
                } catch (cleanupFailure: Throwable) {
                    failure.addSuppressed(cleanupFailure)
                }
            }
            throw failure
        }
    }

    @JvmStatic
    @JvmOverloads
    fun colorize(red: Float, green: Float, blue: Float, alpha: Float = 1f) = apply {
        Renderer.colorize(red, green, blue, alpha)
    }

    @JvmStatic
    fun rotate(angle: Float, x: Float, y: Float, z: Float) = apply {
        Renderer.rotate(angle, x, y, z)
    }

    @JvmStatic
    fun translate(x: Float, y: Float, z: Float) = apply {
        Renderer.translate(x, y, z)
    }

    @JvmStatic
    @JvmOverloads
    fun scale(x: Float, y: Float = x, z: Float = x) = apply {
        Renderer.scale(x, y, z)
    }

    @JvmStatic
    fun pos(x: Float, y: Float, z: Float) = apply {
        if (sessions.current == null)
            begin()
        sessions.current!!.pos(Vec3f(x, y, z))
    }

    @JvmStatic
    fun tex(u: Float, v: Float) = apply {
        requireSession().tex(Vec2f(u, v))
    }

    @JvmStatic
    @JvmOverloads
    fun color(red: Float, green: Float, blue: Float, alpha: Float = 1f) = apply {
        requireSession().color(TessellatorAdapter.floatColor(red, green, blue, alpha))
    }

    @JvmStatic
    @JvmOverloads
    fun color(red: Int, green: Int, blue: Int, alpha: Int = 255) = apply {
        requireSession().color(TessellatorAdapter.intColor(red, green, blue, alpha))
    }

    @JvmStatic
    fun color(color: Long) = apply {
        requireSession().color(TessellatorAdapter.packedColor(color))
    }

    @JvmStatic
    fun normal(x: Float, y: Float, z: Float) = apply {
        requireSession().normal(Vec3f(x, y, z))
    }

    @JvmStatic
    fun draw() {
        val drawing = sessions.detach() ?: return
        var innerFrameStarted = false
        var innerFrameClosed = false
        var failure: Throwable? = null

        try {
            val sourceVertices = drawing.finishVertices()
            val converted = TessellatorAdapter.convertDrawMode(drawing.legacyMode, sourceVertices)
            emitWarning(converted.warningKey, converted.warningMessage)

            val targetMode = converted.mode
            if (targetMode != null && converted.vertices.isNotEmpty()) {
                val format = TessellatorAdapter.selectFormat(
                    drawing.texturedHint,
                    textureEnabled,
                    drawing.activeAttributes,
                )
                if (format.normalDowngraded) {
                    LegacyCompatibility.warnOnce(
                        "tessellator:normal-downgraded",
                        "[CTJS Compatibility] Tessellator normal data is accepted but ignored by the generic compatibility pipeline.",
                    )
                }

                val completedVertices = TessellatorAdapter.completeVertices(converted.vertices, format)

                Renderer3d.begin(targetMode.toRenderer(), format.format.toRenderer())
                innerFrameStarted = true
                Renderer.tryBlendFuncSeparate(
                    drawing.blendFactors.sourceFactor,
                    drawing.blendFactors.destFactor,
                    drawing.blendFactors.sourceFactorAlpha,
                    drawing.blendFactors.destFactorAlpha,
                )
                if (drawing.blendEnabled) Renderer.enableBlend() else Renderer.disableBlend()

                completedVertices.forEach { vertex ->
                    Renderer3d.pos(vertex.position.x, vertex.position.y, vertex.position.z)
                    if (format.useUv) {
                        val uv = checkNotNull(vertex.uv)
                        Renderer3d.tex(uv.x, uv.y)
                    }
                    if (format.useColor) {
                        val color = checkNotNull(vertex.color)
                        Renderer3d.color(color.red, color.green, color.blue, color.alpha)
                    }
                }

                Renderer3d.draw()
                innerFrameClosed = true
            }
        } catch (caught: Throwable) {
            failure = caught
        } finally {
            if (innerFrameStarted && !innerFrameClosed)
                failure = closeRendererFrame(failure)
            failure = closeRendererFrame(failure)
        }

        failure?.let { throw it }
    }

    @JvmStatic
    fun getRenderPos(x: Float, y: Float, z: Float): Vec3f = Renderer.getRenderPos(x, y, z)

    @JvmStatic
    @JvmOverloads
    fun drawString(
        text: String,
        x: Float,
        y: Float,
        z: Float,
        color: Int = -1,
        renderBlackBox: Boolean = true,
        scale: Float = 1f,
        increase: Boolean = true,
    ) {
        Renderer3d.drawString(
            text,
            x,
            y,
            z,
            color.toLong() and 0xffffffffL,
            renderBlackBox,
            scale,
            increase,
        )
    }

    private fun requireSession(): CompatDrawSession = checkNotNull(sessions.current) {
        "Tessellator attribute requires begin() and pos()"
    }

    private fun emitWarning(key: String?, message: String?) {
        if (key != null && message != null)
            LegacyCompatibility.warnOnce(key, message)
    }

    private fun closeRendererFrame(existingFailure: Throwable?): Throwable? {
        return try {
            closeRendererFrame()
            existingFailure
        } catch (cleanupFailure: Throwable) {
            if (existingFailure != null) {
                existingFailure.addSuppressed(cleanupFailure)
                existingFailure
            } else {
                cleanupFailure
            }
        }
    }

    private fun closeRendererFrame() {
        try {
            Renderer.colorize(1f, 1f, 1f, 1f)
                .disableBlend()
                .enableCull()
        } finally {
            Renderer.popMatrix()
        }
    }

    private fun CompatDrawMode.toRenderer() = when (this) {
        CompatDrawMode.LINES -> Renderer.DrawMode.LINES
        CompatDrawMode.LINE_STRIP -> Renderer.DrawMode.LINE_STRIP
        CompatDrawMode.TRIANGLES -> Renderer.DrawMode.TRIANGLES
        CompatDrawMode.TRIANGLE_STRIP -> Renderer.DrawMode.TRIANGLE_STRIP
        CompatDrawMode.TRIANGLE_FAN -> Renderer.DrawMode.TRIANGLE_FAN
        CompatDrawMode.QUADS -> Renderer.DrawMode.QUADS
    }

    private fun CompatVertexFormat.toRenderer() = when (this) {
        CompatVertexFormat.POSITION -> Renderer.VertexFormat.POSITION
        CompatVertexFormat.POSITION_COLOR -> Renderer.VertexFormat.POSITION_COLOR
        CompatVertexFormat.POSITION_TEXTURE -> Renderer.VertexFormat.POSITION_TEXTURE
        CompatVertexFormat.POSITION_TEXTURE_COLOR -> Renderer.VertexFormat.POSITION_TEXTURE_COLOR
    }

}
