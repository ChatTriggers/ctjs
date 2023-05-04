package com.chattriggers.ctjs

object Reference {
    // TODO: Figure out how to substitute these at build time
    const val MOD_ID = "chattriggers"
    const val MOD_NAME = "ChatTriggers"
    const val MOD_VERSION = "3.0.0"

    var isLoaded = true

    @JvmStatic
    fun unloadCT() {
        isLoaded = false
    }

    @JvmStatic
    fun loadCT() {
        isLoaded = true
    }
}
