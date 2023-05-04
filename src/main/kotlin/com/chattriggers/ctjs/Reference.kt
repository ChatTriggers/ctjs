package com.chattriggers.ctjs

import com.chattriggers.ctjs.minecraft.libs.renderer.Image

object Reference {
    // TODO: Figure out how to substitute these at build time
    const val MOD_ID = "chattriggers"
    const val MOD_NAME = "ChatTriggers"
    const val MOD_VERSION = "3.0.0"

    const val DEFAULT_MODULES_FOLDER = "./config/ChatTriggers/modules"

    var isLoaded = true

    @JvmStatic
    fun unloadCT() {
        isLoaded = false

        CTJS.images.forEach(Image::destroy)
    }

    @JvmStatic
    fun loadCT() {
        isLoaded = true
    }
}
