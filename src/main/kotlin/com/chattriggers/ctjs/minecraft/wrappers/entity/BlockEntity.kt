package com.chattriggers.ctjs.minecraft.wrappers.entity

import com.chattriggers.ctjs.minecraft.wrappers.world.block.BlockPos
import com.chattriggers.ctjs.utils.MCBlockEntity

// TODO(breaking): Renamed from TileEntity to BlockEntity
class BlockEntity(val blockEntity: MCBlockEntity) {
    fun getX(): Int = getBlockPos().x

    fun getY(): Int = getBlockPos().y

    fun getZ(): Int = getBlockPos().z

    fun getBlockPos(): BlockPos = BlockPos(blockEntity.pos)

    override fun toString(): String {
        return "BlockEntity{x=${getX()}, y=${getY()}, z=${getZ()}}"
    }
}
