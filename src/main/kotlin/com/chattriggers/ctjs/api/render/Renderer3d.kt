package com.chattriggers.ctjs.api.render

import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.client.Settings
import com.chattriggers.ctjs.api.vec.Vec3f
import com.chattriggers.ctjs.internal.utils.get
import com.mojang.blaze3d.systems.RenderSystem
import gg.essential.elementa.dsl.component1
import gg.essential.elementa.dsl.component2
import gg.essential.elementa.dsl.component3
import gg.essential.elementa.dsl.component4
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import org.joml.Vector3f
import org.mozilla.javascript.NativeObject
import java.awt.Color
import kotlin.math.sqrt

object Renderer3d {
    private var firstVertex = true
    private var began = false

    private val tessellator = Tessellator.getInstance()
    private val worldRenderer = UGraphics(tessellator.buffer)

    /**
     * Begin drawing with the world renderer
     *
     * @param drawMode the GL draw mode
     * @param vertexFormat The [VertexFormat] to use for drawing
     * @return [Renderer3d] to allow for method chaining
     * @see Renderer.DrawMode
     */
    @JvmStatic
    @JvmOverloads
    fun begin(
        drawMode: Renderer.DrawMode = Renderer.DrawMode.QUADS,
        vertexFormat: Renderer.VertexFormat = Renderer.VertexFormat.POSITION,
    ) = apply {
        Renderer.pushMatrix()
        Renderer.enableBlend()
        Renderer.tryBlendFuncSeparate(770, 771, 1, 0)

        worldRenderer.beginWithDefaultShader(drawMode.toUC(), vertexFormat.toMC())

        firstVertex = true
        began = true
    }

    /**
     * Sets a new vertex in the world renderer.
     *
     * @param x the x position
     * @param y the y position
     * @param z the z position
     * @return [Renderer3d] to allow for method chaining
     */
    @JvmStatic
    fun pos(x: Float, y: Float, z: Float) = apply {
        if (!began)
            begin()
        if (!firstVertex)
            worldRenderer.endVertex()
        worldRenderer.pos(Renderer.matrixStack, x.toDouble(), y.toDouble(), z.toDouble())
        firstVertex = false
    }

    /**
     * Sets the texture location on the last defined vertex.
     *
     * @param u the u position in the texture
     * @param v the v position in the texture
     * @return [Renderer3d] to allow for method chaining
     */
    @JvmStatic
    fun tex(u: Float, v: Float) = apply {
        worldRenderer.tex(u.toDouble(), v.toDouble())
    }

    /**
     * Sets the color for the last defined vertex.
     *
     * @param r the red value of the color, between 0 and 1
     * @param g the green value of the color, between 0 and 1
     * @param b the blue value of the color, between 0 and 1
     * @param a the alpha value of the color, between 0 and 1
     * @return [Renderer3d] to allow for method chaining
     */
    @JvmStatic
    @JvmOverloads
    fun color(r: Float, g: Float, b: Float, a: Float = 1f) = apply {
        worldRenderer.color(r, g, b, a)
    }

    /**
     * Sets the color for the last defined vertex.
     *
     * @param r the red value of the color, between 0 and 255
     * @param g the green value of the color, between 0 and 255
     * @param b the blue value of the color, between 0 and 255
     * @param a the alpha value of the color, between 0 and 255
     * @return [Renderer3d] to allow for method chaining
     */
    @JvmStatic
    @JvmOverloads
    fun color(r: Int, g: Int, b: Int, a: Int = 255) = apply {
        worldRenderer.color(r, g, b, a)
    }

    /**
     * Sets the color for the last defined vertex.
     *
     * @param color the color value, can use [Renderer.getColor] to get this
     * @return [Renderer3d] to allow for method chaining
     */
    @JvmStatic
    fun color(color: Long) = apply {
        val (r, g, b, a) = Color(color.toInt())
        color(r, g, b, a)
    }

    /**
     * Sets the normal of the vertex. This is mostly used with [Renderer.VertexFormat.LINES]
     *
     * @param x the x position of the normal vector
     * @param y the y position of the normal vector
     * @param z the z position of the normal vector
     * @return [Renderer3d] to allow for method chaining
     */
    @JvmStatic
    fun normal(x: Float, y: Float, z: Float) = apply {
        worldRenderer.norm(Renderer.matrixStack, x, y, z)
    }

    /**
     * Sets the overlay location on the last defined vertex.
     *
     * @param u the u position in the overlay
     * @param v the v position in the overlay
     * @return [Renderer3d] to allow for method chaining
     */
    @JvmStatic
    fun overlay(u: Int, v: Int) = apply {
        worldRenderer.overlay(u, v)
    }

    /**
     * Sets the light location on the last defined vertex.
     *
     * @param u the u position in the light
     * @param v the v position in the light
     * @return [Renderer3d] to allow for method chaining
     */
    @JvmStatic
    fun light(u: Int, v: Int) = apply {
        worldRenderer.light(u, v)
    }

    /**
     * End the current vertex
     *
     * @return [Renderer3d] to allow for method chaining
     */
    @JvmStatic
    fun endVertex() = apply {
        worldRenderer.endVertex()
    }

    /**
     * Finalizes vertices and draws the world renderer.
     */
    @JvmStatic
    fun draw() {
        if (!began)
            return
        began = false

        worldRenderer.endVertex()

        worldRenderer.drawDirect()
        Renderer.colorize(1f, 1f, 1f, 1f)
            .disableBlend()
            .popMatrix()
    }

