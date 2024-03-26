package com.chattriggers.ctjs.internal.utils

import gg.essential.elementa.state.BasicState

class ResettableState<T>(private val originalValue: T) : BasicState<T>(originalValue) {
    val isOriginalValue: Boolean
        get() = get() === originalValue

    fun reset() = set(originalValue)
}
