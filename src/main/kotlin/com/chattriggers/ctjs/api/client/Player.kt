package com.chattriggers.ctjs.api.client

import com.chattriggers.ctjs.api.entity.Entity
import com.chattriggers.ctjs.api.entity.PlayerMP
import com.chattriggers.ctjs.api.entity.Team
import com.chattriggers.ctjs.api.inventory.Inventory
import com.chattriggers.ctjs.api.inventory.Item
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.api.render.Renderer
import com.chattriggers.ctjs.api.world.PotionEffect
import com.chattriggers.ctjs.api.world.Scoreboard
import com.chattriggers.ctjs.api.world.World
import com.chattriggers.ctjs.api.world.block.BlockFace
import com.chattriggers.ctjs.api.world.block.BlockPos
import gg.essential.universal.UMath
import gg.essential.universal.UMinecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec2
import org.mozilla.javascript.NativeObject
import java.util.*

object Player {
    @JvmStatic
    fun toMC() = UMinecraft.getMinecraft().player

    @JvmField
    val armor = ArmorWrapper()

    /**
     * Gets Minecraft's EntityPlayerSP object representing the user
     *
     * @return The Minecraft EntityPlayerSP object representing the user
     */
    @Deprecated("Use toMC", ReplaceWith("toMC()"))
    @JvmStatic
    fun getPlayer() = toMC()

    @JvmStatic
    fun getTeam(): Team? {
        val team = Scoreboard.toMC()?.getPlayersTeam(getName()) ?: return null
        return Team(team)
    }

    @JvmStatic
    fun asPlayerMP(): PlayerMP? = toMC()?.let(::PlayerMP)

    @JvmStatic
    fun getX(): Double = toMC()?.x ?: 0.0

    @JvmStatic
    fun getY(): Double = toMC()?.y ?: 0.0

    @JvmStatic
    fun getZ(): Double = toMC()?.z ?: 0.0

    @JvmStatic
    fun getPos(): BlockPos = BlockPos(getX(), getY(), getZ())

    @JvmStatic
    fun getRotation() = toMC()?.let { Vec2(it.xRot, it.yRot) } ?: Vec2(0f, 0f)

    @JvmStatic
    fun getLastX(): Double = toMC()?.xOld ?: 0.0

    @JvmStatic
    fun getLastY(): Double = toMC()?.yOld ?: 0.0

    @JvmStatic
    fun getLastZ(): Double = toMC()?.zOld ?: 0.0

    @JvmStatic
    fun getRenderX(): Double = getLastX() + (getX() - getLastX()) * Renderer.partialTicks

    @JvmStatic
    fun getRenderY(): Double = getLastY() + (getY() - getLastY()) * Renderer.partialTicks

    @JvmStatic
    fun getRenderZ(): Double = getLastZ() + (getZ() - getLastZ()) * Renderer.partialTicks

    /**
     * Gets the player's x motion.
     * This is the amount the player will move in the x direction next tick.
     *
     * @return the player's x motion
     */
    @JvmStatic
    fun getMotionX(): Double = toMC()?.deltaMovement?.x ?: 0.0

    /**
     * Gets the player's y motion.
     * This is the amount the player will move in the y direction next tick.
     *
     * @return the player's y motion
     */
    @JvmStatic
    fun getMotionY(): Double = toMC()?.deltaMovement?.y ?: 0.0

    /**
     * Gets the player's z motion.
     * This is the amount the player will move in the z direction next tick.
     *
     * @return the player's z motion
     */
    @JvmStatic
    fun getMotionZ(): Double = toMC()?.deltaMovement?.z ?: 0.0

    /**
     * Gets the player's camera pitch.
     *
     * @return the player's camera pitch
     */
    @JvmStatic
    fun getPitch(): Double = UMath.wrapAngleTo180(toMC()?.xRot?.toDouble() ?: 0.0)

