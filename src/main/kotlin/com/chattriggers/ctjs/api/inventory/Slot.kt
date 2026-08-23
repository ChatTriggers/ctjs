package com.chattriggers.ctjs.api.inventory

import com.chattriggers.ctjs.api.CTWrapper
import com.chattriggers.ctjs.MCSlot

class Slot(override val mcValue: MCSlot) : CTWrapper<MCSlot> {
    val index by mcValue::index

    val displayX by mcValue::x

    val displayY by mcValue::y

    val inventory get() = Inventory(mcValue.container)

    val item get(): Item? = Item.fromMC(mcValue.item)

    val isEnabled get() = mcValue.isActive

    override fun toString() = "Slot(inventory=$inventory, index=$index, item=$item)"
}
