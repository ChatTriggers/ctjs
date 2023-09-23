package com.chattriggers.ctjs.api.vec

import kotlin.math.acos
import kotlin.math.sqrt

data class Vec2f @JvmOverloads constructor(val x: Float = 0f, val y: Float = 0f) {
    fun magnitudeSquared() = x * x + y * y

    fun magnitude() = sqrt(magnitudeSquared())

    fun translated(dx: Float, dy: Float) = Vec2f(x + dx, y + dy)

    fun scaled(scale: Float) = Vec2f(x * scale, y * scale)

    fun scaled(xScale: Float, yScale: Float) = Vec2f(x * xScale, y * yScale)

    fun dotProduct(other: Vec2f) = x * other.x + y * other.y

    fun angleTo(other: Vec2f): Float {
        return acos(dotProduct(other) / (magnitude() * other.magnitude()).coerceIn(-1f, 1f))
    }

    fun normalized() = magnitude().let {
        Vec2f(x / it, y / it)
    }

    operator fun unaryMinus() = Vec2f(-x, -y)

    operator fun plus(other: Vec2f) = Vec2f(x + other.x, y + other.y)

    operator fun minus(other: Vec2f) = this + (-other)

    override fun toString() = "Vec2f($x, $y)"
}
