package com.chattriggers.ctjs.internal.utils

import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import org.mozilla.javascript.NativeObject

inline fun <reified T> NativeObject?.get(key: String): T? {
    return this?.get(key) as? T
}

fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.onExecute(block: (CommandContext<S>) -> Unit): T =
    executes {
        block(it)
        1
    }
