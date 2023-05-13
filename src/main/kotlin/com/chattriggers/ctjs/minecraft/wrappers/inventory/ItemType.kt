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

    constructor(blockType: BlockType) : this(blockType.mcBlock.asItem())

    fun getName() = getNameComponent().unformattedText

    fun getNameComponent() = UTextComponent(item.name)

    fun getID() = MCItem.getRawId(item)

    fun getTranslationKey() = item.translationKey

    fun getRegistryName() = Registries.ITEM.getId(item)
}
