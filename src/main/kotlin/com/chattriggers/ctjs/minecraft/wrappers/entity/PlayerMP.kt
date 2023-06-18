package com.chattriggers.ctjs.minecraft.wrappers.entity

import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.mixins.PlayerEntityMixin
import com.chattriggers.ctjs.utils.asMixin
import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.entity.player.PlayerEntity

class PlayerMP(override val mcValue: PlayerEntity) : LivingEntity(mcValue) {
    fun isSpectator() = mcValue.isSpectator

    fun getPing(): Int {
        return getPlayerInfo()?.latency ?: -1
    }

    fun getTeam(): Team? {
        return getPlayerInfo()?.scoreboardTeam?.let(::Team)
    }

    /**
     * Gets the display name for this player,
     * i.e. the name shown in tab list and in the player's nametag.
     * @return the display name
     */
    fun getDisplayName() = getPlayerName(getPlayerInfo())

    fun setTabDisplayName(textComponent: UTextComponent) {
        getPlayerInfo()?.displayName = textComponent
    }

    /**
     * Sets the name for this player shown above their head,
     * in their name tag
     *
     * @param textComponent the new name to display
     */
    fun setNametagName(textComponent: UTextComponent) {
        mcValue.asMixin<PlayerEntityMixin>().setOverriddenNametagName(textComponent.formattedText)
    }

    /**
     * Draws the player in the GUI
     */
    // TODO:
    // @JvmOverloads
    // fun draw(
    //     player: Any,
    //     x: Int,
    //     y: Int,
    //     rotate: Boolean = false,
    //     showNametag: Boolean = false,
    //     showArmor: Boolean = true,
    //     showCape: Boolean = true,
    //     showHeldItem: Boolean = true,
    //     showArrows: Boolean = true
    // ) = apply {
    //     Renderer.drawPlayer(player, x, y, rotate, showNametag, showArmor, showCape, showHeldItem, showArrows)
    // }

    private fun getPlayerName(playerListEntry: PlayerListEntry?): UTextComponent {
        return playerListEntry?.displayName?.let(::UTextComponent)
            ?: UTextComponent(net.minecraft.scoreboard.Team.decorateName(
                playerListEntry?.scoreboardTeam,
                playerListEntry?.displayName
            ))
    }

    private fun getPlayerInfo() = Client.getConnection()?.getPlayerListEntry(mcValue.uuid)

    override fun toString(): String {
        return "PlayerMP{name=${getName()}, ping=${getPing()}, livingEntity=${super.toString()}}"
    }

    override fun getNameComponent() = getDisplayName()
}
