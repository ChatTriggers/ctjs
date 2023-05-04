package com.chattriggers.ctjs.utils

import com.chattriggers.ctjs.engine.module.ModuleUpdater

internal interface Initializer {
    fun init()

    companion object {
        internal val initializers = listOf(ModuleUpdater)
    }
}
