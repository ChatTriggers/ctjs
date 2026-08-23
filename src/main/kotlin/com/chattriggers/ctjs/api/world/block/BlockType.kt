package com.chattriggers.ctjs.api.world.block

import com.chattriggers.ctjs.api.CTWrapper
import com.chattriggers.ctjs.api.inventory.Item
import com.chattriggers.ctjs.api.inventory.ItemType
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.MCBlock
import com.chattriggers.ctjs.MCItem
import com.chattriggers.ctjs.internal.utils.toIdentifier
import net.minecraft.core.registries.BuiltInRegistries

/**
 * An immutable wrapper around Minecraft's Block object. Note
 * that this references a block "type", and not an actual block
 * in the world. If a reference to a particular block is needed,
 * use [Block]
 */
class BlockType(override val mcValue: MCBlock) : CTWrapper<MCBlock> {
    constructor(block: BlockType) : this(block.mcValue)

    constructor(blockName: String) : this(
        requireNotNull(BuiltInRegistries.BLOCK.getValue(blockName.toIdentifier())) {
            "Unknown block: $blockName"
        }
    )

    constructor(blockID: Int) : this(ItemType(MCItem.byId(blockID)).getRegistryName())

    constructor(item: Item) : this(MCBlock.byItem(item.mcValue.item))

    /**
     * Returns a [Block] based on this block and the
     * provided BlockPos
     *
     * @param blockPos the block position
     * @return a [Block] object
     */
    fun withBlockPos(blockPos: BlockPos) = Block(this, blockPos)

    fun getID(): Int = BuiltInRegistries.BLOCK.indexOf(mcValue)

    /**
     * Gets the block's registry name.
     * Example: minecraft:oak_planks
     *
     * @return the block's registry name
     */
    fun getRegistryName(): String = BuiltInRegistries.BLOCK.getKey(mcValue).toString()

    /**
     * Gets the block's translation key.
     * Example: block.minecraft.oak_planks
     *
     * @return the block's translation key
     */
    fun getTranslationKey(): String = mcValue.descriptionId

    /**
     * Gets the block's localized name.
     * Example: Wooden Planks
     *
     * @return the block's localized name
     */
    fun getName() = TextComponent(mcValue.name).formattedText

    fun getLightValue(): Int = getDefaultState().lightEmission

    fun getDefaultState() = mcValue.defaultBlockState()

    fun canProvidePower() = getDefaultState().isSignalSource

    fun isTranslucent() = getDefaultState().propagatesSkylightDown()

    override fun toString(): String = "BlockType{${getRegistryName()}}"
}
