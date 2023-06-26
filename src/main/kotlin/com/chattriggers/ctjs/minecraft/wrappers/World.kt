package com.chattriggers.ctjs.minecraft.wrappers

import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer
import com.chattriggers.ctjs.minecraft.wrappers.entity.BlockEntity
import com.chattriggers.ctjs.minecraft.wrappers.entity.Entity
import com.chattriggers.ctjs.minecraft.wrappers.entity.Particle
import com.chattriggers.ctjs.minecraft.wrappers.entity.PlayerMP
import com.chattriggers.ctjs.minecraft.wrappers.world.Chunk
import com.chattriggers.ctjs.minecraft.wrappers.world.block.Block
import com.chattriggers.ctjs.minecraft.wrappers.world.block.BlockPos
import com.chattriggers.ctjs.minecraft.wrappers.world.block.BlockType
import com.chattriggers.ctjs.mixins.ClientChunkManagerAccessor
import com.chattriggers.ctjs.mixins.ClientChunkMapAccessor
import com.chattriggers.ctjs.mixins.ClientWorldAccessor
import com.chattriggers.ctjs.utils.*
import com.mojang.brigadier.StringReader
import gg.essential.universal.UMinecraft
import net.minecraft.block.BlockState
import net.minecraft.client.world.ClientWorld
import net.minecraft.particle.ParticleEffect
import net.minecraft.particle.ParticleType
import net.minecraft.registry.Registries
import net.minecraft.world.LightType

object World : CTWrapper<ClientWorld?> {
    override val mcValue get() = UMinecraft.getMinecraft().world

    val spawn = SpawnWrapper()
    val particle = ParticleWrapper()

    /**
     * Gets Minecraft's [ClientWorld] object
     *
     * @return The Minecraft [ClientWorld] object
     */
    @Deprecated("Use toMC", ReplaceWith("toMC()"))
    @JvmStatic
    fun getWorld(): ClientWorld? = toMC()

    @JvmStatic
    fun isLoaded(): Boolean = toMC() != null

    @JvmStatic
    fun isRaining(): Boolean = toMC()?.isRaining ?: false

    @JvmStatic
    fun getRainingStrength(): Float = toMC()?.getRainGradient(Renderer.partialTicks) ?: -1f

    @JvmStatic
    fun getTime(): Long = toMC()?.time ?: -1L

    @JvmStatic
    fun getDifficulty(): Settings.Difficulty? = toMC()?.difficulty?.let(Settings.Difficulty::fromMC)

    @JvmStatic
    fun getMoonPhase(): Int = toMC()?.moonPhase ?: -1

    /**
     * Gets the [Block] at a location in the world.
     *
     * @param x the x position
     * @param y the y position
     * @param z the z position
     * @return the [Block] at the location
     */
    @JvmStatic
    fun getBlockAt(x: Number, y: Number, z: Number) = getBlockAt(BlockPos(x, y, z))

    /**
     * Gets the [Block] at a location in the world.
     *
     * @param pos The block position
     * @return the [Block] at the location
     */
    @JvmStatic
    fun getBlockAt(pos: BlockPos): Block {
        return Block(BlockType(getBlockStateAt(pos).block), pos)
    }

    /**
     * Gets the [BlockState] at a location in the world.
     *
     * @param pos The block position
     * @return the [BlockState] at the location
     */
    @JvmStatic
    fun getBlockStateAt(pos: BlockPos): BlockState {
        return toMC()!!.getBlockState(pos.toMC())
    }

    /**
     * Gets the skylight level at the given position. This is the value seen in the debug (F3) menu
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return the skylight level at the location
     */
    fun getSkyLightLevel(x: Int, y: Int, z: Int): Int = getSkyLightLevel(BlockPos(x, y, z))

    /**
     * Gets the skylight level at the given position. This is the value seen in the debug (F3) menu
     *
     * @param pos The block position
     * @return the skylight level at the location
     */
    fun getSkyLightLevel(pos: BlockPos): Int {
        return toMC()?.getLightLevel(LightType.SKY, pos.toMC()) ?: 0
    }

    /**
     * Gets the block light level at the given position. This is the value seen in the debug (F3) menu
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return the block light level at the location
     */
    fun getBlockLightLevel(x: Int, y: Int, z: Int): Int = getBlockLightLevel(BlockPos(x, y, z))

    /**
     * Gets the block light level at the given position. This is the value seen in the debug (F3) menu
     *
     * @param pos The block position
     * @return the block light level at the location
     */
    fun getBlockLightLevel(pos: BlockPos): Int {
        return toMC()?.getLightLevel(LightType.BLOCK, pos.toMC()) ?: 0
    }

    /**
     * Gets all of the players in the world, and returns their wrapped versions.
     *
     * @return the players
     */
    @JvmStatic
    fun getAllPlayers(): List<PlayerMP> = toMC()?.players?.map(::PlayerMP) ?: listOf()

    /**
     * Gets a player by their username, must be in the currently loaded chunks!
     *
     * @param name the username
     * @return the player with said username, or null if they don't exist.
     */
    @JvmStatic
    fun getPlayerByName(name: String) = getAllPlayers().firstOrNull { it.getName() == name }