    /**
     * Renders floating lines of text in the 3D world at a specific position.
     * This should be placed inside a `preRenderWorld` trigger.
     *
     * @param text The string array of text to render
     * @param x X coordinate in the game world
     * @param y Y coordinate in the game world
     * @param z Z coordinate in the game world
     * @param color the color of the text
     * @param renderBlackBox render a pretty black border behind the text
     * @param scale the scale of the text
     * @param increase whether to scale the text up as the player moves away
     */
    @JvmStatic
    @JvmOverloads
    fun drawString(
        text: String,
        x: Float,
        y: Float,
        z: Float,
        color: Long = Renderer.colorized ?: Renderer.WHITE,
        renderBlackBox: Boolean = true,
        scale: Float = 1f,
        increase: Boolean = false,
        centered: Boolean = true,
    ) {
        val (lines, width, height) = Renderer.splitText(text)

        val fontRenderer = Renderer.getFontRenderer()
        val camera = Client.getMinecraft().gameRenderer.camera
        val renderPos = Vec3f(
            x - camera.pos.x.toFloat(),
            y - camera.pos.y.toFloat(),
            z - camera.pos.z.toFloat(),
        )

        val lScale = scale * if (increase) {
            val distance =
                sqrt(renderPos.x * renderPos.x + renderPos.y * renderPos.y + renderPos.z * renderPos.z)
            val multiplier = distance / 120f //mobs only render ~120 blocks away
            multiplier
        } else {
            0.025f
        }

        Renderer.colorize(1f, 1f, 1f, 0.5f)
        Renderer.pushMatrix()
        Renderer.translate(renderPos.x, renderPos.y, renderPos.z)
        Renderer.multiply(camera.rotation)
        Renderer.scale(-lScale, -lScale, lScale)

        val opacity = (Settings.toMC().getTextBackgroundOpacity(0.25f) * 255).toInt() shl 24

        val xShift = -(width / 2)
        val yShift = -(height / 2)

        val vertexConsumers = MinecraftClient.getInstance().bufferBuilders.entityVertexConsumers
        var yOffset = 0

        for (line in lines) {
            val centerShift = if (centered) {
                xShift + (fontRenderer.getWidth(line).toFloat() / 2)
            } else 0f

            Renderer.pushMatrix()
            val matrix = Renderer.matrixStack.toMC().peek().positionMatrix

            if (renderBlackBox) {
                fontRenderer.draw(
                    line,
                    xShift - centerShift,
                    yShift + yOffset,
                    553648127,
                    false,
                    matrix,
                    vertexConsumers,
                    TextRenderer.TextLayerType.NORMAL,
                    opacity,
                    0xff,
                )
            }

            fontRenderer.draw(
                line,
                xShift - centerShift,
                yShift + yOffset,
                color.toInt(),
                false,
                matrix,
                vertexConsumers,
                TextRenderer.TextLayerType.NORMAL,
                0,
                0xff,
            )

            Renderer.popMatrix()

            yOffset += fontRenderer.fontHeight + 1
        }

        Renderer.popMatrix()
    }

    /**
     * A variant of drawString that takes an object instead of positional parameters
     */
    @JvmStatic
    fun drawString(obj: NativeObject) {
        drawString(
            obj.get<String>("text") ?: error("Expected \"text\" property in object passed to Renderer3d.drawString"),
            obj.get<Number>("x")?.toFloat() ?: error("Expected \"x\" property in object passed to Renderer3d.drawString"),
            obj.get<Number>("y")?.toFloat() ?: error("Expected \"y\" property in object passed to Renderer3d.drawString"),
            obj.get<Number>("z")?.toFloat() ?: error("Expected \"z\" property in object passed to Renderer3d.drawString"),
            obj.get<Number>("color")?.toLong() ?: Renderer.colorized ?: Renderer.WHITE,
            obj.get<Boolean>("renderBlackBox") ?: true,
            obj.get<Number>("scale")?.toFloat() ?: 1f,
            obj.get<Boolean>("increase") ?: false,
            obj.get<Boolean>("centered") ?: true,
        )
    }

    /**
     * Draws a line in the world from (x1, y1, z1) to (x2, y2, z2)
     *
     * @param color the color of the line
     * @param x1 the starting x coordinate
     * @param y1 the starting y coordinate
     * @param z1 the starting z coordinate
     * @param x2 the ending x coordinate
     * @param y2 the ending y coordinate
     * @param z2 the ending z coordinate
     * @param thickness how thick the line should be
     */
    @JvmStatic
    fun drawLine(
        color: Long,
        x1: Float,
        y1: Float,
        z1: Float,
        x2: Float,
        y2: Float,
        z2: Float,
        thickness: Float,
    ) {
        Renderer.pushMatrix(UMatrixStack())
            .disableDepth()
            .rotateToCamera()
            .translateToPlayer()
            .disableCull()
        RenderSystem.lineWidth(thickness)

        val (r, g, b, a) = Color(color.toInt(), true)

        val normalVec = Vector3f(x2 - x1, y2 - y1, z2 - z1).normalize()

        begin(Renderer.DrawMode.LINES, Renderer.VertexFormat.LINES)
        pos(x1, y1, z1).color(r, g, b, a).normal(normalVec.x, normalVec.y, normalVec.z).endVertex()
        pos(x2, y2, z2).color(r, g, b, a).normal(normalVec.x, normalVec.y, normalVec.z).endVertex()
        draw()

        RenderSystem.lineWidth(1f)
        Renderer
            .enableCull()
            .enableDepth()
            .popMatrix()
    }
}
