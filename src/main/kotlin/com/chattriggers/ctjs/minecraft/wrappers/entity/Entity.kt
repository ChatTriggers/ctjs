package com.chattriggers.ctjs.minecraft.wrappers.entity

import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer
import com.chattriggers.ctjs.minecraft.wrappers.World
import com.chattriggers.ctjs.minecraft.wrappers.world.Chunk
import com.chattriggers.ctjs.minecraft.wrappers.world.block.BlockPos
import com.chattriggers.ctjs.utils.MCEntity
import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.entity.MovementType
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import java.util.*

open class Entity(val entity: MCEntity) {
    fun getX() = entity.pos.x

    fun getY() = entity.pos.y

    fun getZ() = entity.pos.z

    fun getPos() = BlockPos(getX(), getY(), getZ())

    fun getLastX() = entity.lastRenderX

    fun getLastY() = entity.lastRenderY

    fun getLastZ() = entity.lastRenderZ

    fun getRenderX() = getLastX() + (getX() - getLastX()) * Renderer.partialTicks

    fun getRenderY() = getLastY() + (getY() - getLastY()) * Renderer.partialTicks

    fun getRenderZ() = getLastZ() + (getZ() - getLastZ()) * Renderer.partialTicks

    /**
     * Gets the pitch, the horizontal direction the entity is facing towards.
     * This has a range of -180 to 180.
     *
     * @return the entity's pitch
     */
    fun getPitch() = MathHelper.wrapDegrees(entity.getPitch(Renderer.partialTicks))

    /**
     * Gets the yaw, the vertical direction the entity is facing towards.
     * This has a range of -180 to 180.
     *
     * @return the entity's yaw
     */
    fun getYaw() = MathHelper.wrapDegrees(entity.getYaw(Renderer.partialTicks))

    /**
     * Gets the entity's x motion.
     * This is the amount the entity will move in the x direction next tick.
     *
     * @return the entity's x motion
     */
    fun getMotionX(): Double = entity.velocity.x

    /**
     * Gets the entity's y motion.
     * This is the amount the entity will move in the y direction next tick.
     *
     * @return the entity's y motion
     */
    fun getMotionY(): Double = entity.velocity.y

    /**
     * Gets the entity's z motion.
     * This is the amount the entity will move in the z direction next tick.
     *
     * @return the entity's z motion
     */
    fun getMotionZ(): Double = entity.velocity.z

    /**
     * Returns the entity this entity is riding, if one exists
     *
     * @return an Entity or null
     */
    fun getRiding(): Entity? {
        return entity.vehicle?.let(::Entity)
    }

    // TODO(breaking): Removed getRider()

    /**
     * Returns a list of all entity riding this entity
     *
     * @return List of entities, empty if there are no riders
     */
    fun getRiders() = entity.passengerList?.map(::Entity).orEmpty()

    /**
     * Checks whether the entity is dead.
     * This is a fairly loose term, dead for a particle could mean it has faded,
     * while dead for an entity means it has no health.
     *
     * @return whether an entity is dead
     */
    fun isDead(): Boolean = !entity.isAlive

    /**
     * Gets the entire width of the entity's hitbox
     *
     * @return the entity's width
     */
    fun getWidth(): Float = entity.width

    /**
     * Gets the entire height of the entity's hitbox
     *
     * @return the entity's height
     */
    fun getHeight(): Float = entity.height

    /**
     * Gets the height of the eyes on the entity,
     * can be added to its Y coordinate to get the actual Y location of the eyes.
     * This value defaults to 85% of an entity's height, however is different for some entities.
     *
     * @return the height of the entity's eyes
     */
    fun getEyeHeight(): Float = entity.standingEyeHeight

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
    open fun getNameComponent(): UTextComponent = UTextComponent(entity.name)

    /**
     * Gets the Java class name of the entity, for example "EntityVillager"
     *
     * @return the entity's class name
     */
    fun getClassName(): String = entity.javaClass.simpleName

    /**
     * Gets the Java UUID object of this entity.
     * Use of [UUID.toString] in conjunction is recommended.
     *
     * @return the entity's uuid
     */
    fun getUUID(): UUID = entity.uuid

