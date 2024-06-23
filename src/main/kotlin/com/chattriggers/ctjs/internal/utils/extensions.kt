package com.chattriggers.ctjs.internal.utils

import com.chattriggers.ctjs.internal.launch.Descriptor
import com.fasterxml.jackson.core.Version
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.Scriptable
import java.net.URLEncoder
import java.nio.charset.Charset
import kotlin.reflect.KClass

fun String.toVersion(): Version {
    val (semvar, extra) = if ('-' in this) {
        split('-')
    } else listOf(this, null)

    val split = semvar!!.split(".").map(String::toInt)
    return Version(split.getOrElse(0) { 0 }, split.getOrElse(1) { 0 }, split.getOrElse(2) { 0 }, extra, null, null)
}

fun String.toIdentifier(): Identifier {
    return Identifier.of(if (':' in this) this else "minecraft:$this")
}

fun String.urlEncode() = URLEncoder.encode(this, Charset.defaultCharset())

// A helper function that makes the intent explicit and reduces parens
inline fun <reified T> Any.asMixin() = this as T

inline fun <reified T> NativeObject?.get(key: String): T? {
    return this?.get(key) as? T
}

fun NativeObject?.getOption(key: String, default: Any): String {
    return (this?.get(key) ?: default).toString()
}

// Note: getOrDefault<Number>(...).toInt/Double/Float() should be preferred
//       over getOrDefault<Int/Double/Float>(...), as the exact numeric type
//       of numeric properties depends on Rhino internals
inline fun <reified T> NativeObject?.getOrDefault(key: String, default: T): T {
    return this?.get(key) as? T ?: default
}

fun NativeObject?.getOrNull(key: String): Any? {
    return this?.get(key).takeIf { it != Scriptable.NOT_FOUND }
}

fun Double.toRadians() = this * MathHelper.RADIANS_PER_DEGREE
fun Float.toRadians() = this * MathHelper.RADIANS_PER_DEGREE
fun Double.toDegrees() = this * MathHelper.DEGREES_PER_RADIAN
fun Float.toDegrees() = this * MathHelper.DEGREES_PER_RADIAN

fun KClass<*>.descriptorString(): String = java.descriptorString()
fun KClass<*>.descriptor() = Descriptor.Object(descriptorString())
