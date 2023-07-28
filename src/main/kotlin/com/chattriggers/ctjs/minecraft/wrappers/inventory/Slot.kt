package com.chattriggers.ctjs.minecraft.wrappers.inventory

import com.chattriggers.ctjs.minecraft.wrappers.CTWrapper
import com.chattriggers.ctjs.utils.MCSlot

class Slot(override val mcValue: MCSlot) : CTWrapper<MCSlot> {
    val index by mcValue::index

    val displayX by mcValue::x

    val displayY by mcValue::y

    val inventory get() = Inventory(mcValue.inventory)

    val item get(): Item? = Item.fromMC(mcValue.stack)

    val isEnabled get() = mcValue.isEnabled

    override fun toString() = "Slot(inventory=$inventory, index=$index, item=$item)"
}
