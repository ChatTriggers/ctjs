package com.chattriggers.ctjs.minecraft.libs.renderer

import com.chattriggers.ctjs.minecraft.libs.ChatLib
import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.minecraft.wrappers.Player
import com.chattriggers.ctjs.minecraft.wrappers.entity.PlayerMP
import com.chattriggers.ctjs.mixins.EntityRenderDispatcherAccessor
import com.chattriggers.ctjs.utils.asMixin
import com.chattriggers.ctjs.utils.getOrDefault
import com.chattriggers.ctjs.utils.toRadians
import com.chattriggers.ctjs.utils.vec.Vec3f
import com.mojang.blaze3d.systems.RenderSystem
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UMinecraft
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.render.DiffuseLighting
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.entity.EntityRendererFactory
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.mozilla.javascript.NativeObject
import java.util.*
import kotlin.math.*

object Renderer {
    var colorized: Int? = null
    private var retainTransforms = false
    private var drawMode: DrawMode? = null
    private var firstVertex = true
    private var began = false

    private val tessellator = Tessellator.getInstance()
    private val worldRenderer = UGraphics(tessellator.buffer)

    // The currently-active matrix stack
    internal lateinit var matrixStack: UMatrixStack

    private lateinit var slimCTRenderPlayer: CTPlayerRenderer
    private lateinit var normalCTRenderPlayer: CTPlayerRenderer

    // The current partialTicks value
    @JvmStatic
    var partialTicks = 0f
        internal set

    @JvmStatic
    val BLACK = color(0, 0, 0, 255)

    @JvmStatic
    val DARK_BLUE = color(0, 0, 190, 255)

    @JvmStatic
    val DARK_GREEN = color(0, 190, 0, 255)

    @JvmStatic
    val DARK_AQUA = color(0, 190, 190, 255)

    @JvmStatic
    val DARK_RED = color(190, 0, 0, 255)

    @JvmStatic
    val DARK_PURPLE = color(190, 0, 190, 255)

    @JvmStatic
    val GOLD = color(217, 163, 52, 255)

    @JvmStatic
    val GRAY = color(190, 190, 190, 255)

    @JvmStatic
    val DARK_GRAY = color(63, 63, 63, 255)

    @JvmStatic
    val BLUE = color(63, 63, 254, 255)

    @JvmStatic
    val GREEN = color(63, 254, 63, 255)

    @JvmStatic
    val AQUA = color(63, 254, 254, 255)

    @JvmStatic
    val RED = color(254, 63, 63, 255)

    @JvmStatic
    val LIGHT_PURPLE = color(254, 63, 254, 255)

    @JvmStatic
    val YELLOW = color(254, 254, 63, 255)

    @JvmStatic
    val WHITE = color(255, 255, 255, 255)

    @JvmStatic
    fun getColor(color: Int): Int {
        return when (color) {
            0 -> BLACK
            1 -> DARK_BLUE
            2 -> DARK_GREEN
            3 -> DARK_AQUA
            4 -> DARK_RED
            5 -> DARK_PURPLE
            6 -> GOLD
            7 -> GRAY
            8 -> DARK_GRAY
            9 -> BLUE
            10 -> GREEN
            11 -> AQUA
            12 -> RED
            13 -> LIGHT_PURPLE
            14 -> YELLOW
            else -> WHITE
        }
    }

    @JvmStatic
    internal fun initializePlayerRenderers(context: EntityRendererFactory.Context) {
        normalCTRenderPlayer = CTPlayerRenderer(context, slim = false)
        slimCTRenderPlayer = CTPlayerRenderer(context, slim = true)
    }

    @JvmStatic
    fun getFontRenderer() = UMinecraft.getFontRenderer()

    @JvmStatic
    fun getRenderManager() = UMinecraft.getMinecraft().worldRenderer

    @JvmStatic
    fun getStringWidth(text: String) = getFontRenderer().getWidth(ChatLib.addColor(text))

    @JvmStatic
    @JvmOverloads
    fun color(red: Int, green: Int, blue: Int, alpha: Int = 255): Int {
        return (alpha.coerceIn(0, 255) shl 24) or
            (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)
    }

