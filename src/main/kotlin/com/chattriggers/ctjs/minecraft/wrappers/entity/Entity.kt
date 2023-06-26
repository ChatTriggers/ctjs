package com.chattriggers.ctjs.minecraft.wrappers.entity

import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer
import com.chattriggers.ctjs.minecraft.wrappers.CTWrapper
import com.chattriggers.ctjs.minecraft.wrappers.World
import com.chattriggers.ctjs.minecraft.wrappers.world.Chunk
import com.chattriggers.ctjs.minecraft.wrappers.world.block.BlockPos
import com.chattriggers.ctjs.utils.MCEntity
import com.chattriggers.ctjs.utils.MCLivingEntity
import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.entity.MovementType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.registry.RegistryKey
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.dimension.DimensionTypes
import java.util.*
import kotlin.math.sqrt
import net.minecraft.world.dimension.DimensionType as MCDimensionType

open class Entity(override val mcValue: MCEntity) : CTWrapper<MCEntity> {
    fun getX() = mcValue.pos.x

    fun getY() = mcValue.pos.y

    fun getZ() = mcValue.pos.z

    fun getPos() = BlockPos(getX(), getY(), getZ())

    fun getLastX() = mcValue.lastRenderX

    fun getLastY() = mcValue.lastRenderY

    fun getLastZ() = mcValue.lastRenderZ

    fun getRenderX() = getLastX() + (getX() - getLastX()) * Renderer.partialTicks

    fun getRenderY() = getLastY() + (getY() - getLastY()) * Renderer.partialTicks

    fun getRenderZ() = getLastZ() + (getZ() - getLastZ()) * Renderer.partialTicks

    /**
     * Gets the pitch, the horizontal direction the entity is facing towards.
     * This has a range of -180 to 180.
     *
     * @return the entity's pitch
     */
    fun getPitch() = MathHelper.wrapDegrees(mcValue.getPitch(Renderer.partialTicks))

    /**
     * Gets the yaw, the vertical direction the entity is facing towards.
     * This has a range of -180 to 180.
     *
     * @return the entity's yaw
     */
    fun getYaw() = MathHelper.wrapDegrees(mcValue.getYaw(Renderer.partialTicks))

    /**
     * Gets the entity's x motion.
     * This is the amount the entity will move in the x direction next tick.
     *
     * @return the entity's x motion
     */
    fun getMotionX(): Double = mcValue.velocity.x

    /**
     * Gets the entity's y motion.
     * This is the amount the entity will move in the y direction next tick.
     *
     * @return the entity's y motion
     */
    fun getMotionY(): Double = mcValue.velocity.y

    /**
     * Gets the entity's z motion.
     * This is the amount the entity will move in the z direction next tick.
     *
     * @return the entity's z motion
     */
    fun getMotionZ(): Double = mcValue.velocity.z

    /**
     * Returns the entity this entity is riding, if one exists
     *
     * @return an Entity or null
     */
    fun getRiding(): Entity? {
        return mcValue.vehicle?.let(::fromMC)
    }

    // TODO(breaking): Removed getRider()

    /**
     * Returns a list of all entity riding this entity
     *
     * @return List of entities, empty if there are no riders
     */
    fun getRiders() = mcValue.passengerList?.map(::fromMC).orEmpty()

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
    fun getWidth(): Float = mcValue.width

    /**
     * Gets the entire height of the entity's hitbox
     *
     * @return the entity's height
     */
    fun getHeight(): Float = mcValue.height

    /**
     * Gets the height of the eyes on the entity,
     * can be added to its Y coordinate to get the actual Y location of the eyes.
     * This value defaults to 85% of an entity's height, however is different for some entities.
     *
     * @return the height of the entity's eyes
     */
    fun getEyeHeight(): Float = mcValue.standingEyeHeight

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
     * @return the (custom) name of the entity as a [UTextComponent]
     */
    fun getNameComponent(): UTextComponent = UTextComponent(mcValue.name)

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
    fun getAir(): Int = mcValue.air

