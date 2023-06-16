package com.chattriggers.ctjs.minecraft.wrappers.world.block

import com.chattriggers.ctjs.minecraft.wrappers.inventory.Item
import com.chattriggers.ctjs.minecraft.wrappers.inventory.ItemType
import com.chattriggers.ctjs.utils.MCBlock
import com.chattriggers.ctjs.utils.MCItem
import com.chattriggers.ctjs.utils.toIdentifier
import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

// TODO(breaking): rename mcBlock to block to match all other mc class fields inside wrappers
/**
 * An immutable wrapper around Minecraft's Block object. Note
 * that this references a block "type", and not an actual block
 * in the world. If a reference to a particular block is needed,
 * use [Block]
 */
class BlockType(val block: MCBlock) {
    constructor(block: BlockType) : this(block.block)

    constructor(blockName: String) : this(Registries.BLOCK[blockName.toIdentifier()])

    constructor(blockID: Int) : this(ItemType(MCItem.byRawId(blockID)).getRegistryName())

    constructor(item: Item) : this(MCBlock.getBlockFromItem(item.stack.item))

    /**
     * Returns a [Block] based on this block and the
     * provided BlockPos
     *
     * @param blockPos the block position
     * @return a [Block] object
     */
    fun withBlockPos(blockPos: BlockPos) = Block(this, blockPos)

     fun getID(): Int = Registries.BLOCK.indexOf(block)

    /**
     * Gets the block's registry name.
     * Example: minecraft:oak_planks
     *
     * @return the block's registry name
     */
    fun getRegistryName(): String = Registries.BLOCK.getId(block).toString()

    /**
     * Gets the block's translation key.
     * Example: block.minecraft.oak_planks
     *
     * @return the block's translation key
     */
     fun getTranslationKey(): String = block.translationKey

    /**
     * Gets the block's localized name.
     * Example: Wooden Planks
     *
     * @return the block's localized name
     */
    fun getName() = UTextComponent(block.name).formattedText

    // TODO: Rename this method?
    fun getLightValue(): Int = block.defaultState.luminance

    fun getDefaultState() = block.defaultState

    // TODO(breaking): Remove getDefaultMetadata and getHarvestLevel

    fun canProvidePower() = getDefaultState().emitsRedstonePower()

    fun isTranslucent() = block.defaultState.hasSidedTransparency()

    override fun toString(): String = "BlockType{${getRegistryName()}}"
}
