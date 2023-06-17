package com.chattriggers.ctjs.minecraft.wrappers

import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer
import com.chattriggers.ctjs.minecraft.wrappers.entity.PlayerMP
import com.chattriggers.ctjs.minecraft.wrappers.entity.Team
import com.chattriggers.ctjs.minecraft.wrappers.inventory.Inventory
import com.chattriggers.ctjs.minecraft.wrappers.inventory.Item
import com.chattriggers.ctjs.minecraft.wrappers.world.PotionEffect
import gg.essential.universal.UMath
import gg.essential.universal.UMinecraft
import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.client.network.ClientPlayerEntity
import java.util.*

object Player {
    /**
     * Gets Minecraft's EntityPlayerSP object representing the user
     *
     * @return The Minecraft EntityPlayerSP object representing the user
     */
    @JvmStatic
    fun getPlayer(): ClientPlayerEntity? = UMinecraft.getMinecraft().player

    @JvmStatic
    fun getTeam(): Team? = Scoreboard.getScoreboard()?.getPlayerTeam(getName())?.let(::Team)

    @JvmStatic
    fun asPlayerMP(): PlayerMP? = getPlayer()?.let(::PlayerMP)

    @JvmStatic
    fun getX(): Double = getPlayer()?.x ?: 0.0

    @JvmStatic
    fun getY(): Double = getPlayer()?.y ?: 0.0

    @JvmStatic
    fun getZ(): Double = getPlayer()?.z ?: 0.0

    @JvmStatic
    fun getLastX(): Double = getPlayer()?.lastRenderX ?: 0.0

    @JvmStatic
    fun getLastY(): Double = getPlayer()?.lastRenderY ?: 0.0

    @JvmStatic
    fun getLastZ(): Double = getPlayer()?.lastRenderZ ?: 0.0

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
    fun getMotionX(): Double = getPlayer()?.velocity?.x ?: 0.0

    /**
     * Gets the player's y motion.
     * This is the amount the player will move in the y direction next tick.
     *
     * @return the player's y motion
     */
    @JvmStatic
    fun getMotionY(): Double = getPlayer()?.velocity?.y ?: 0.0

    /**
     * Gets the player's z motion.
     * This is the amount the player will move in the z direction next tick.
     *
     * @return the player's z motion
     */
    @JvmStatic
    fun getMotionZ(): Double = getPlayer()?.velocity?.z ?: 0.0

    /**
     * Gets the player's camera pitch.
     *
     * @return the player's camera pitch
     */
    @JvmStatic
    fun getPitch(): Double = UMath.wrapAngleTo180(getPlayer()?.pitch?.toDouble() ?: 0.0)

    /**
     * Gets the player's camera yaw.
     *
     * @return the player's camera yaw
     */
    @JvmStatic
    fun getYaw(): Double = UMath.wrapAngleTo180(getPlayer()?.yaw?.toDouble() ?: 0.0)

    // TODO(breaking): Remove getRawYaw (completely useless method)

    /**
     * Gets the player's username.
     *
     * @return the player's username
     */
    @JvmStatic
    fun getName(): String = UMinecraft.getMinecraft().session.username

    // TODO(breaking): getUUID returns UUID object now
    /**
     * Gets the Java UUID object of the player.
     * Use of [UUID.toString] in conjunction is recommended.
     *
     * @return the player's uuid
     */
    @JvmStatic
    fun getUUID(): UUID = UMinecraft.getMinecraft().session.profile.id

    @JvmStatic
    fun getHP(): Float = getPlayer()?.health ?: 0f

    @JvmStatic
    fun getHunger(): Int = getPlayer()?.hungerManager?.foodLevel ?: 0

    @JvmStatic
    fun getSaturation(): Float = getPlayer()?.hungerManager?.saturationLevel ?: 0f

    @JvmStatic
    fun getArmorPoints(): Int = getPlayer()?.armor ?: 0

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
    fun getAirLevel(): Int = getPlayer()?.air ?: 0

    @JvmStatic
    fun getXPLevel(): Int = getPlayer()?.experienceLevel ?: 0

    @JvmStatic
    fun getXPProgress(): Float = getPlayer()?.experienceProgress ?: 0f

    @JvmStatic
    fun getBiome(): String {
        val pos = getPlayer()?.blockPos ?: return ""
        val biomeEntry = World.getWorld()?.getBiome(pos) ?: return ""

        return biomeEntry.key.get().value.path
    }

    /**
     * Gets the light level at the player's current position.
     *
     * @return the light level at the player's current position
     */
    @JvmStatic
    fun getLightLevel(): Int = World.getWorld()?.getLightLevel(getPlayer()?.blockPos) ?: 0

    @JvmStatic
    fun isMoving(): Boolean = getPlayer()?.movementSpeed?.let { it != 0f } ?: false

