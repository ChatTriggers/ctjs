package com.chattriggers.ctjs.api.entity

import com.chattriggers.ctjs.api.CTWrapper
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.api.render.Renderer
import com.chattriggers.ctjs.api.world.Chunk
import com.chattriggers.ctjs.api.world.World
import com.chattriggers.ctjs.api.world.block.BlockPos
import com.chattriggers.ctjs.MCDimensionType
import com.chattriggers.ctjs.MCEntity
import com.chattriggers.ctjs.MCLivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.resources.ResourceKey
import net.minecraft.util.Mth
import net.minecraft.world.level.dimension.BuiltinDimensionTypes
import net.minecraft.world.phys.Vec2
import java.util.*
import kotlin.math.sqrt

open class Entity(override val mcValue: MCEntity) : CTWrapper<MCEntity> {
    fun getX() = mcValue.x

    fun getY() = mcValue.y

    fun getZ() = mcValue.z

    fun getPos() = BlockPos(getX(), getY(), getZ())

    fun getRotation() = Vec2(mcValue.xRot, mcValue.yRot)

    fun getLastX() = mcValue.xOld

    fun getLastY() = mcValue.yOld

    fun getLastZ() = mcValue.zOld

    fun getRenderX() = getLastX() + (getX() - getLastX()) * Renderer.partialTicks

    fun getRenderY() = getLastY() + (getY() - getLastY()) * Renderer.partialTicks

    fun getRenderZ() = getLastZ() + (getZ() - getLastZ()) * Renderer.partialTicks

    /**
     * Gets the pitch, the horizontal direction the entity is facing towards.
     * This has a range of -180 to 180.
     *
     * @return the entity's pitch
     */
    fun getPitch() = Mth.wrapDegrees(mcValue.getXRot(Renderer.partialTicks))

    /**
     * Gets the yaw, the vertical direction the entity is facing towards.
     * This has a range of -180 to 180.
     *
     * @return the entity's yaw
     */
    fun getYaw() = Mth.wrapDegrees(mcValue.getYRot(Renderer.partialTicks))

    /**
     * Gets the entity's x motion.
     * This is the amount the entity will move in the x direction next tick.
     *
     * @return the entity's x motion
     */
    fun getMotionX(): Double = mcValue.deltaMovement.x

    /**
     * Gets the entity's y motion.
     * This is the amount the entity will move in the y direction next tick.
     *
     * @return the entity's y motion
     */
    fun getMotionY(): Double = mcValue.deltaMovement.y

    /**
     * Gets the entity's z motion.
     * This is the amount the entity will move in the z direction next tick.
     *
     * @return the entity's z motion
     */
    fun getMotionZ(): Double = mcValue.deltaMovement.z

    /**
     * Returns the entity this entity is riding, if one exists
     *
     * @return an Entity or null
     */
    fun getRiding(): Entity? {
        return mcValue.vehicle?.let(::fromMC)
    }

    /**
     * Returns a list of all entity riding this entity
     *
     * @return List of entities, empty if there are no riders
     */
    fun getRiders() = mcValue.passengers.map(::fromMC)

    /**
     * Checks whether the entity is dead.
     * This is a fairly loose term, dead for a particle could mean it has faded,
     * while dead for an entity means it has no health.
     *
     * @return whether an entity is dead
     */
    fun isDead(): Boolean = !mcValue.isAlive

    /**
     * Gets the entire width of the entity's hitbox
     *
     * @return the entity's width
     */
    fun getWidth(): Float = mcValue.bbWidth

    /**
     * Gets the entire height of the entity's hitbox
     *
     * @return the entity's height
     */
    fun getHeight(): Float = mcValue.bbHeight

    /**
     * Gets the height of the eyes on the entity,
     * can be added to its Y coordinate to get the actual Y location of the eyes.
     * This value defaults to 85% of an entity's height, however is different for some entities.
     *
     * @return the height of the entity's eyes
     */
    fun getEyeHeight(): Float = mcValue.eyeHeight

    /**
     * Gets the name of the entity, could be "Villager",
     * or, if the entity has a custom name, it returns that.
     *
     * @return the (custom) name of the entity as a String
     */
    fun getName(): String = getNameComponent().unformattedText

