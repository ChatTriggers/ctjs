package com.chattriggers.ctjs.minecraft.wrappers.entity

import com.chattriggers.ctjs.minecraft.wrappers.world.block.Block
import com.chattriggers.ctjs.minecraft.wrappers.world.block.BlockPos
import com.chattriggers.ctjs.minecraft.wrappers.world.block.BlockType
import com.chattriggers.ctjs.utils.MCBlockEntity
import net.minecraft.block.entity.BlockEntityType

// TODO(breaking): Renamed from TileEntity to BlockEntity
class BlockEntity(val blockEntity: MCBlockEntity) {
    fun getX(): Int = getBlockPos().x

    fun getY(): Int = getBlockPos().y

    fun getZ(): Int = getBlockPos().z

    fun getBlockType(): BlockType = BlockType(BlockEntityType.getId(blockEntity.type)!!.toString())

    fun getBlockPos(): BlockPos = BlockPos(blockEntity.pos)

    fun getBlock(): Block = Block(getBlockType(), getBlockPos())

    override fun toString(): String {
        return "BlockEntity{type=${getBlockType()}, pos=(${getX()}, ${getY()}, ${getZ()})}"
    }
}