    @JvmStatic
    @JvmOverloads
    fun getRainbow(step: Float, speed: Float = 1f): Int {
        val red = ((sin(step / speed) + 0.75) * 170).toInt()
        val green = ((sin(step / speed + 2 * PI / 3) + 0.75) * 170).toInt()
        val blue = ((sin(step / speed + 4 * PI / 3) + 0.75) * 170).toInt()
        return color(red, green, blue)
    }

    @JvmStatic
    @JvmOverloads
    fun getRainbowColors(step: Float, speed: Float = 1f): IntArray {
        val red = ((sin(step / speed) + 0.75) * 170).toInt()
        val green = ((sin(step / speed + 2 * PI / 3) + 0.75) * 170).toInt()
        val blue = ((sin(step / speed + 4 * PI / 3) + 0.75) * 170).toInt()
        return intArrayOf(red, green, blue)
    }

    @JvmStatic
    fun retainTransforms(retain: Boolean) = apply {
        retainTransforms = retain
        resetTransformsIfNecessary()
    }

    @JvmStatic
    fun disableAlpha() = apply { UGraphics.disableAlpha() }

    @JvmStatic
    fun enableAlpha() = apply { UGraphics.enableAlpha() }

    @JvmStatic
    fun enableLighting() = apply { UGraphics.enableLighting() }

    @JvmStatic
    fun disableLighting() = apply { UGraphics.disableLighting() }

    @JvmStatic
    fun enableDepth() = apply { UGraphics.enableDepth() }

    @JvmStatic
    fun disableDepth() = apply { UGraphics.disableDepth() }

    @JvmStatic
    fun depthFunc(func: Int) = apply { UGraphics.depthFunc(func) }

    @JvmStatic
    fun depthMask(flag: Boolean) = apply { UGraphics.depthMask(flag) }

    @JvmStatic
    fun disableBlend() = apply { UGraphics.disableBlend() }

    @JvmStatic
    fun enableBlend() = apply { UGraphics.enableBlend() }

    @JvmStatic
    fun blendFunc(func: Int) = apply { UGraphics.blendEquation(func) }

    @JvmStatic
    fun tryBlendFuncSeparate(sourceFactor: Int, destFactor: Int, sourceFactorAlpha: Int, destFactorAlpha: Int) = apply {
        UGraphics.tryBlendFuncSeparate(sourceFactor, destFactor, sourceFactorAlpha, destFactorAlpha)
    }

    @JvmStatic
    @JvmOverloads
    fun bindTexture(texture: Image, textureIndex: Int = 0) = apply {
        UGraphics.bindTexture(textureIndex, texture.getTexture().glId)
    }

    @JvmStatic
    fun deleteTexture(texture: Image) = apply {
        UGraphics.deleteTexture(texture.getTexture().glId)
    }

    @JvmStatic
    fun pushMatrix() = apply {
        matrixStack.push()
    }

    @JvmStatic
    fun popMatrix() = apply {
        matrixStack.pop()
    }

    /**
     * Begin drawing with the Renderer with default draw mode of quads and textured
     *
     * @param drawMode the GL draw mode
     * @param textured if the Renderer is textured
     * @return the Renderer to allow for method chaining
     * @see com.chattriggers.ctjs.minecraft.libs.renderer.Shape.setDrawMode
     */
    @JvmStatic
    @JvmOverloads
    fun begin(drawMode: DrawMode? = Renderer.drawMode, textured: Boolean = true) = apply {
        pushMatrix()
        enableBlend()
        tryBlendFuncSeparate(770, 771, 1, 0)

        translate(-Client.camera.getX().toFloat(), -Client.camera.getY().toFloat(), -Client.camera.getZ().toFloat())

        worldRenderer.beginWithDefaultShader(
            drawMode?.toUC() ?: UGraphics.DrawMode.QUADS,
            if (textured) UGraphics.CommonVertexFormats.POSITION_TEXTURE else UGraphics.CommonVertexFormats.POSITION,
        )

        firstVertex = true
        began = true
    }

    @JvmStatic
    @JvmOverloads
    fun begin(drawMode: Int, textured: Boolean = true) =
        begin(DrawMode.fromUC(UGraphics.DrawMode.fromGl(drawMode)), textured)

    /**
     * Sets a new vertex in the Tessellator.
     *
     * @param x the x position
     * @param y the y position
     * @param z the z position
     * @return the Tessellator to allow for method chaining
     */
    @JvmStatic
    fun pos(x: Float, y: Float, z: Float) = apply {
        if (!began)
            begin()
        if (!firstVertex)
            worldRenderer.endVertex()
        worldRenderer.pos(matrixStack, x.toDouble(), y.toDouble(), z.toDouble())
        firstVertex = false
    }

