package com.chattriggers.ctjs.api

interface CTWrapper<MCClass> {
    val mcValue: MCClass

    fun toMC(): MCClass = mcValue
}