    // TODO(breaking): remove setAir

    fun distanceTo(other: Entity): Float = distanceTo(other.mcValue)

    fun distanceTo(other: MCEntity): Float = mcValue.distanceTo(other)

    fun distanceTo(blockPos: BlockPos): Double = distanceTo(
        blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble(),
    )

    fun distanceTo(x: Double, y: Double, z: Double): Double = sqrt(mcValue.squaredDistanceTo(x, y, z))

    fun isOnGround() = mcValue.isOnGround

    fun isCollided() = World.toMC()?.getOtherEntities(mcValue, mcValue.boundingBox)?.isNotEmpty() ?: false

    fun getDistanceWalked() = mcValue.distanceTraveled / 0.6f

    fun getStepHeight() = mcValue.stepHeight

    fun hasNoClip() = mcValue.noClip

    fun getTicksExisted() = mcValue.age

    fun getFireResistance() = mcValue.fireTicks

    fun isImmuneToFire() = mcValue.isFireImmune

    fun isInWater() = mcValue.isTouchingWater

    fun isWet() = mcValue.isWet

    // TODO(breaking): Remove isAirborne

    // TODO(breaking): Use enum instead of int
    fun getDimension() = mcValue.world.dimensionKey.let { key ->
        DimensionType.values().first { it.toMC() == key }
    }

    // TODO(breaking): remove setPosition

    // TODO(breaking): remove setAngles

    fun getMaxInPortalTime() = mcValue.maxNetherPortalTime

    // TODO(breaking): remove setOnFire

    // TODO(breaking): remove extinguish

    // TODO(breaking): remove move

    fun isSilent() = mcValue.isSilent

    // TODO(breaking): remove setIsSilent

    fun isInLava() = mcValue.isInLava

    // TODO(breaking): remove addVelocity

    @JvmOverloads
    fun getLookVector(partialTicks: Float = Renderer.partialTicks) = mcValue.getRotationVec(partialTicks)

    @JvmOverloads
    fun getEyePosition(partialTicks: Float = Renderer.partialTicks) = mcValue.eyePos

    fun canBeCollidedWith() = mcValue.isCollidable

    fun canBePushed() = mcValue.isPushable

    // TODO*(breaking): Remove dropItem

    fun isSneaking() = mcValue.isSneaking

    // TODO(breaking): remove setIsSneaking

    fun isSprinting() = mcValue.isSprinting

    // TODO(breaking): remove setIsSprinting

    fun isInvisible() = mcValue.isInvisible

    // TODO(breaking): remove setIsInvisible

    fun isOutsideBorder() = World.toMC()?.worldBorder?.contains(mcValue.blockPos) ?: false

    // TODO(breaking): Remove setIsOutsideBorder

    fun isBurning(): Boolean = mcValue.isOnFire

    fun getWorld() = mcValue.entityWorld

    fun getChunk(): Chunk = Chunk(getWorld().getWorldChunk(mcValue.blockPos))

    override fun toString(): String {
        return "Entity{name=${getName()}, pos=(${getX()}, ${getY()}, ${getZ()})}"
    }

    enum class DimensionType(override val mcValue: RegistryKey<MCDimensionType>) : CTWrapper<RegistryKey<MCDimensionType>> {
        OVERWORLD(DimensionTypes.OVERWORLD),
        NETHER(DimensionTypes.THE_NETHER),
        END(DimensionTypes.THE_END),
        OVERWORLD_CAVES(DimensionTypes.OVERWORLD_CAVES),
    }

    companion object {
        @JvmStatic
        fun fromMC(entity: MCEntity): Entity = when (entity) {
            is PlayerEntity -> PlayerMP(entity)
            is MCLivingEntity -> LivingEntity(entity)
            else -> Entity(entity)
        }
    }
}