    /**
     * Sets the texture location on the last defined vertex.
     * Use directly after using [Tessellator.pos]
     *
     * @param u the u position in the texture
     * @param v the v position in the texture
     * @return the Tessellator to allow for method chaining
     */
    @JvmStatic
    fun tex(u: Float, v: Float) = apply {
        worldRenderer.tex(u.toDouble(), v.toDouble())
    }

    @JvmStatic
    @JvmOverloads
    fun translate(x: Float, y: Float, z: Float = 0.0F) = apply {
        matrixStack.translate(x, y, z)
    }

    @JvmStatic
    @JvmOverloads
    fun scale(scaleX: Float, scaleY: Float = scaleX, scaleZ: Float = 1f) = apply {
        matrixStack.scale(scaleX, scaleY, scaleZ)
    }

    @JvmStatic
    fun rotate(angle: Float) = apply {
        matrixStack.rotate(angle, 0f, 0f, 1f)
    }

    @JvmStatic
    @JvmOverloads
    fun colorize(red: Float, green: Float, blue: Float, alpha: Float = 1f) = apply {
        colorized = fixAlpha(color(red.toInt(), green.toInt(), blue.toInt(), alpha.toInt()))
        RenderSystem.setShaderColor(red.coerceIn(0f, 1f), green.coerceIn(0f, 1f), blue.coerceIn(0f, 1f), alpha.coerceIn(0f, 1f))
    }

    @JvmStatic
    @JvmOverloads
    fun colorize(red: Int, green: Int, blue: Int, alpha: Int = 255) =
        colorize(red / 255f, green / 255f, blue / 255f, alpha / 255f)

    @JvmStatic
    fun setDrawMode(drawMode: Int) = setDrawMode(DrawMode.fromUC(UGraphics.DrawMode.fromGl(drawMode)))

    @JvmStatic
    fun setDrawMode(drawMode: DrawMode) = apply {
        this.drawMode = drawMode
    }

    @JvmStatic
    fun getDrawMode() = drawMode

    @JvmStatic
    fun fixAlpha(color: Int): Int {
        val alpha = color ushr 24 and 255
        return if (alpha < 10)
            (color and 0xFF_FF_FF) or 0xA_FF_FF_FF
        else color
    }

    /**
     * Gets a fixed render position from x, y, and z inputs adjusted with partial ticks
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the Vec3f position to render at
     */
    @JvmStatic
    fun getRenderPos(x: Float, y: Float, z: Float): Vec3f {
        return Vec3f(
            x - Player.getRenderX().toFloat(),
            y - Player.getRenderY().toFloat(),
            z - Player.getRenderZ().toFloat()
        )
    }

    @JvmStatic
    fun drawRect(color: Int, x: Float, y: Float, width: Float, height: Float) = apply {
        val pos = mutableListOf(x, y, x + width, y + height)
        if (pos[0] > pos[2])
            Collections.swap(pos, 0, 2)
        if (pos[1] > pos[3])
            Collections.swap(pos, 1, 3)

        UGraphics.enableBlend()
        UGraphics.tryBlendFuncSeparate(770, 771, 1, 0)
        doColor(color)

        worldRenderer.beginWithDefaultShader(
            drawMode?.toUC() ?: UGraphics.DrawMode.QUADS,
            UGraphics.CommonVertexFormats.POSITION
        )
            .pos(matrixStack, pos[0].toDouble(), pos[3].toDouble(), 0.0).endVertex()
            .pos(matrixStack, pos[2].toDouble(), pos[3].toDouble(), 0.0).endVertex()
            .pos(matrixStack, pos[2].toDouble(), pos[1].toDouble(), 0.0).endVertex()
            .pos(matrixStack, pos[0].toDouble(), pos[1].toDouble(), 0.0).endVertex()
            .drawDirect()

        colorize(1f, 1f, 1f, 1f)
        UGraphics.disableBlend()

        resetTransformsIfNecessary()
    }

    // TODO(breaking): Removed drawShape in favor of Shape()

