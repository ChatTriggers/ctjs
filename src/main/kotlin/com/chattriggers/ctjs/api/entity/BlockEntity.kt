package com.chattriggers.ctjs.api.entity

import com.chattriggers.ctjs.api.CTWrapper
import com.chattriggers.ctjs.api.world.block.Block
import com.chattriggers.ctjs.api.world.block.BlockPos
import com.chattriggers.ctjs.api.world.block.BlockType
import com.chattriggers.ctjs.MCBlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.core.registries.BuiltInRegistries

class BlockEntity(override val mcValue: MCBlockEntity) : CTWrapper<MCBlockEntity> {

    fun getX(): Int = getBlockPos().x

    fun getY(): Int = getBlockPos().y

    fun getZ(): Int = getBlockPos().z

    fun getBlockType(): BlockType = BlockType(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(mcValue.type).toString())

    fun getBlockPos(): BlockPos = BlockPos(mcValue.blockPos)

    fun getBlock(): Block = Block(getBlockType(), getBlockPos())

    override fun toString(): String {
        return "BlockEntity(type=${getBlockType()}, pos=[${getX()}, ${getY()}, ${getZ()}])"
    }
}
