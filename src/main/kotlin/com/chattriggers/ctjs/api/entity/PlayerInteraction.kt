package com.chattriggers.ctjs.api.entity

import net.minecraft.world.InteractionHand

sealed class PlayerInteraction(val name: String, val mainHand: Boolean) {
    object AttackBlock : PlayerInteraction("AttackBlock", true)
    object AttackEntity : PlayerInteraction("AttackEntity", true)
    object BreakBlock : PlayerInteraction("BreakBlock", true)
    class UseBlock(hand: InteractionHand) : PlayerInteraction("UseBlock", hand == InteractionHand.MAIN_HAND)
    class UseEntity(hand: InteractionHand) : PlayerInteraction("UseEntity", hand == InteractionHand.MAIN_HAND)
    class UseItem(hand: InteractionHand) : PlayerInteraction("UseItem", hand == InteractionHand.MAIN_HAND)

    override fun toString(): String = name
}
