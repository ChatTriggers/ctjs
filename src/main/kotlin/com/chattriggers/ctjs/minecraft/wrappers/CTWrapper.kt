package com.chattriggers.ctjs.minecraft.wrappers

interface CTWrapper<MCClass> {
    val mcValue: MCClass

    fun toMC(): MCClass = mcValue
}