    @JvmStatic
    fun hasPlayer(name: String) = getPlayerByName(name) != null

    @JvmStatic
    fun getChunk(x: Int, y: Int, z: Int) = Chunk(toMC()!!.getWorldChunk(MCBlockPos(x, y, z)))

    @JvmStatic
    fun getAllEntities() = toMC()?.entities?.map(Entity::fromMC) ?: listOf()

    /**
     * Gets every entity loaded in the world of a certain class
     *
     * @param clazz the class to filter for (Use `Java.type().class` to get this)
     * @return the entity list
     */
    @JvmStatic
    fun getAllEntitiesOfType(clazz: Class<*>): List<Entity> {
        return getAllEntities().filter {
            clazz.isInstance(it.toMC())
        }
    }

    @JvmStatic
    fun getAllBlockEntities(): List<BlockEntity> {
        val chunks = toMC()
            ?.asMixin<ClientWorldAccessor>()
            ?.chunkManager?.asMixin<ClientChunkManagerAccessor>()
            ?.chunks?.asMixin<ClientChunkMapAccessor>()
            ?.chunks ?: return emptyList()

        val blockEntities = mutableListOf<BlockEntity>()

        for (i in 0 until chunks.length()) {
            blockEntities += Chunk(chunks.getPlain(i) ?: continue).getAllBlockEntities()
        }

        return blockEntities
    }

    @JvmStatic
    fun getAllBlockEntitiesOfType(clazz: Class<*>): List<BlockEntity> {
        return getAllBlockEntities().filter {
            clazz.isInstance(it.toMC())
        }
    }

    /**
     * World border object to get border parameters
     */
    object border {
        /**
         * Gets the border center x location.
         *
         * @return the border center x location
         */
        @JvmStatic
        fun getCenterX(): Double = toMC()!!.worldBorder.centerX

        /**
         * Gets the border center z location.
         *
         * @return the border center z location
         */
        @JvmStatic
        fun getCenterZ(): Double = toMC()!!.worldBorder.centerZ

        /**
         * Gets the border size.
         *
         * @return the border size
         */
        @JvmStatic
        fun getSize(): Double = toMC()!!.worldBorder.size

        /**
         * Gets the border target size.
         *
         * @return the border target size
         */
        @JvmStatic
        fun getTargetSize(): Double = toMC()!!.worldBorder.sizeLerpTarget

        /**
         * Gets the border time until the target size is met.
         *
         * @return the border time until target
         */
        @JvmStatic
        fun getTimeUntilTarget(): Long = toMC()!!.worldBorder.sizeLerpTime
    }

    /**
     * World spawn object for getting spawn location.
     */
    class SpawnWrapper {
        /**
         * Gets the spawn x location.
         *
         * @return the spawn x location.
         */
        fun getX(): Int = toMC()!!.spawnPos.x

        /**
         * Gets the spawn y location.
         *
         * @return the spawn y location.
         */
        fun getY(): Int = toMC()!!.spawnPos.y

        /**
         * Gets the spawn z location.
         *
         * @return the spawn z location.
         */
        fun getZ(): Int = toMC()!!.spawnPos.z
    }

    class ParticleWrapper {
        /**
         * Gets an array of all the different particle names you can pass
         * to [spawnParticle]
         *
         * @return the array of name strings
         */
        fun getParticleNames(): List<String> = Registries.PARTICLE_TYPE.keys.map { it.value.path }.toList()

        /**
         * Spawns a particle into the world with the given attributes,
         * which can be configured further with the returned [com.chattriggers.ctjs.minecraft.wrappers.entity.Particle]
         *
         * @param particle the name of the particle to spawn, see [getParticleNames]
         * @param x the x coordinate to spawn the particle at
         * @param y the y coordinate to spawn the particle at
         * @param z the z coordinate to spawn the particle at
         * @param xSpeed the motion the particle should have in the x direction
         * @param ySpeed the motion the particle should have in the y direction
         * @param zSpeed the motion the particle should have in the z direction
         * @return the newly spawned particle for further configuration
         */
        fun spawnParticle(
            particle: String,
            x: Double,
            y: Double,
            z: Double,
            xSpeed: Double,
            ySpeed: Double,
            zSpeed: Double,
        ): Particle {
            @Suppress("UNCHECKED_CAST")
            val particleType = Registries.PARTICLE_TYPE.get(particle.toIdentifier()) as? ParticleType<ParticleEffect>

            requireNotNull(particleType) {
                "Invalid particle parameter"
            }

            val fx = Client.getMinecraft().particleManager.addParticle(
                particleType.parametersFactory.read(particleType, StringReader(particle)),
                x,
                y,
                z,
                xSpeed,
                ySpeed,
                zSpeed
            )!!

            return Particle(fx)
        }

        fun spawnParticle(particle: MCParticle): Particle {
            Client.getMinecraft().particleManager.addParticle(particle)
            return Particle(particle)
        }
    }
}
