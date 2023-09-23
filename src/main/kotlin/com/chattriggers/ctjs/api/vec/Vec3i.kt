package com.chattriggers.ctjs.api.vec

import java.util.*
import kotlin.math.acos
import kotlin.math.sqrt

open class Vec3i @JvmOverloads constructor(
    val x: Int = 0, val y: Int = 0, val z: Int = 0,
) {
    fun magnitudeSquared() = x * x + y * y + z * z

    fun magnitude() = sqrt(magnitudeSquared().toFloat())

    open fun translated(dx: Int, dy: Int, dz: Int) = Vec3i(x + dx, y + dy, z + dz)

    open fun scaled(scale: Int) = Vec3i(x * scale, y * scale, z * scale)

    open fun scaled(xScale: Int, yScale: Int, zScale: Int) = Vec3i(x * xScale, y * yScale, z * zScale)

    open fun crossProduct(other: Vec3i) = Vec3i(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )

    fun dotProduct(other: Vec3i) = x * other.x + y * other.y + z * other.z

    fun angleTo(other: Vec3i): Float {
        return acos(dotProduct(other) / (magnitude() * other.magnitude()).coerceIn(-1f, 1f))
    }

    fun normalized() = magnitude().let {
        Vec3f(x / it, y / it, z / it)
    }

    open operator fun unaryMinus() = Vec3i(-x, -y, -z)

    open operator fun plus(other: Vec3i) = Vec3i(x + other.x, y + other.y, z + other.z)

    open operator fun minus(other: Vec3i) = this + (-other)

    override fun hashCode() = Objects.hash(x, y, z)

    override fun equals(other: Any?) = other is Vec3i && x == other.x && y == other.y && z == other.z

    override fun toString() = "Vec3i($x, $y, $z)"
}
