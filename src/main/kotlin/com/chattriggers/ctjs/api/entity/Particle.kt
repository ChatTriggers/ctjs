package com.chattriggers.ctjs.api.entity

import com.chattriggers.ctjs.api.CTWrapper
import com.chattriggers.ctjs.api.render.Renderer
import com.chattriggers.ctjs.internal.mixins.ParticleAccessor
import com.chattriggers.ctjs.internal.mixins.SingleQuadParticleAccessor
import com.chattriggers.ctjs.MCParticle
import com.chattriggers.ctjs.internal.utils.asMixin
import net.minecraft.client.particle.SingleQuadParticle
import java.awt.Color

class Particle(override val mcValue: MCParticle) : CTWrapper<MCParticle> {
    private val mixed: ParticleAccessor = mcValue.asMixin()
    private val quadMixed: SingleQuadParticleAccessor? =
        (mcValue as? SingleQuadParticle)?.asMixin()
    private var fallbackRed = 1f
    private var fallbackGreen = 1f
    private var fallbackBlue = 1f
    private var fallbackAlpha = 1f

    var x by mixed::x
    var y by mixed::y
    var z by mixed::z

    var lastX by mixed::prevPosX
    var lastY by mixed::prevPosY
    var lastZ by mixed::prevPosZ

    val renderX get() = lastX + (x - lastX) * Renderer.partialTicks
    val renderY get() = lastY + (y - lastY) * Renderer.partialTicks
    val renderZ get() = lastZ + (z - lastZ) * Renderer.partialTicks

    var motionX by mixed::velocityX
    var motionY by mixed::velocityY
    var motionZ by mixed::velocityZ

    var red: Float
        get() = quadMixed?.red ?: fallbackRed
        set(value) {
            quadMixed?.red = value
            fallbackRed = value
        }
    var green: Float
        get() = quadMixed?.green ?: fallbackGreen
        set(value) {
            quadMixed?.green = value
            fallbackGreen = value
        }
    var blue: Float
        get() = quadMixed?.blue ?: fallbackBlue
        set(value) {
            quadMixed?.blue = value
            fallbackBlue = value
        }
    var alpha: Float
        get() = quadMixed?.alpha ?: fallbackAlpha
        set(value) {
            quadMixed?.alpha = value
            fallbackAlpha = value
        }

    var age by mixed::age
    var dead by mixed::dead

    fun scale(scale: Float) = apply {
        mcValue.scale(scale)
    }

    /**
     * Sets the color of the particle.
     * @param red the red value between 0 and 1.
     * @param green the green value between 0 and 1.
     * @param blue the blue value between 0 and 1.
     */
    fun setColor(red: Float, green: Float, blue: Float) = apply {
        this.red = red
        this.green = green
        this.blue = blue
    }

    /**
     * Sets the color of the particle.
     * @param red the red value between 0 and 1.
     * @param green the green value between 0 and 1.
     * @param blue the blue value between 0 and 1.
     * @param alpha the alpha value between 0 and 1.
     */
    fun setColor(red: Float, green: Float, blue: Float, alpha: Float) = apply {
        setColor(red, green, blue)
        setAlpha(alpha)
    }

    fun setColor(color: Long) = apply {
        val red = (color shr 16 and 255).toFloat() / 255.0f
        val blue = (color shr 8 and 255).toFloat() / 255.0f
        val green = (color and 255).toFloat() / 255.0f
        val alpha = (color shr 24 and 255).toFloat() / 255.0f

        setColor(red, green, blue, alpha)
    }

    /**
     * Sets the alpha of the particle.
     * @param alpha the alpha value between 0 and 1.
     */
    fun setAlpha(alpha: Float) = apply {
        this.alpha = alpha
    }

    /**
     * Returns the color of the Particle
     *
     * @return A [Color] with the R, G, B and A values
     */
    fun getColor() = Color(red, green, blue, alpha)

    fun setColor(color: Color) = setColor(color.rgb.toLong())

    /**
     * Sets the amount of ticks this particle will live for
     *
     * @param maxAge the particle's max age (in ticks)
     */
    fun setMaxAge(maxAge: Int) = apply {
        mcValue.lifetime = maxAge
    }

    fun remove() = apply {
        mcValue.remove()
    }

    override fun toString() =
        "Particle(type=${mcValue.javaClass.simpleName}, pos=($x, $y, $z), color=[$red, $green, $blue, $alpha], age=$age)"
}
