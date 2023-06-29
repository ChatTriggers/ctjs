package com.chattriggers.ctjs.minecraft.wrappers.world

import com.chattriggers.ctjs.minecraft.wrappers.CTWrapper
import com.chattriggers.ctjs.minecraft.wrappers.World
import com.chattriggers.ctjs.mixins.ChunkAccessor
import com.chattriggers.ctjs.minecraft.wrappers.entity.BlockEntity
import com.chattriggers.ctjs.minecraft.wrappers.entity.Entity
import com.chattriggers.ctjs.utils.*
import net.minecraft.util.math.Box

// TODO: Add more methods here?
class Chunk(override val mcValue: MCChunk) : CTWrapper<MCChunk> {
    /**
     * Gets the x position of the chunk
     */
    fun getX() = mcValue.pos.x

    /**
     * Gets the z position of the chunk
     */
    fun getZ() = mcValue.pos.z

    /**
     * Gets the minimum x coordinate of a block in the chunk
     *
     * @return the minimum x coordinate
     */
    fun getMinBlockX() = getX() * 16

    /**
     * Gets the minimum z coordinate of a block in the chunk
     *
     * @return the minimum z coordinate
     */
    fun getMinBlockZ() = getZ() * 16

    /**
     * Gets every entity in this chunk
     *
     * @return the entity list
     */
    fun getAllEntities(): List<Entity> = getAllEntitiesOfType(MCEntity::class.java)

    /**
     * Gets every entity in this chunk of a certain class
     *
     * @param clazz the class to filter for (Use `Java.type().class` to get this)
     * @return the entity list
     */
    fun getAllEntitiesOfType(clazz: Class<MCEntity>): List<Entity> {
        val box = Box(
            MCBlockPos(getMinBlockX(), mcValue.bottomY, getMinBlockZ())
        ).stretch(16.0, mcValue.topY.toDouble(), 16.0)

        return World.toMC()?.getEntitiesByClass(clazz, box) { true }?.map(Entity::fromMC) ?: listOf()
    }

    /**
     * Gets every block entity in this chunk
     *
     * @return the block entity list
     */
    fun getAllBlockEntities(): List<BlockEntity> {
        return mcValue.asMixin<ChunkAccessor>().blockEntities.values.map(::BlockEntity)
    }

    /**
     * Gets every block entity in this chunk of a certain class
     *
     * @param clazz the class to filter for (Use `Java.type().class` to get this)
     * @return the block entity list
     */
    fun getAllBlockEntitiesOfType(clazz: Class<*>): List<BlockEntity> {
        return getAllBlockEntities().filter {
            clazz.isInstance(it.toMC())
        }
    }
}
