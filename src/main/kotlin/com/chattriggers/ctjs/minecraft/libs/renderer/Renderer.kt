package com.chattriggers.ctjs.minecraft.libs.renderer

import com.chattriggers.ctjs.minecraft.libs.ChatLib
import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.minecraft.wrappers.Player
import com.chattriggers.ctjs.minecraft.wrappers.Settings
import com.chattriggers.ctjs.minecraft.wrappers.entity.PlayerMP
import com.chattriggers.ctjs.mixins.EntityRenderDispatcherAccessor
import com.chattriggers.ctjs.utils.*
import com.chattriggers.ctjs.utils.vec.Vec3f
import com.mojang.blaze3d.systems.RenderSystem
import gg.essential.elementa.dsl.component1
import gg.essential.elementa.dsl.component2
import gg.essential.elementa.dsl.component3
import gg.essential.elementa.dsl.component4
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UMinecraft
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.render.DiffuseLighting
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.entity.EntityRendererFactory
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.mozilla.javascript.NativeObject
import java.awt.Color
import java.util.*
import kotlin.math.*

object Renderer {
    private val NEWLINE_REGEX = """\n|\r\n?""".toRegex()

    @JvmField
    var colorized: Long? = null
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

    @JvmField
    val screen = ScreenWrapper()

    // The current partialTicks value
    @JvmStatic
    var partialTicks = 0f
        internal set

    @JvmField
    val BLACK = getColor(0, 0, 0, 255)

    @JvmField
    val DARK_BLUE = getColor(0, 0, 190, 255)

    @JvmField
    val DARK_GREEN = getColor(0, 190, 0, 255)

    @JvmField
    val DARK_AQUA = getColor(0, 190, 190, 255)

    @JvmField
    val DARK_RED = getColor(190, 0, 0, 255)

    @JvmField
    val DARK_PURPLE = getColor(190, 0, 190, 255)

    @JvmField
    val GOLD = getColor(217, 163, 52, 255)

    @JvmField
    val GRAY = getColor(190, 190, 190, 255)

    @JvmField
    val DARK_GRAY = getColor(63, 63, 63, 255)

    @JvmField
    val BLUE = getColor(63, 63, 254, 255)

    @JvmField
    val GREEN = getColor(63, 254, 63, 255)

    @JvmField
    val AQUA = getColor(63, 254, 254, 255)

    @JvmField
    val RED = getColor(254, 63, 63, 255)

    @JvmField
    val LIGHT_PURPLE = getColor(254, 63, 254, 255)

    @JvmField
    val YELLOW = getColor(254, 254, 63, 255)

    @JvmField
    val WHITE = getColor(255, 255, 255, 255)

