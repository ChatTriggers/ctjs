package com.chattriggers.ctjs.api.inventory.action

import net.minecraft.screen.slot.SlotActionType

class DropAction(slot: Int, windowId: Int) : Action(slot, windowId) {
    private var holdingCtrl = false

    fun getHoldingCtrl(): Boolean = holdingCtrl

    /**
     * Whether the click should act as if control is being held (defaults to false)
     *
     * @param holdingCtrl to hold ctrl or not
     */
    fun setHoldingCtrl(holdingCtrl: Boolean) = apply {
        this.holdingCtrl = holdingCtrl
    }

    override fun complete() {
        doClick(if (holdingCtrl) 1 else 0, SlotActionType.THROW)
    }
}
