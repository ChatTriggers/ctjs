package com.chattriggers.ctjs.api.vec

import kotlin.math.acos
import kotlin.math.sqrt

data class Vec3f @JvmOverloads constructor(
    val x: Float = 0f, val y: Float = 0f, val z: Float = 0f,
) {
    fun magnitudeSquared() = x * x + y * y + z * z

    fun magnitude() = sqrt(magnitudeSquared())

    fun translated(dx: Float, dy: Float, dz: Float) = Vec3f(x + dx, y + dy, z + dz)

    fun scaled(scale: Float) = Vec3f(x * scale, y * scale, z * scale)

    fun scaled(xScale: Float, yScale: Float, zScale: Float) = Vec3f(x * xScale, y * yScale, z * zScale)

    fun crossProduct(other: Vec3f) = Vec3f(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )

    fun dotProduct(other: Vec3f) = x * other.x + y * other.y + z * other.z

    fun angleTo(other: Vec3f): Float {
        return acos(dotProduct(other) / (magnitude() * other.magnitude()).coerceIn(-1f, 1f))
    }

    fun normalized() = magnitude().let {
        Vec3f(x / it, y / it, z / it)
    }

    operator fun unaryMinus() = Vec3f(-x, -y, -z)

    operator fun plus(other: Vec3f) = Vec3f(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vec3f) = this + (-other)

    override fun toString() = "Vec3f($x, $y, $z)"
}
