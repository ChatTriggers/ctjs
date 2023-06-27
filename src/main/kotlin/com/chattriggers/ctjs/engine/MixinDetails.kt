package com.chattriggers.ctjs.engine

import com.chattriggers.ctjs.launch.MixinCallback

data class MixinDetails(
    val injectors: MutableList<MixinCallback> = mutableListOf(),
    val fieldWideners: MutableMap<String, Boolean> = mutableMapOf(),
    val methodWideners: MutableMap<String, Boolean> = mutableMapOf(),
)
