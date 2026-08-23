package com.chattriggers.ctjs.api.render

internal object GuiInputArguments {
    fun mouseDragged(
        deltaX: Double,
        deltaY: Double,
        x: Double,
        y: Double,
        button: Int,
    ): Array<Any> = arrayOf(deltaX, deltaY, x, y, button)
}
