package com.chattriggers.ctjs.api.inventory

import com.chattriggers.ctjs.api.CTWrapper
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.api.world.block.BlockType
import com.chattriggers.ctjs.MCItem
import com.chattriggers.ctjs.internal.utils.toIdentifier
import net.minecraft.item.Items
import net.minecraft.registry.Registries

class ItemType(override val mcValue: MCItem) : CTWrapper<MCItem> {
    init {
        require(mcValue !== Items.AIR) {
            "Can not wrap air as an ItemType"
        }
    }

    constructor(itemName: String) : this(Registries.ITEM[itemName.toIdentifier()])

    constructor(id: Int) : this(Registries.ITEM[id])

    constructor(blockType: BlockType) : this(blockType.toMC().asItem())

    fun getName(): String = getNameComponent().formattedText

    fun getNameComponent(): TextComponent = TextComponent(mcValue.name)

    fun getId(): Int = MCItem.getRawId(mcValue)

    fun getTranslationKey(): String = mcValue.translationKey

    fun getRegistryName(): String = Registries.ITEM.getId(mcValue).toString()

    fun asItem(): Item = Item(this)

    companion object {
        @JvmStatic
        fun fromMC(mcValue: MCItem): ItemType? {
            return if (mcValue === Items.AIR) {
                null
            } else {
                ItemType(mcValue)
            }
        }
    }
}