    /**
     * Gets the entity's air level.
     *
     * The returned value will be an integer. If the player is not taking damage, it
     * will be between 300 (not in water) and 0. If the player is taking damage, it
     * will be between -20 and 0, getting reset to 0 every time the player takes damage.
     *
     * @return the entity's air level
     */
    fun getAir(): Int = entity.air

    fun setAir(air: Int) = apply {
        entity.air = air
    }

    fun distanceTo(other: Entity): Float = distanceTo(other.entity)

    fun distanceTo(other: MCEntity): Float = entity.distanceTo(other)

    // fun distanceTo(blockPos: BlockPos): Float = entity.getDistance(
    //     blockPos.x.toDouble(),
    //     blockPos.y.toDouble(),
    //     blockPos.z.toDouble()
    // ).toFloat()
    //
    // fun distanceTo(x: Float, y: Float, z: Float): Float = entity.distanceTo(
    //     x.toDouble(),
    //     y.toDouble(),
    //     z.toDouble()
    // ).toFloat()

    fun isOnGround() = entity.isOnGround

    // TODO: Test this
    fun isCollided() = entity.collidedSoftly

    fun getDistanceWalked() = entity.distanceTraveled / 0.6f

    fun getStepHeight() = entity.stepHeight

    fun hasNoClip() = entity.noClip

    fun getTicksExisted() = entity.age

    fun getFireResistance() = entity.fireTicks

    fun isImmuneToFire() = entity.isFireImmune

    fun isInWater() = entity.isTouchingWater

    fun isWet() = entity.isWet

    // TODO(breaking): Remove isAirborne

    // TODO(breaking): Use enum instead of int
    fun getDimension() = entity.world.dimension

    fun setPosition(x: Double, y: Double, z: Double) = apply {
        entity.setPosition(x, y, z)
    }

    fun setAngles(yaw: Float, pitch: Float) = apply {
        entity.pitch = pitch
        entity.yaw = yaw
    }

    fun getMaxInPortalTime() = entity.maxNetherPortalTime

    fun setOnFire(seconds: Int) = apply {
        entity.setOnFireFor(seconds)
    }

    fun extinguish() = apply {
        entity.extinguish()
    }

    fun move(x: Double, y: Double, z: Double) = apply {
        entity.move(MovementType.SELF, Vec3d(x, y, z))
    }

    fun isSilent() = entity.isSilent

    fun setIsSilent(silent: Boolean) = apply {
        entity.isSilent = silent
    }

    fun isInLava() = entity.isInLava

    fun addVelocity(x: Double, y: Double, z: Double) = apply {
        entity.addVelocity(x, y, z)
    }

    @JvmOverloads
    fun getLookVector(partialTicks: Float = Renderer.partialTicks) = entity.getRotationVec(partialTicks)

    @JvmOverloads
    fun getEyePosition(partialTicks: Float = Renderer.partialTicks) = entity.eyePos

    fun canBeCollidedWith() = entity.isCollidable

    fun canBePushed() = entity.isPushable

    // fun dropItem(item: Item, size: Int) = entity.dropItem(item.item, size)

    fun isSneaking() = entity.isSneaking

    fun setIsSneaking(sneaking: Boolean) = apply {
        entity.isSneaking = sneaking
    }

    fun isSprinting() = entity.isSprinting

    fun setIsSprinting(sprinting: Boolean) = apply {
        entity.isSprinting = sprinting
    }

    fun isInvisible() = entity.isInvisible

    fun setIsInvisible(invisible: Boolean) = apply {
        entity.isInvisible = invisible
    }

    fun isOutsideBorder() = World.getWorld()?.worldBorder?.contains(entity.blockPos) ?: false

    // TODO(breaking): Remove setIsOutsideBorder

    fun isBurning(): Boolean = entity.isOnFire

    fun getWorld() = entity.entityWorld

    fun getChunk(): Chunk = Chunk(getWorld().getWorldChunk(entity.blockPos))

    override fun toString(): String {
        return "Entity{name=${getName()}, x=${getX()}, y=${getY()}, z=${getZ()}}"
    }
}