    @JvmStatic
    @JvmOverloads
    fun drawLine(
        color: Int,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        thickness: Float,
        drawMode: UGraphics.DrawMode = UGraphics.DrawMode.QUADS,
    ) {
        val theta = -atan2(y2 - y1, x2 - x1)
        val i = sin(theta) * (thickness / 2)
        val j = cos(theta) * (thickness / 2)

        UGraphics.enableBlend()
        UGraphics.tryBlendFuncSeparate(770, 771, 1, 0)
        doColor(color)

        worldRenderer.beginWithDefaultShader(this.drawMode?.toUC() ?: drawMode, UGraphics.CommonVertexFormats.POSITION)
            .pos(matrixStack, (x1 + i).toDouble(), (y1 + j).toDouble(), 0.0).endVertex()
            .pos(matrixStack, (x2 + i).toDouble(), (y2 + j).toDouble(), 0.0).endVertex()
            .pos(matrixStack, (x2 - i).toDouble(), (y2 - j).toDouble(), 0.0).endVertex()
            .pos(matrixStack, (x1 - i).toDouble(), (y1 - j).toDouble(), 0.0).endVertex()
            .drawDirect()

        colorize(1f, 1f, 1f, 1f)
        UGraphics.disableBlend()

        resetTransformsIfNecessary()
    }

    @JvmStatic
    @JvmOverloads
    fun drawCircle(
        color: Int,
        x: Float,
        y: Float,
        radius: Float,
        steps: Int,
        drawMode: UGraphics.DrawMode = UGraphics.DrawMode.TRIANGLE_STRIP,
    ) {
        val theta = 2 * PI / steps
        val cos = cos(theta).toFloat()
        val sin = sin(theta).toFloat()

        var xHolder: Float
        var circleX = 1f
        var circleY = 0f

        enableBlend()
        tryBlendFuncSeparate(770, 771, 1, 0)
        doColor(color)

        worldRenderer.beginWithDefaultShader(this.drawMode?.toUC() ?: drawMode, UGraphics.CommonVertexFormats.POSITION)

        for (i in 0..steps) {
            worldRenderer.pos(matrixStack, x.toDouble(), y.toDouble(), 0.0).endVertex()
            worldRenderer.pos(matrixStack, (circleX * radius + x).toDouble(), (circleY * radius + y).toDouble(), 0.0)
                .endVertex()
            xHolder = circleX
            circleX = cos * circleX - sin * circleY
            circleY = sin * xHolder + cos * circleY
            worldRenderer.pos(matrixStack, (circleX * radius + x).toDouble(), (circleY * radius + y).toDouble(), 0.0)
                .endVertex()
        }

        worldRenderer.drawDirect()

        colorize(1f, 1f, 1f, 1f)
        disableBlend()

        resetTransformsIfNecessary()
    }

    @JvmOverloads
    @JvmStatic
    fun drawString(text: String, x: Float, y: Float, shadow: Boolean = false) {
        val fr = getFontRenderer()
        var newY = y

        ChatLib.addColor(text).split("\n").forEach {
            if (shadow) {
                fr.drawWithShadow(matrixStack.toMC(), it, x, newY, colorized ?: WHITE)
            } else {
                fr.draw(matrixStack.toMC(), it, x, newY, colorized ?: WHITE)
            }

            newY += fr.fontHeight
        }

        resetTransformsIfNecessary()
    }

    @JvmStatic
    fun drawStringWithShadow(text: String, x: Float, y: Float) = drawString(text, x, y, shadow = true)

    @JvmStatic
    fun drawImage(image: Image, x: Double, y: Double, width: Double, height: Double) {
        if (colorized == null)
            colorize(1f, 1f, 1f, 1f)

        enableBlend()
        scale(1f, 1f, 50f)

        RenderSystem.setShaderTexture(0, image.getTexture().glId)

        worldRenderer.beginWithDefaultShader(
            drawMode?.toUC() ?: UGraphics.DrawMode.QUADS,
            UGraphics.CommonVertexFormats.POSITION_TEXTURE,
        )

        worldRenderer.pos(matrixStack, x, y + height, 0.0).tex(0.0, 1.0).endVertex()
        worldRenderer.pos(matrixStack, x + width, y + height, 0.0).tex(1.0, 1.0).endVertex()
        worldRenderer.pos(matrixStack, x + width, y, 0.0).tex(1.0, 0.0).endVertex()
        worldRenderer.pos(matrixStack, x, y, 0.0).tex(0.0, 0.0).endVertex()
        worldRenderer.drawDirect()

        resetTransformsIfNecessary()
    }

