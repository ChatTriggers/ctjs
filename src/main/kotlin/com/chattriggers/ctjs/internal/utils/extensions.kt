package com.chattriggers.ctjs.internal.utils

import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.Scriptable
import tools.jackson.core.Version
import java.net.URLEncoder
import java.nio.charset.Charset

fun String.toVersion(): Version {
    val (semvar, extra) = if ('-' in this) {
        split('-')
    } else listOf(this, null)

    val split = semvar!!.split(".").map(String::toInt)
    return Version(split.getOrElse(0) { 0 }, split.getOrElse(1) { 0 }, split.getOrElse(2) { 0 }, extra, null, null)
}

fun String.toIdentifier(): Identifier {
    return Identifier.parse(if (':' in this) this else "minecraft:$this")
}

fun String.urlEncode() = URLEncoder.encode(this, Charset.defaultCharset())

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

fun Double.toRadians() = this * Mth.RAD_TO_DEG
fun Float.toRadians() = this * Mth.RAD_TO_DEG
fun Double.toDegrees() = this * Mth.DEG_TO_RAD
fun Float.toDegrees() = this * Mth.DEG_TO_RAD

fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.onExecute(block: (CommandContext<S>) -> Unit): T =
    executes {
        block(it)
        1
    }
