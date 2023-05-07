package com.chattriggers.ctjs.minecraft.wrappers.world.block

import com.chattriggers.ctjs.minecraft.wrappers.Player
import com.chattriggers.ctjs.minecraft.wrappers.World

/**
 * An immutable reference to a placed block in the world. It
 * has a block type, a position, and optionally a specific face.
 */
open class Block(
    val type: BlockType,
    val pos: BlockPos,
    val face: BlockFace? = null,
) {
    val x: Int get() = pos.x
    val y: Int get() = pos.y
    val z: Int get() = pos.z

    fun withType(type: BlockType) = Block(type, pos, face)

    fun withPos(pos: BlockPos) = Block(type, pos, face)

    /**
     * Narrows this block to reference a certain face. Used by
     * [Player.lookingAt] to specify the block face
     * being looked at.
     */
    fun withFace(face: BlockFace) = Block(type, pos, face)

    fun getState() = World.getWorld()?.getBlockState(pos.toMC())

    // TODO(breaking): remove getMetadata

    @JvmOverloads
    fun isEmittingPower(face: BlockFace? = null): Boolean {
        if (face != null)
            return World.getWorld()!!.isEmittingRedstonePower(pos.toMC(), face.toMC())
        return BlockFace.values().any { isEmittingPower(it) }
    }

    // TODO(breaking): Renamed this method from isPowered
    fun isReceivingPower() = World.getWorld()!!.isReceivingRedstonePower(pos.toMC())

    // TODO(breaking): Renamed this method from getRedstoneStrength
    fun getReceivingPower() = World.getWorld()!!.getReceivedRedstonePower(pos.toMC())

    @JvmOverloads
    fun getEmittingPower(face: BlockFace? = null): Int {
        if (face != null)
            return World.getWorld()!!.getEmittedRedstonePower(pos.toMC(), face.toMC())
        return BlockFace.values().asSequence().map(::getEmittingPower).firstOrNull { it != 0 } ?: 0
    }

    // TODO:
    /**
     * Checks whether the block can be mined with the tool in the player's hand
     *
     * @return whether the block can be mined
     */
    // fun canBeHarvested(): Boolean = type.mcBlock.canHarvestBlock(World.getWorld(), pos.toMCBlock(), Player.getPlayer())

    // fun canBeHarvestedWith(item: Item): Boolean = item.canHarvest(type)

    override fun toString() = "Block(type=$type, pos=($x, $y, $z), face=$face}"
}
