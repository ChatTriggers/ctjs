package com.chattriggers.ctjs.minecraft.wrappers.inventory

import com.chattriggers.ctjs.minecraft.wrappers.world.block.BlockType
import com.chattriggers.ctjs.utils.MCItem
import com.chattriggers.ctjs.utils.toIdentifier
import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.registry.Registries

// TODO(breaking): Completely redid this API
class ItemType(private val item: MCItem) {
    constructor(itemName: String) : this(Registries.ITEM[itemName.toIdentifier()])

    constructor(id: Int) : this(Registries.ITEM[id])

    constructor(blockType: BlockType) : this(blockType.block.asItem())

    fun getName(): String = getNameComponent().unformattedText

    fun getNameComponent(): UTextComponent = UTextComponent(item.name)

    fun getID(): Int = MCItem.getRawId(item)

    fun getTranslationKey(): String = item.translationKey

    fun getRegistryName(): String = Registries.ITEM.getId(item).toString()
}