    /**
     * Draws a player entity to the screen, similar to the one displayed in the inventory screen.
     *
     * Takes a parameter with the following options:
     * - player: The player entity to draw. Can be a [PlayerMP] or [AbstractClientPlayerEntity].
     *           Defaults to Player.toMC()
     * - x: The x position on the screen to render the player
     * - y: The y position on the screen to render the player
     * - size: The size of the rendered player
     * - rotate: Whether the player should look at the mouse cursor, similar to the inventory screen
     * - pitch: THe pitch the rendered player will face, if rotate is false
     * - yaw: The yaw the rendered player will face, if rotate is false
     * - showNametag: Whether the nametag of the player should be rendered
     * - showArmor: Whether the armor of the player should be rendered
     * - showCape: Whether the cape of the player should be rendered
     * - showHeldItem: Whether the held item of the player should be rendered
     * - showArrows: Whether any arrows stuck in the player's model should be rendered
     * - showElytra: Whether the player's Elytra should be rendered
     * - showParrot: Whether a perched parrot should be rendered
     * - showBeeStinger: Whether any stuck bee stingers should be rendered
     *
     * @param obj An options bag
     */
    // TODO(breaking): Takes an object instead of 85 parameters, since more were added and the parameter list
    //                 was far to large to be practical
    @JvmStatic
    fun drawPlayer(obj: NativeObject) {
        val entity = obj["player"].let {
            it as? AbstractClientPlayerEntity
                ?: ((it as? PlayerMP)?.toMC() as? AbstractClientPlayerEntity)
                ?: Player.toMC()
                ?: return
        }

        val x = obj.getOrDefault<Number>("x", 0).toInt()
        val y = obj.getOrDefault<Number>("y", 0).toInt()
        val size = obj.getOrDefault<Number>("size", 20).toDouble()
        val rotate = obj.getOrDefault<Boolean>("rotate", false)
        val pitch = obj.getOrDefault<Number>("pitch", 0f).toFloat()
        val yaw = obj.getOrDefault<Number>("yaw", 0f).toFloat()
        val slim = obj.getOrDefault<Boolean>("slim", false)
        val showNametag = obj.getOrDefault<Boolean>("showNametag", false)
        val showArmor = obj.getOrDefault<Boolean>("showArmor", false)
        val showCape = obj.getOrDefault<Boolean>("showCape", false)
        val showHeldItem = obj.getOrDefault<Boolean>("showHeldItem", false)
        val showArrows = obj.getOrDefault<Boolean>("showArrows", false)
        val showElytra = obj.getOrDefault<Boolean>("showElytra", false)
        val showParrot = obj.getOrDefault<Boolean>("showParrot", false)
        val showStingers = obj.getOrDefault<Boolean>("showBeeStinger", false)

        matrixStack.push()

        val (entityYaw, entityPitch) = if (rotate) {
            val mouseX = x - Client.getMouseX()
            val mouseY = y - Client.getMouseY() - (entity.standingEyeHeight * size)
            atan((mouseX / 40.0f)).toFloat() to atan((mouseY / 40.0f)).toFloat()
        } else {
            val scaleFactor = 130f / 180f
            (yaw * scaleFactor).toRadians() to pitch.toRadians()
        }

        val flipModelRotation = Quaternionf().rotateZ(Math.PI.toFloat())
        val pitchModelRotation = Quaternionf().rotateX(entityPitch * 20.0f * (Math.PI / 180.0).toFloat())
        flipModelRotation.mul(pitchModelRotation)

        val oldBodyYaw = entity.bodyYaw
        val oldYaw = entity.yaw
        val oldPitch = entity.pitch
        val oldPrevHeadYaw = entity.prevHeadYaw
        val oldHeadYaw = entity.headYaw

        entity.bodyYaw = 180.0f + entityYaw * 20.0f
        entity.yaw = 180.0f + entityYaw * 40.0f
        entity.pitch = -entityPitch * 20.0f
        entity.headYaw = entity.yaw
        entity.prevHeadYaw = entity.yaw

        matrixStack.push()
        matrixStack.translate(0.0, 0.0, 1000.0)
        matrixStack.push()
        matrixStack.translate(x.toDouble(), y.toDouble(), -950.0)

        // UC's version of multiplyPositionMatrix
        matrixStack.peek().model.mul(Matrix4f().scaling(size.toFloat(), size.toFloat(), (-size).toFloat()))

        matrixStack.multiply(flipModelRotation)
        DiffuseLighting.method_34742()

        val entityRenderDispatcher = MinecraftClient.getInstance().entityRenderDispatcher

        if (pitchModelRotation != null) {
            pitchModelRotation.conjugate()
            entityRenderDispatcher.rotation = pitchModelRotation
        }

        entityRenderDispatcher.setRenderShadows(false)
        val vertexConsumers = MinecraftClient.getInstance().bufferBuilders.entityVertexConsumers

        val light = 0xf000f0

        val entityRenderer = if (slim) slimCTRenderPlayer else normalCTRenderPlayer
        entityRenderer.setOptions(showNametag, showArmor, showCape, showHeldItem, showArrows, showElytra, showParrot, showStingers)

        val vec3d = entityRenderer.getPositionOffset(entity, partialTicks)
        val d = vec3d.getX()
        val e = vec3d.getY()
        val f = vec3d.getZ()
        matrixStack.push()
        matrixStack.translate(d, e, f)
        RenderSystem.runAsFancy {
            entityRenderer.render(entity, 0.0f, 1.0f, matrixStack.toMC(), vertexConsumers, light)
            if (entity.doesRenderOnFire()) {
                entityRenderDispatcher.asMixin<EntityRenderDispatcherAccessor>().invokeRenderFire(matrixStack.toMC(), vertexConsumers, entity)
            }
        }

        this.matrixStack.pop()

        vertexConsumers.draw()
        entityRenderDispatcher.setRenderShadows(true)
        matrixStack.pop()
        DiffuseLighting.enableGuiDepthLighting()
        matrixStack.pop()

        entity.bodyYaw = oldBodyYaw
        entity.yaw = oldYaw
        entity.pitch = oldPitch
        entity.prevHeadYaw = oldPrevHeadYaw
        entity.headYaw = oldHeadYaw

        this.matrixStack.pop()
    }

