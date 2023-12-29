package com.chattriggers.ctjs.api.world.block

import com.chattriggers.ctjs.api.client.Player
import com.chattriggers.ctjs.api.inventory.Item
import com.chattriggers.ctjs.api.world.World

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

    fun getState() = World.toMC()?.getBlockState(pos.toMC())

    @JvmOverloads
    fun isEmittingPower(face: BlockFace? = null): Boolean {
        if (face != null)
            return World.toMC()!!.isEmittingRedstonePower(pos.toMC(), face.toMC())
        return BlockFace.entries.any { isEmittingPower(it) }
    }

    @JvmOverloads
    fun getEmittingPower(face: BlockFace? = null): Int {
        if (face != null)
            return World.toMC()!!.getEmittedRedstonePower(pos.toMC(), face.toMC())
        return BlockFace.entries.asSequence().map(::getEmittingPower).firstOrNull { it != 0 } ?: 0
    }

    fun isReceivingPower() = World.toMC()!!.isReceivingRedstonePower(pos.toMC())

    fun getReceivingPower() = World.toMC()!!.getReceivedRedstonePower(pos.toMC())

    /**
     * Checks whether the block can be mined with the tool in the player's hand
     *
     * @return whether the block can be mined
     */
    fun canBeHarvested(): Boolean = Player.getHeldItem()?.let(::canBeHarvestedWith) ?: false

    fun canBeHarvestedWith(item: Item): Boolean = item.canHarvest(this)

    override fun toString() = "Block{type=$type, pos=($x, $y, $z), face=$face}"
}
