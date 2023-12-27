package com.chattriggers.ctjs.api.render

import com.chattriggers.ctjs.api.vec.Vec2f
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class Shape(private var color: Long) {
    private val vertexes = mutableListOf<Vec2f>()
    private val reversedVertexes = vertexes.asReversed()
    private var drawMode = Renderer.DrawMode.QUADS
    private var area = 0f

    fun copy(): Shape = clone()

    fun clone(): Shape {
        val clone = Shape(color)
        clone.vertexes.addAll(vertexes)
        clone.setDrawMode(drawMode)
        return clone
    }

    fun getColor(): Long = color

    fun setColor(color: Long) = apply { this.color = Renderer.fixAlpha(color) }

    fun getDrawMode(): Renderer.DrawMode = drawMode

    /**
     * Sets the GL draw mode of the shape
     */
    fun setDrawMode(drawMode: Renderer.DrawMode) = apply { this.drawMode = drawMode }

    fun getVertexes(): List<Vec2f> = vertexes

    fun addVertex(x: Float, y: Float) = apply {
        vertexes.add(Vec2f(x, y))
        updateArea()
    }

    fun insertVertex(index: Int, x: Float, y: Float) = apply {
        vertexes.add(index, Vec2f(x, y))
        updateArea()
    }

    fun removeVertex(index: Int) = apply {
        vertexes.removeAt(index)
        updateArea()
    }

    fun clearVertices() = apply {
        vertexes.clear()
        area = 0f
    }

    /**
     * Sets the shape as a line pointing from [x1, y1] to [x2, y2] with a thickness
     */
    fun setLine(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float) = apply {
        vertexes.clear()

        val theta = -atan2(y2 - y1, x2 - x1)
        val i = sin(theta) * (thickness / 2)
        val j = cos(theta) * (thickness / 2)

        addVertex(x1 + i, y1 + j)
        addVertex(x2 + i, y2 + j)
        addVertex(x2 - i, y2 - j)
        addVertex(x1 - i, y1 - j)

        drawMode = Renderer.DrawMode.QUADS
    }

    /**
     * Sets the shape as a circle with a center at [x, y]
     * with radius and number of steps around the circle
     */
    fun setCircle(x: Float, y: Float, radius: Float, steps: Int) = apply {
        vertexes.clear()

        val theta = 2 * PI / steps
        val cos = cos(theta).toFloat()
        val sin = sin(theta).toFloat()

        var xHolder: Float
        var circleX = 1f
        var circleY = 0f

        for (i in 0..steps) {
            addVertex(x, y)
            addVertex(circleX * radius + x, circleY * radius + y)
            xHolder = circleX
            circleX = cos * circleX - sin * circleY
            circleY = sin * xHolder + cos * circleY
            addVertex(circleX * radius + x, circleY * radius + y)
        }

        drawMode = Renderer.DrawMode.TRIANGLE_STRIP
    }

    fun draw() = apply {
        Renderer.apply {
            begin(drawMode, Renderer.VertexFormat.POSITION_COLOR)

            if (area < 0) {
                vertexes.forEach { pos(it.x, it.y).color(color) }
            } else {
                reversedVertexes.forEach { pos(it.x, it.y).color(color) }
            }

            draw()
        }
    }

    private fun updateArea() {
        area = 0f

        for (i in vertexes.indices) {
            val p1 = vertexes[i]
            val p2 = vertexes[(i + 1) % vertexes.size]

            area += p1.x * p2.y - p2.x * p1.y
        }

        area /= 2
    }
}