    /**
     * Gets the name of the entity, could be "Villager",
     * or, if the entity has a custom name, it returns that.
     *
     * @return the (custom) name of the entity as a [TextComponent]
     */
    fun getNameComponent(): TextComponent = TextComponent(mcValue.name)

    /**
     * Gets the Java class name of the entity, for example "EntityVillager"
     *
     * @return the entity's class name
     */
    fun getClassName(): String = mcValue.javaClass.simpleName

    /**
     * Gets the Java UUID object of this entity.
     * Use of [UUID.toString] in conjunction is recommended.
     *
     * @return the entity's uuid
     */
    fun getUUID(): UUID = mcValue.uuid

    /**
     * Gets the entity's air level.
     *
     * The returned value will be an integer. If the player is not taking damage, it
     * will be between 300 (not in water) and 0. If the player is taking damage, it
     * will be between -20 and 0, getting reset to 0 every time the player takes damage.
     *
     * @return the entity's air level
     */
    fun getAir(): Int = mcValue.airSupply

    fun distanceTo(other: Entity): Float = distanceTo(other.mcValue)

    fun distanceTo(other: MCEntity): Float = mcValue.distanceTo(other)

    fun distanceTo(blockPos: BlockPos): Double = distanceTo(
        blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble(),
    )

    fun distanceTo(x: Double, y: Double, z: Double): Double = sqrt(mcValue.distanceToSqr(x, y, z))

    fun isOnGround() = mcValue.onGround()

    fun isCollided() = World.toMC()?.getEntities(mcValue, mcValue.boundingBox) { true }?.isNotEmpty() ?: false

    fun getDistanceWalked() = mcValue.moveDist / 0.6f

    fun getStepHeight() = mcValue.maxUpStep()

    fun hasNoClip() = mcValue.noPhysics

    fun getTicksExisted() = mcValue.tickCount

    fun getFireResistance() = mcValue.remainingFireTicks

    fun isImmuneToFire() = mcValue.fireImmune()

    fun isInWater() = mcValue.isInWater

    fun isWet() = mcValue.isInWaterOrRain

    fun getDimension() = mcValue.level().dimensionTypeRegistration().unwrapKey().orElseThrow().let { key ->
        DimensionType.entries.first { it.toMC() == key }
    }

    fun getMaxInPortalTime() = mcValue.portalCooldown

    fun isSilent() = mcValue.isSilent

    fun isInLava() = mcValue.isInLava

    @JvmOverloads
    fun getLookVector(partialTicks: Float = Renderer.partialTicks) = mcValue.getViewVector(partialTicks)

    @JvmOverloads
    fun getEyePosition(partialTicks: Float = Renderer.partialTicks) = mcValue.getEyePosition(partialTicks)

    fun canBeCollidedWith() = mcValue.canBeCollidedWith(null)

    fun canBePushed() = mcValue.isPushable

    fun isSneaking() = mcValue.isCrouching

    fun isSprinting() = mcValue.isSprinting

    fun isInvisible() = mcValue.isInvisible

    fun isOutsideBorder() = World.toMC()?.worldBorder?.isWithinBounds(mcValue.blockPosition()) ?: false

    fun isBurning(): Boolean = mcValue.isOnFire

    fun getWorld() = mcValue.level()

    fun getChunk(): Chunk = Chunk(getWorld().getChunkAt(mcValue.blockPosition()))

    override fun toString(): String {
        val coordStrings = listOf(getX(), getY(), getZ()).map { "%.3f".format(it) }
        return "${this::class.simpleName}(name=${getName()}, pos=[${coordStrings.joinToString()}])"
    }

    enum class DimensionType(
        override val mcValue: ResourceKey<MCDimensionType>,
    ) : CTWrapper<ResourceKey<MCDimensionType>> {
        OVERWORLD(BuiltinDimensionTypes.OVERWORLD),
        NETHER(BuiltinDimensionTypes.NETHER),
        END(BuiltinDimensionTypes.END),
        OVERWORLD_CAVES(BuiltinDimensionTypes.OVERWORLD_CAVES),
    }

    companion object {
        @JvmStatic
        fun fromMC(entity: MCEntity): Entity = when (entity) {
            is Player -> PlayerMP(entity)
            is MCLivingEntity -> LivingEntity(entity)
            else -> Entity(entity)
        }
    }
}
