package com.chattriggers.ctjs.utils

import com.fasterxml.jackson.core.Version
import net.minecraft.util.Identifier

fun String.toVersion(): Version {
    val split = this.split(".").map(String::toInt)
    return Version(split.getOrNull(0) ?: 0, split.getOrNull(1) ?: 0, split.getOrNull(2) ?: 0, null, null, null)
}

fun String.toIdentifier(): Identifier {
    return Identifier(if (':' in this) this else "minecraft:$this")
}

// A helper function that makes the intent explicit and reduces parens
inline fun <reified T> Any.asMixin() = this as T
