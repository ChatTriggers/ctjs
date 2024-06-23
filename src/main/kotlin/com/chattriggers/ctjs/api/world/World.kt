package com.chattriggers.ctjs.api.world

import com.chattriggers.ctjs.MCBlockPos
import com.chattriggers.ctjs.MCParticle
import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.client.Settings
import com.chattriggers.ctjs.api.entity.BlockEntity
import com.chattriggers.ctjs.api.entity.Entity
import com.chattriggers.ctjs.api.entity.Particle
import com.chattriggers.ctjs.api.entity.PlayerMP
import com.chattriggers.ctjs.api.render.Renderer
import com.chattriggers.ctjs.api.world.block.Block
import com.chattriggers.ctjs.api.world.block.BlockPos
import com.chattriggers.ctjs.api.world.block.BlockType
import com.chattriggers.ctjs.internal.mixins.ClientChunkManagerAccessor
import com.chattriggers.ctjs.internal.mixins.ClientChunkMapAccessor
import com.chattriggers.ctjs.internal.mixins.ClientWorldAccessor
import com.chattriggers.ctjs.internal.utils.asMixin
import com.chattriggers.ctjs.internal.utils.toIdentifier
import gg.essential.universal.UMinecraft
import net.minecraft.block.BlockState
import net.minecraft.client.world.ClientWorld
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.particle.BlockStateParticleEffect
import net.minecraft.particle.DustColorTransitionParticleEffect
import net.minecraft.particle.DustParticleEffect
import net.minecraft.particle.EntityEffectParticleEffect
import net.minecraft.particle.ItemStackParticleEffect
import net.minecraft.particle.ParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.particle.SculkChargeParticleEffect
import net.minecraft.particle.ShriekParticleEffect
import net.minecraft.particle.VibrationParticleEffect
import net.minecraft.registry.Registries
import net.minecraft.world.LightType
import net.minecraft.world.event.BlockPositionSource
import kotlin.math.roundToInt

object World {
    @JvmStatic
    fun toMC() = UMinecraft.getMinecraft().world

    @JvmField
    val spawn = SpawnWrapper()

    @JvmField
    val particle = ParticleWrapper()

    @JvmField
    val border = BorderWrapper()

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
    @JvmStatic
    fun getSkyLightLevel(x: Int, y: Int, z: Int): Int = getSkyLightLevel(BlockPos(x, y, z))

    /**
     * Gets the skylight level at the given position. This is the value seen in the debug (F3) menu
     *
     * @param pos The block position
     * @return the skylight level at the location
     */
    @JvmStatic
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
    @JvmStatic
    fun getBlockLightLevel(x: Int, y: Int, z: Int): Int = getBlockLightLevel(BlockPos(x, y, z))

    /**
     * Gets the block light level at the given position. This is the value seen in the debug (F3) menu
     *
     * @param pos The block position
     * @return the block light level at the location
     */
    @JvmStatic
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
     * Returns the TPS of the current world.
     *
     * On modern version (1.20.3+), this is variable. On earlier versions,
     * it is always 20.
     */
    @JvmStatic
    fun getTicksPerSecond(): Int {
        val mpt = toMC()?.tickManager?.millisPerTick ?: return 20
        return (1000.0 / mpt).roundToInt()
    }

    /**
     * World border object to get border parameters
     */
    class BorderWrapper {
        /**
         * Gets the border center x location.
         *
         * @return the border center x location
         */
        fun getCenterX(): Double = toMC()!!.worldBorder.centerX

        /**
         * Gets the border center z location.
         *
         * @return the border center z location
         */
        fun getCenterZ(): Double = toMC()!!.worldBorder.centerZ

        /**
         * Gets the border size.
         *
         * @return the border size
         */
        fun getSize(): Double = toMC()!!.worldBorder.size

        /**
         * Gets the border target size.
         *
         * @return the border target size
         */
        fun getTargetSize(): Double = toMC()!!.worldBorder.sizeLerpTarget

        /**
         * Gets the border time until the target size is met.
         *
         * @return the border time until target
         */
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
         * which can be configured further with the returned [com.chattriggers.ctjs.api.entity.Particle]
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
        ): Particle? {
            val particleType = Registries.PARTICLE_TYPE.get(particle.toIdentifier())

            requireNotNull(particleType) {
                "Invalid particle parameter"
            }

            val effect = if (particleType is ParticleEffect) {
                particleType
            } else {
                val blockPos = BlockPos(x, y, z)
                val blockState = getBlockStateAt(blockPos)

                when (particleType) {
                    ParticleTypes.BLOCK -> BlockStateParticleEffect(ParticleTypes.BLOCK, blockState)
                    ParticleTypes.BLOCK_MARKER -> BlockStateParticleEffect(ParticleTypes.BLOCK_MARKER, blockState)
                    ParticleTypes.DUST -> DustParticleEffect.DEFAULT
                    ParticleTypes.DUST_COLOR_TRANSITION -> DustColorTransitionParticleEffect.DEFAULT
                    ParticleTypes.DUST_PILLAR -> BlockStateParticleEffect(ParticleTypes.DUST_PILLAR, blockState)
                    ParticleTypes.ENTITY_EFFECT -> EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, 1f, 0f, 0f)
                    ParticleTypes.FALLING_DUST -> BlockStateParticleEffect(ParticleTypes.FALLING_DUST, blockState)
                    ParticleTypes.ITEM -> ItemStackParticleEffect(ParticleTypes.ITEM, ItemStack(Items.STONE, 1))
                    ParticleTypes.SCULK_CHARGE -> SculkChargeParticleEffect(0f)
                    ParticleTypes.SHRIEK -> ShriekParticleEffect(0)
                    ParticleTypes.VIBRATION -> VibrationParticleEffect(BlockPositionSource(blockPos.toMC()), 0)

                    else -> throw IllegalStateException("Particle not accounted for: $particle")
                }
            }

            val fx = Client.getMinecraft().particleManager.addParticle(
                effect,
                x,
                y,
                z,
                xSpeed,
                ySpeed,
                zSpeed
            )

            return fx?.let(::Particle)
        }

        fun spawnParticle(particle: MCParticle): Particle {
            Client.getMinecraft().particleManager.addParticle(particle)
            return Particle(particle)
        }
    }
}
