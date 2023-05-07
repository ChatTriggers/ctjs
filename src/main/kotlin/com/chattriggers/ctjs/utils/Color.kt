package com.chattriggers.ctjs.utils

import net.minecraft.util.math.ColorHelper.Argb
import net.minecraft.util.math.MathHelper

data class Color @JvmOverloads constructor(val red: Int, val green: Int, val blue: Int, val alpha: Int = 255) {
    val rgb get() = Argb.getArgb(alpha, red, green, blue)

    val redF get() = red.toFloatColor()
    val greenF get() = green.toFloatColor()
    val blueF get() = blue.toFloatColor()
    val alphaF get() = alpha.toFloatColor()

    init {
        require(red in 0..255) { "Expected red color component to be in [0, 255], found $red" }
        require(green in 0..255) { "Expected green color component to be in [0, 255], found $green" }
        require(blue in 0..255) { "Expected blue color component to be in [0, 255], found $blue" }
        require(alpha in 0..255) { "Expected alpha color component to be in [0, 255], found $alpha" }
    }

    constructor(argb: Int) : this(Argb.getRed(argb), Argb.getGreen(argb), Argb.getBlue(argb), Argb.getAlpha(argb))

    @JvmOverloads
    constructor(r: Float, g: Float, b: Float, a: Float = 1f) : this(r.toIntColor(), g.toIntColor(), b.toInt(), a.toIntColor())

    companion object {
        private fun Float.toIntColor() = MathHelper.map(coerceIn(0f, 1f), 0f, 1f, 0f, 255f).toInt()
        private fun Int.toFloatColor() = MathHelper.map(coerceIn(0, 255).toFloat(), 0f, 255f, 0f, 1f)
    }
}
