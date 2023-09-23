package com.chattriggers.ctjs.internal.launch

import com.chattriggers.ctjs.engine.MixinCallback

internal data class MixinDetails(
    val injectors: MutableList<MixinCallback> = mutableListOf(),
    val fieldWideners: MutableMap<String, Boolean> = mutableMapOf(),
    val methodWideners: MutableMap<String, Boolean> = mutableMapOf(),
)
