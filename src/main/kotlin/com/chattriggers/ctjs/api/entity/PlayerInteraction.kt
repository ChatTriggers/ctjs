package com.chattriggers.ctjs.api.entity

import net.minecraft.util.Hand

sealed class PlayerInteraction(val name: String, val mainHand: Boolean) {
    object AttackBlock : PlayerInteraction("AttackBlock", true)
    object AttackEntity : PlayerInteraction("AttackEntity", true)
    object BreakBlock : PlayerInteraction("BreakBlock", true)
    class UseBlock(hand: Hand) : PlayerInteraction("UseBlock", hand == Hand.MAIN_HAND)
    class UseEntity(hand: Hand) : PlayerInteraction("UseEntity", hand == Hand.MAIN_HAND)
    class UseItem(hand: Hand) : PlayerInteraction("UseItem", hand == Hand.MAIN_HAND)

    override fun toString(): String = name
}