    @JvmStatic
    fun getColor(color: Int): Long {
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
    fun getColor(red: Int, green: Int, blue: Int, alpha: Int = 255): Long {
        return ((alpha.coerceIn(0, 255) shl 24) or
            (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)).toLong()
    }

    @JvmStatic
    @JvmOverloads
    fun getRainbow(step: Float, speed: Float = 1f): Long {
        val red = ((sin(step / speed) + 0.75) * 170).toInt()
        val green = ((sin(step / speed + 2 * PI / 3) + 0.75) * 170).toInt()
        val blue = ((sin(step / speed + 4 * PI / 3) + 0.75) * 170).toInt()
        return getColor(red, green, blue)
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

    @JvmStatic
    fun translateToPlayer() = apply {
        translate(-Client.camera.getX().toFloat(), -Client.camera.getY().toFloat(), -Client.camera.getZ().toFloat())
    }

    /**
     * Begin drawing with the world renderer
     *
     * @param drawMode the GL draw mode
     * @param vertexFormat The [VertexFormat] to use for drawing
     * @return the Renderer to allow for method chaining
     * @see com.chattriggers.ctjs.minecraft.libs.renderer.Shape.setDrawMode
     */
    @JvmStatic
    @JvmOverloads
    fun begin(
        drawMode: DrawMode? = Renderer.drawMode,
        vertexFormat: VertexFormat = VertexFormat.POSITION,
    ) = apply {
        pushMatrix()
        enableBlend()
        tryBlendFuncSeparate(770, 771, 1, 0)

        worldRenderer.beginWithDefaultShader(drawMode?.toUC() ?: UGraphics.DrawMode.QUADS, vertexFormat.toUC())

        firstVertex = true
        began = true
    }

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
    fun color(x: Float, y: Float, z: Float, a: Float = 1f) = apply {
        worldRenderer.color(x, y, z, a)
    }

    @JvmStatic
    @JvmOverloads
    fun color(x: Int, y: Int, z: Int, a: Int = 255) = apply {
        worldRenderer.color(x, y, z, a)
    }

    @JvmStatic
    fun color(color: Long) = apply {
        val (r, g, b, a) = Color(color.toInt())
        color(r, g, b, a)
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
    fun multiply(quaternion: Quaternionf) = apply {
        matrixStack.multiply(quaternion)
    }

    @JvmStatic
    @JvmOverloads
    fun colorize(red: Float, green: Float, blue: Float, alpha: Float = 1f) = apply {
        colorized = fixAlpha(getColor(red.toInt(), green.toInt(), blue.toInt(), alpha.toInt()))
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
    fun fixAlpha(color: Long): Long {
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
    fun drawRect(color: Long, x: Float, y: Float, width: Float, height: Float) = apply {
        val pos = mutableListOf(x, y, x + width, y + height)
        if (pos[0] > pos[2])
            Collections.swap(pos, 0, 2)
        if (pos[1] > pos[3])
            Collections.swap(pos, 1, 3)

        doColor(color)

        begin(drawMode ?: DrawMode.QUADS)
        pos(pos[0], pos[3], 0f)
        pos(pos[2], pos[3], 0f)
        pos(pos[2], pos[1], 0f)
        pos(pos[0], pos[1], 0f)
        draw()

        colorize(1f, 1f, 1f, 1f)
        disableBlend()

        resetTransformsIfNecessary()
    }

    @JvmStatic
    fun drawLine(
        color: Long,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        thickness: Float,
    ) {
        val theta = -atan2(y2 - y1, x2 - x1)
        val i = sin(theta) * (thickness / 2)
        val j = cos(theta) * (thickness / 2)

        doColor(color)

        begin(DrawMode.QUADS, VertexFormat.POSITION)
        pos(x1 + i, y1 + j, 0f)
        pos(x2 + i, y2 + j, 0f)
        pos(x2 - i, y2 - j, 0f)
        pos(x1 - i, y1 - j, 0f)
        draw()

        colorize(1f, 1f, 1f, 1f)
        disableBlend()

        resetTransformsIfNecessary()
    }

    @JvmStatic
    fun drawCircle(
        color: Long,
        x: Float,
        y: Float,
        radius: Float,
        steps: Int,
    ) {
        val theta = 2 * PI / steps
        val cos = cos(theta).toFloat()
        val sin = sin(theta).toFloat()

        var xHolder: Float
        var circleX = 1f
        var circleY = 0f

        doColor(color)

        begin(Renderer.DrawMode.TRIANGLE_STRIP)

        for (i in 0..steps) {
            pos(x, y, 0f)
            pos(circleX * radius + x, circleY * radius + y, 0f)
            xHolder = circleX
            circleX = cos * circleX - sin * circleY
            circleY = sin * xHolder + cos * circleY
            pos(circleX * radius + x, circleY * radius + y, 0f)
        }

        draw()

        colorize(1f, 1f, 1f, 1f)
        disableBlend()

        resetTransformsIfNecessary()
    }

    @JvmOverloads
    @JvmStatic
    fun drawString(text: String, x: Float, y: Float, color: Long = colorized ?: WHITE, shadow: Boolean = false) {
        val fr = getFontRenderer()
        var newY = y

        splitText(text).lines.forEach {
            if (shadow) {
                fr.drawWithShadow(matrixStack.toMC(), it, x, newY, color.toInt())
            } else {
                fr.draw(matrixStack.toMC(), it, x, newY, color.toInt())
            }

            newY += fr.fontHeight
        }

        resetTransformsIfNecessary()
    }

    @JvmOverloads
    @JvmStatic
    fun drawStringWithShadow(text: String, x: Float, y: Float, color: Long = colorized ?: WHITE) = drawString(text, x, y, color, shadow = true)

    /**
     * Renders floating lines of text in the 3D world at a specific position.
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
    fun drawString3D(
        text: String,
        x: Float,
        y: Float,
        z: Float,
        color: Long = colorized ?: WHITE,
        renderBlackBox: Boolean = true,
        scale: Float = 1f,
        increase: Boolean = false,
        centered: Boolean = true,
    ) {
        val (lines, width, height) = splitText(text)

        val fontRenderer = getFontRenderer()
        val camera = Client.getMinecraft().gameRenderer.camera
        val renderPos = Vec3f(
            x - camera.pos.x.toFloat(),
            y - camera.pos.y.toFloat(),
            z - camera.pos.z.toFloat(),
        )

        val lScale = scale * if (increase) {
            val distance = sqrt(renderPos.x * renderPos.x + renderPos.y * renderPos.y + renderPos.z * renderPos.z)
            val multiplier = distance / 120f //mobs only render ~120 blocks away
            multiplier
        } else {
            0.025f
        }

        colorize(1f, 1f, 1f, 0.5f)
        pushMatrix()
        translate(renderPos.x, renderPos.y, renderPos.z)
        multiply(camera.rotation)
        scale(-lScale, -lScale, lScale)

        val opacity = (Settings.toMC().getTextBackgroundOpacity(0.25f) * 255).toInt() shl 24

        val xShift = -(width / 2)
        val yShift = -(height / 2)

        val vertexConsumers = MinecraftClient.getInstance().bufferBuilders.entityVertexConsumers
        var yOffset = 0

        for (line in lines) {
            val centerShift = if (centered) {
                xShift + (fontRenderer.getWidth(line).toFloat() / 2)
            } else 0f

            pushMatrix()
            val matrix = matrixStack.toMC().peek().positionMatrix

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
                    0xf00000,
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
                0xf00000,
            )

            popMatrix()

            yOffset += fontRenderer.fontHeight + 1
        }

        popMatrix()
    }

    /**
     * A variant of drawString3D that takes an object instead of positional parameters
     */
    @JvmStatic
    fun drawString3D(obj: NativeObject) {
        drawString3D(
            obj.get<String>("text") ?: error("Expected \"text\" property in object passed to Renderer.drawString3D"),
            obj.get<Number>("x")?.toFloat() ?: error("Expected \"x\" property in object passed to Renderer.drawString3D"),
            obj.get<Number>("y")?.toFloat() ?: error("Expected \"y\" property in object passed to Renderer.drawString3D"),
            obj.get<Number>("z")?.toFloat() ?: error("Expected \"z\" property in object passed to Renderer.drawString3D"),
            obj.get<Number>("color")?.toLong() ?: colorized ?: WHITE,
            obj.get<Boolean>("renderBlackBox") ?: true,
            obj.get<Number>("scale")?.toFloat() ?: 1f,
            obj.get<Boolean>("increase") ?: false,
            obj.get<Boolean>("centered") ?: true,
        )
    }

    private data class TextLines(val lines: List<String>, val width: Float, val height: Float)

    private fun splitText(text: String): TextLines {
        val lines = ChatLib.addColor(text).split(NEWLINE_REGEX)
        return TextLines(
            lines,
            lines.maxOf { getFontRenderer().getWidth(it) }.toFloat(),
            (getFontRenderer().fontHeight * lines.size + (lines.size - 1)).toFloat(),
        )
    }

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

    internal fun doColor(color: Long) {
        if (colorized == null) {
            val (r, g, b, a) = Color(color.toInt(), true)
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
        worldRenderer.drawDirect()

        colorize(1f, 1f, 1f, 1f)
        disableBlend()
        resetTransformsIfNecessary()
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

    enum class VertexFormat(private val ucValue: UGraphics.CommonVertexFormats) {
        POSITION(UGraphics.CommonVertexFormats.POSITION),
        POSITION_COLOR(UGraphics.CommonVertexFormats.POSITION_COLOR),
        POSITION_TEXTURE(UGraphics.CommonVertexFormats.POSITION_TEXTURE),
        POSITION_TEXTURE_COLOR(UGraphics.CommonVertexFormats.POSITION_TEXTURE_COLOR),
        POSITION_COLOR_TEXTURE_LIGHT(UGraphics.CommonVertexFormats.POSITION_COLOR_TEXTURE_LIGHT),
        POSITION_TEXTURE_LIGHT_COLOR(UGraphics.CommonVertexFormats.POSITION_TEXTURE_LIGHT_COLOR),
        POSITION_TEXTURE_COLOR_LIGHT(UGraphics.CommonVertexFormats.POSITION_TEXTURE_COLOR_LIGHT),
        POSITION_TEXTURE_COLOR_NORMAL(UGraphics.CommonVertexFormats.POSITION_TEXTURE_COLOR_NORMAL);

        fun toUC() = ucValue

        companion object {
            @JvmStatic
            fun fromUC(ucValue: UGraphics.CommonVertexFormats) = values().first { it.ucValue == ucValue }
        }
    }

    class ScreenWrapper {
        fun getWidth(): Int = UMinecraft.getMinecraft().window.scaledWidth

        fun getHeight(): Int = UMinecraft.getMinecraft().window.scaledHeight

        fun getScale(): Double = UMinecraft.getMinecraft().window.scaleFactor
    }
}
