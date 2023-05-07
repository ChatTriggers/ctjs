package com.chattriggers.ctjs.minecraft.wrappers.world.block

import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.block.Block as MCBlock

/**
 * An immutable wrapper around Minecraft's Block object. Note
 * that this references a block "type", and not an actual block
 * in the world. If a reference to a particular block is needed,
 * use [Block]
 */
class BlockType(val mcBlock: MCBlock) {
    constructor(block: BlockType) : this(block.mcBlock)

    // constructor(blockName: String) : this(MCBlock.getBlockFromName(blockName)!!)
    //
    // constructor(blockID: Int) : this(MCBlock.getBlockById(blockID))
    //
    // constructor(item: Item) : this(MCBlock.getBlockFromItem(item.item))

    /**
     * Returns a [Block] based on this block and the
     * provided BlockPos
     *
     * @param blockPos the block position
     * @return a [Block] object
     */
    fun withBlockPos(blockPos: BlockPos) = Block(this, blockPos)

    // fun getID(): Int = MCBlock.getIdFromBlock(mcBlock)

    /**
     * Gets the block's registry name.
     * Example: minecraft:planks
     *
     * @return the block's registry name
     */
    fun getRegistryName() = mcBlock.translationKey

    /**
     * Gets the block's unlocalized name.
     * Example: tile.wood
     *
     * @return the block's unlocalized name
     */
    // TODO: What is this?
    // fun getUnlocalizedName(): String = mcBlock.unlocalizedName

    /**
     * Gets the block's localized name.
     * Example: Wooden Planks
     *
     * @return the block's localized name
     */
    // TODO(breaking): Return UTextComponent instead of String
    fun getName() = UTextComponent(mcBlock.name)

    // TODO: Rename this method?
    fun getLightValue(): Int {
        return mcBlock.defaultState.luminance
    }

    fun getDefaultState() = mcBlock.defaultState

    // TODO(breaking): Remove getDefaultMetadata and getHarvestLevel

    fun canProvidePower() = getDefaultState().emitsRedstonePower()

    fun isTranslucent() = mcBlock.defaultState.hasSidedTransparency()

    override fun toString(): String = "BlockType(${getRegistryName()})"
}
