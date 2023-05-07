package com.chattriggers.ctjs.minecraft.wrappers.entity

import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer
import com.chattriggers.ctjs.mixins.ParticleAccessor
import com.chattriggers.ctjs.utils.asMixin
import net.minecraft.client.particle.Particle as MCParticle

class Particle(val particle: MCParticle) {
    private val mixed: ParticleAccessor = particle.asMixin()

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

    var red by mixed::red
    var green by mixed::green
    var blue by mixed::blue
    var alpha by mixed::alpha

    var age by mixed::age
    var dead by mixed::dead

    fun scale(scale: Float) = apply {
        particle.scale(scale)
    }

    // TODO(breaking): Removed multiplyVelocity

    /**
     * Sets the color of the particle.
     * @param red the red value between 0 and 1.
     * @param green the green value between 0 and 1.
     * @param blue the blue value between 0 and 1.
     */
    fun setColor(red: Float, green: Float, blue: Float) = apply {
        particle.setColor(red, green, blue)
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
        mixed.setAlpha(alpha)
    }

    /**
     * Returns the color of the Particle
     *
     * @return A [java.awt.Color] with the R, G, B and A values
     */
    // TODO: Color wrapper
    // fun getColor(): Color = Color(
    //     underlyingEntity.redColorF,
    //     underlyingEntity.greenColorF,
    //     underlyingEntity.blueColorF,
    //     underlyingEntity.alpha
    // )

    /**
     * Sets the amount of ticks this particle will live for
     *
     * @param maxAge the particle's max age (in ticks)
     */
    fun setMaxAge(maxAge: Int) = apply {
        particle.maxAge = maxAge
    }

    fun remove() = apply {
        particle.markDead()
    }

    override fun toString() = "Particle(pos=[$x, $y, $z], color=[$red, $green, $blue, $alpha], age=$age)"
}
