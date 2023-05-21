package com.chattriggers.ctjs.utils

import com.fasterxml.jackson.core.Version
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import org.mozilla.javascript.NativeObject

fun String.toVersion(): Version {
    val split = this.split(".").map(String::toInt)
    return Version(split.getOrNull(0) ?: 0, split.getOrNull(1) ?: 0, split.getOrNull(2) ?: 0, null, null, null)
}

fun String.toIdentifier(): Identifier {
    return Identifier(if (':' in this) this else "minecraft:$this")
}

// A helper function that makes the intent explicit and reduces parens
inline fun <reified T> Any.asMixin() = this as T

fun NativeObject?.getOption(key: String, default: Any): String {
    return (this?.get(key) ?: default).toString()
}

// Note: getOrDefault<Number>(...).toInt/Double/Float() should be preferred
//       over getOrDefault<Int/Double/Float>(...), as the exact numeric type
//       of numeric properties depends on Rhino internals
inline fun <reified T> NativeObject?.getOrDefault(key: String, default: T): T {
    return this?.get(key) as? T ?: default
}

fun Double.toRadians() = this * MathHelper.RADIANS_PER_DEGREE
fun Float.toRadians() = this * MathHelper.RADIANS_PER_DEGREE
fun Double.toDegrees() = this * MathHelper.DEGREES_PER_RADIAN
fun Float.toDegrees() = this * MathHelper.DEGREES_PER_RADIAN