    internal fun doColor(color: Int) {
        if (colorized == null) {
            val a = (color ushr 24 and 255).toFloat() / 255.0f
            val r = (color ushr 16 and 255).toFloat() / 255.0f
            val g = (color ushr 8 and 255).toFloat() / 255.0f
            val b = (color and 255).toFloat() / 255.0f
            colorize(r, g, b, a)
        }
    }

    /**
     * Finalizes vertices and draws the Renderer.
     */
    @JvmStatic
    fun draw() {
        if (!began)
            return
        began = false

        worldRenderer.endVertex()
        tessellator.draw()

        colorize(1f, 1f, 1f, 1f)
        disableBlend()
        resetTransformsIfNecessary()
    }

    private fun area(points: Array<out List<Float>>): Float {
        var area = 0f

        for (i in points.indices) {
            val (x1, y1) = points[i]
            val (x2, y2) = points[(i + 1) % points.size]

            area += x1 * y2 - x2 * y1
        }

        return area / 2
    }

    internal fun resetTransformsIfNecessary() {
        if (!retainTransforms) {
            colorized = null
            drawMode = null
            popMatrix()
            pushMatrix()
        }
    }

    enum class DrawMode(private val ucValue: UGraphics.DrawMode) {
        LINES(UGraphics.DrawMode.LINES),
        LINE_STRIP(UGraphics.DrawMode.LINE_STRIP),
        TRIANGLES(UGraphics.DrawMode.TRIANGLES),
        TRIANGLE_STRIP(UGraphics.DrawMode.TRIANGLE_STRIP),
        TRIANGLE_FAN(UGraphics.DrawMode.TRIANGLE_FAN),
        QUADS(UGraphics.DrawMode.QUADS);

        fun toUC() = ucValue

        companion object {
            @JvmStatic
            fun fromUC(ucValue: UGraphics.DrawMode) = values().first { it.ucValue == ucValue }
        }
    }

    object screen {
        @JvmStatic
        fun getWidth(): Int = UMinecraft.getMinecraft().window.scaledWidth

        @JvmStatic
        fun getHeight(): Int = UMinecraft.getMinecraft().window.scaledHeight

        @JvmStatic
        fun getScale(): Double = UMinecraft.getMinecraft().window.scaleFactor
    }
}
