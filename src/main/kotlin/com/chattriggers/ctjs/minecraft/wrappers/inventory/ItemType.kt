package com.chattriggers.ctjs.minecraft.wrappers.inventory

import com.chattriggers.ctjs.minecraft.objects.TextComponent
import com.chattriggers.ctjs.minecraft.wrappers.CTWrapper
import com.chattriggers.ctjs.minecraft.wrappers.world.block.BlockType
import com.chattriggers.ctjs.utils.MCItem
import com.chattriggers.ctjs.utils.toIdentifier
import net.minecraft.registry.Registries

class ItemType(override val mcValue: MCItem) : CTWrapper<MCItem> {
    constructor(itemName: String) : this(Registries.ITEM[itemName.toIdentifier()])

    constructor(id: Int) : this(Registries.ITEM[id])

    constructor(blockType: BlockType) : this(blockType.toMC().asItem())

    fun getName(): String = getNameComponent().formattedText

    fun getNameComponent(): TextComponent = TextComponent(mcValue.name)

    fun getID(): Int = MCItem.getRawId(mcValue)

    fun getTranslationKey(): String = mcValue.translationKey

    fun getRegistryName(): String = Registries.ITEM.getId(mcValue).toString()
}