    /**
     * Gets the player's camera yaw.
     *
     * @return the player's camera yaw
     */
    @JvmStatic
    fun getYaw(): Double = UMath.wrapAngleTo180(toMC()?.yRot?.toDouble() ?: 0.0)

    /**
     * Gets the player's username.
     *
     * @return the player's username
     */
    @JvmStatic
    fun getName(): String = UMinecraft.getMinecraft().user.name

    /**
     * Gets the Java UUID object of the player.
     * Use of [UUID.toString] in conjunction is recommended.
     *
     * @return the player's uuid
     */
    @JvmStatic
    fun getUUID(): UUID = UMinecraft.getMinecraft().gameProfile.id

    @JvmStatic
    fun getHP(): Float = toMC()?.health ?: 0f

    @JvmStatic
    fun getHunger(): Int = toMC()?.foodData?.foodLevel ?: 0

    @JvmStatic
    fun getSaturation(): Float = toMC()?.foodData?.saturationLevel ?: 0f

    @JvmStatic
    fun getArmorPoints(): Int = toMC()?.armorValue ?: 0

    /**
     * Gets the player's air level.
     *
     * The returned value will be an integer. If the player is not taking damage, it
     * will be between 300 (not in water) and 0. If the player is taking damage, it
     * will be between -20 and 0, getting reset to 0 every time the player takes damage.
     *
     * @return the player's air level
     */
    @JvmStatic
    fun getAirLevel(): Int = toMC()?.airSupply ?: 0

    @JvmStatic
    fun getXPLevel(): Int = toMC()?.experienceLevel ?: 0

    @JvmStatic
    fun getXPProgress(): Float = toMC()?.experienceProgress ?: 0f

    @JvmStatic
    fun getBiome(): String {
        val pos = toMC()?.blockPosition() ?: return ""
        val biomeEntry = World.toMC()?.getBiome(pos) ?: return ""

        val biomeKey = biomeEntry.unwrapKey().orElse(null) ?: return ""
        return biomeKey.identifier().path
    }

    /**
     * Gets the light level at the player's current position.
     *
     * @return the light level at the player's current position
     */
    @JvmStatic
    fun getLightLevel(): Int = toMC()?.blockPosition()?.let { World.toMC()?.getMaxLocalRawBrightness(it) } ?: 0

    @JvmStatic
    fun isMoving(): Boolean = toMC()?.speed?.let { it != 0f } ?: false

    @JvmStatic
    fun isSneaking(): Boolean = toMC()?.isShiftKeyDown ?: false

    @JvmStatic
    fun isSprinting(): Boolean = toMC()?.isSprinting ?: false

    /**
     * Checks if player can be pushed by water.
     *
     * @return true if the player is flying, false otherwise
     */
    @JvmStatic
    fun isFlying(): Boolean = toMC()?.abilities?.flying ?: false

    @JvmStatic
    fun isSleeping(): Boolean = toMC()?.isSleeping ?: false

    /**
     * Gets the direction the player is facing.
     * Example: "South West"
     *
     * @return The direction the player is facing, one of the four cardinal directions
     */
    @JvmStatic
    fun facing(): String {
        if (toMC() == null) return ""

        val yaw = getYaw()

        return when {
            yaw in -22.5..22.5 -> "South"
            yaw in 22.5..67.5 -> "South West"
            yaw in 67.5..112.5 -> "West"
            yaw in 112.5..157.5 -> "North West"
            yaw < -157.5 || yaw > 157.5 -> "North"
            yaw in -157.5..-112.5 -> "North East"
            yaw in -112.5..-67.5 -> "East"
            yaw in -67.5..-22.5 -> "South East"
            else -> ""
        }
    }

    /**
     * Gets the current active potion effects. Returns an empty list
     * if the player has no active potion effects.
     *
     * @return a list of the active [PotionEffect]s
     */
    @JvmStatic
    fun getActivePotionEffects(): List<PotionEffect> =
        toMC()?.activeEffects?.map(::PotionEffect).orEmpty()

