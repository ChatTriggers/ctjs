package com.chattriggers.ctjs.minecraft.wrappers.world

import com.chattriggers.ctjs.utils.MCChunk
import gg.essential.universal.UMinecraft
import net.minecraft.util.math.BlockPos
import net.minecraft.world.LightType

// TODO: Add more methods here?
class Chunk(val chunk: MCChunk) {
    /**
     * Gets the x position of the chunk
     */
    fun getX() = chunk.pos.x

    /**
     * Gets the z position of the chunk
     */
    fun getZ() = chunk.pos.z

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
     * Gets the skylight level at the given position. This is the value seen in the debug (F3) menu
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return the skylight level at the location
     */
    // TODO: Move this method somewhere else
    fun getSkyLightLevel(x: Int, y: Int, z: Int): Int {
        return UMinecraft.getMinecraft().world?.getLightLevel(LightType.SKY, BlockPos(x, y, z)) ?: 0
    }

    /**
     * Gets the block light level at the given position. This is the value seen in the debug (F3) menu
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return the block light level at the location
     */
    // TODO: Move this method somewhere else
    fun getBlockLightLevel(x: Int, y: Int, z: Int): Int {
        return UMinecraft.getMinecraft().world?.getLightLevel(LightType.BLOCK, BlockPos(x, y, z)) ?: 0
    }

    // TODO:
    // /**
    //  * Gets every entity in this chunk
    //  *
    //  * @return the entity list
    //  */
    // fun getAllEntities(): List<Entity> {
    //     return chunk.entityLists.toList().flatten().map(::Entity)
    // }
    //
    // /**
    //  * Gets every entity in this chunk of a certain class
    //  *
    //  * @param clazz the class to filter for (Use `Java.type().class` to get this)
    //  * @return the entity list
    //  */
    // fun getAllEntitiesOfType(clazz: Class<*>): List<Entity> {
    //     return getAllEntities().filter {
    //         clazz.isInstance(it.entity)
    //     }
    // }
    //
    // /**
    //  * Gets every tile entity in this chunk
    //  *
    //  * @return the tile entity list
    //  */
    // fun getAllTileEntities(): List<TileEntity> {
    //     return chunk.tileEntityMap.values.map(::TileEntity)
    // }
    //
    // /**
    //  * Gets every tile entity in this chunk of a certain class
    //  *
    //  * @param clazz the class to filter for (Use `Java.type().class` to get this)
    //  * @return the tile entity list
    //  */
    // fun getAllTileEntitiesOfType(clazz: Class<*>): List<TileEntity> {
    //     return getAllTileEntities().filter {
    //         clazz.isInstance(it.tileEntity)
    //     }
    // }
}