    @JvmStatic
    fun isSneaking(): Boolean = getPlayer()?.isSneaking ?: false

    @JvmStatic
    fun isSprinting(): Boolean = getPlayer()?.isSprinting ?: false

    /**
     * Checks if player can be pushed by water.
     *
     * @return true if the player is flying, false otherwise
     */
    @JvmStatic
    fun isFlying(): Boolean = getPlayer()?.abilities?.flying ?: false

    @JvmStatic
    fun isSleeping(): Boolean = getPlayer()?.isSleeping ?: false

    /**
     * Gets the direction the player is facing.
     * Example: "South West"
     *
     * @return The direction the player is facing, one of the four cardinal directions
     */
    @JvmStatic
    fun facing(): String {
        if (getPlayer() == null) return ""

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
    fun getActivePotionEffects(): List<PotionEffect> = getPlayer()?.activeStatusEffects?.values?.map(::PotionEffect).orEmpty()

    /**
     * Gets the current object that the player is looking at,
     * whether that be a block or an entity. Returns an air [BlockType] when not looking
     * at anything.
     *
     * @return the [Block], [Entity], [Sign], or [BlockType] being looked at
     */
    @JvmStatic
    fun lookingAt(): Any {
        TODO()
    }

    /**
     * Gets the current item in the player's main hand.
     *
     * @return the current held [Item]
     */
    @JvmStatic
    fun getHeldItem(): Item? = getPlayer()?.inventory?.selectedSlot?.let {
        getInventory()?.getStackInSlot(it)
    }

    /**
     * Sets the current held item based on the provided index.
     *
     * @param index the new held item index
     */
    @JvmStatic
    fun setHeldItemIndex(index: Int) {
        getPlayer()?.inventory?.selectedSlot = index
    }

    /**
     * Gets the current index of the held item.
     *
     * @return the current index
     */
    @JvmStatic
    fun getHeldItemIndex(): Int = getPlayer()?.inventory?.selectedSlot ?: -1

    /**
     * Gets the inventory of the player, i.e. the inventory accessed by 'e'.
     *
     * @return the player's inventory
     */
    @JvmStatic
    fun getInventory(): Inventory? = getPlayer()?.inventory?.let(::Inventory)

    /**
     * Gets the display name for the player,
     * i.e. the name shown in tab list and in the player's nametag.
     * @return the display name
     */
    @JvmStatic
    fun getDisplayName(): UTextComponent = asPlayerMP()?.getDisplayName() ?: UTextComponent("")

    /**
     * Sets the name for this player shown in tab list
     *
     * @param textComponent the new name to display
     */
    @JvmStatic
    fun setTabDisplayName(textComponent: UTextComponent) {
        asPlayerMP()?.setTabDisplayName(textComponent)
    }

    /**
     * Sets the name for this player shown above their head,
     * in their name tag
     *
     * @param textComponent the new name to display
     */
    @JvmStatic
    fun setNametagName(textComponent: UTextComponent) {
        asPlayerMP()?.setNametagName(textComponent)
    }

    // TODO:
    // @Deprecated("Use the better named method", ReplaceWith("getContainer()"))
    // @JvmStatic
    // fun getOpenedInventory(): Inventory? = getContainer()
    //
    // /**
    //  * Gets the container the user currently has open, i.e. a chest.
    //  *
    //  * @return the currently opened container
    //  */
    // @JvmStatic
    // fun getContainer(): Inventory? = getPlayer()?.openContainer?.let(::Inventory)

    // /**
    //  * Draws the player in the GUI
    //  */
    // @JvmStatic
    // @JvmOverloads
    // fun draw(
    //     x: Int,
    //     y: Int,
    //     rotate: Boolean = false,
    //     showNametag: Boolean = false,
    //     showArmor: Boolean = true,
    //     showCape: Boolean = true,
    //     showHeldItem: Boolean = true,
    //     showArrows: Boolean = true
    // ) = apply {
    //     Renderer.drawPlayer(this, x, y, rotate, showNametag, showArmor, showCape, showHeldItem, showArrows)
    // }

    object armor {
        /**
         * @return the [Item] in the player's helmet slot or null if the slot is empty
         */
        @JvmStatic
        fun getHelmet(): Item? = getInventory()?.getStackInSlot(39)

        /**
         * @return the [Item] in the player's chestplate slot or null if the slot is empty
         */
        @JvmStatic
        fun getChestplate(): Item? = getInventory()?.getStackInSlot(38)

        /**
         * @return the [Item] in the player's leggings slot or null if the slot is empty
         */
        @JvmStatic
        fun getLeggings(): Item? = getInventory()?.getStackInSlot(37)

        /**
         * @return the [Item] in the player's boots slot or null if the slot is empty
         */
        @JvmStatic
        fun getBoots(): Item? = getInventory()?.getStackInSlot(36)
    }
}