    /**
     * Gets the current object that the player is looking at,
     * whether that be a block or an entity. Returns null when not looking
     * at anything.
     *
     * @return the [Block] or [Entity] being looked at, or null if air
     */
    @JvmStatic
    fun lookingAt(): Any? {
        val target = Client.getMinecraft().hitResult

        return when (target?.type) {
            HitResult.Type.MISS -> null
            HitResult.Type.BLOCK -> {
                val block = target as BlockHitResult
                World.getBlockAt(BlockPos(block.blockPos)).withFace(BlockFace.fromMC(block.direction))
            }
            HitResult.Type.ENTITY -> {
                Entity.fromMC((target as EntityHitResult).entity)
            }
            null -> null
        }
    }

    /**
     * Gets the current item in the player's hand.
     *
     * @param hand the hand of the item
     * @return the current held [Item]
     */
    @JvmOverloads
    @JvmStatic
    fun getHeldItem(hand: InteractionHand = InteractionHand.MAIN_HAND): Item? {
        return toMC()?.getItemInHand(hand)?.let(Item::fromMC)
    }

    /**
     * Sets the current held item based on the provided index.
     *
     * @param index the new held item index
     */
    @JvmStatic
    fun setHeldItemIndex(index: Int) {
        toMC()?.inventory?.selectedSlot = index
    }

    /**
     * Gets the current index of the held item.
     *
     * @return the current index
     */
    @JvmStatic
    fun getHeldItemIndex(): Int = toMC()?.inventory?.selectedSlot ?: -1

    /**
     * Gets the inventory of the player, i.e. the inventory accessed by 'e'.
     *
     * @return the player's inventory
     */
    @JvmStatic
    fun getInventory(): Inventory? = toMC()?.inventory?.let(::Inventory)

    /**
     * Gets the display name for the player,
     * i.e. the name shown in tab list and in the player's nametag.
     * @return the display name
     */
    @JvmStatic
    fun getDisplayName(): TextComponent = asPlayerMP()?.getDisplayName() ?: TextComponent("")

    /**
     * Sets the name for this player shown in tab list
     *
     * @param textComponent the new name to display
     */
    @JvmStatic
    fun setTabDisplayName(textComponent: TextComponent) {
        asPlayerMP()?.setTabDisplayName(textComponent)
    }

    /**
     * Sets the name for this player shown above their head,
     * in their name tag
     *
     * @param textComponent the new name to display
     */
    @JvmStatic
    fun setNametagName(textComponent: TextComponent) {
        asPlayerMP()?.setNametagName(textComponent)
    }

    /**
     * Gets the container the user currently has open, i.e. a chest.
     *
     * @return the currently opened container
     */
    @JvmStatic
    fun getContainer(): Inventory? = (Client.getMinecraft().screen as? AbstractContainerScreen<*>)?.let(::Inventory)

    /**
     * Draws the player in the GUI. Takes the same parameters as [Renderer.drawPlayer]
     * minus `player`.
     *
     * @see Renderer.drawPlayer
     */
    @JvmStatic
    fun draw(obj: NativeObject) = apply {
        obj["player"] = this
        Renderer.drawPlayer(obj)
    }

    class ArmorWrapper {
        /**
         * @return the [Item] in the player's helmet slot or null if the slot is empty
         */
        fun getHelmet(): Item? = getInventory()?.getStackInSlot(39)

        /**
         * @return the [Item] in the player's chestplate slot or null if the slot is empty
         */
        fun getChestplate(): Item? = getInventory()?.getStackInSlot(38)

        /**
         * @return the [Item] in the player's leggings slot or null if the slot is empty
         */
        fun getLeggings(): Item? = getInventory()?.getStackInSlot(37)

        /**
         * @return the [Item] in the player's boots slot or null if the slot is empty
         */
        fun getBoots(): Item? = getInventory()?.getStackInSlot(36)
    }
}
