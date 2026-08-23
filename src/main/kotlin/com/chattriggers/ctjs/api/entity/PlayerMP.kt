package com.chattriggers.ctjs.api.entity

import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.api.render.Renderer
import com.chattriggers.ctjs.internal.NameTagOverridable
import com.chattriggers.ctjs.MCTeam
import com.chattriggers.ctjs.internal.utils.asMixin
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.world.entity.player.Player
import net.minecraft.network.chat.Component
import org.mozilla.javascript.NativeObject

class PlayerMP(override val mcValue: Player) : LivingEntity(mcValue) {
    fun isSpectator() = mcValue.isSpectator

    fun getPing(): Int {
        return getPlayerInfo()?.latency ?: -1
    }

    fun getTeam(): Team? {
        val team = getPlayerInfo()?.team ?: return null
        return Team(team)
    }

    /**
     * Gets the display name for this player,
     * i.e. the name shown in tab list and in the player's nametag.
     * @return the display name
     */
    fun getDisplayName() = getPlayerName(getPlayerInfo())

    fun setTabDisplayName(textComponent: TextComponent) {
        getPlayerInfo()?.tabListDisplayName = textComponent
    }

    /**
     * Sets the name for this player shown above their head,
     * in their name tag
     *
     * @param textComponent the new name to display
     */
    fun setNametagName(textComponent: TextComponent) {
        mcValue.asMixin<NameTagOverridable>().ctjs_setOverriddenNametagName(textComponent)
    }

    /**
     * Draws the player in the GUI. Takes the same parameters as [Renderer.drawPlayer]
     * minus `player`.
     *
     * @see Renderer.drawPlayer
     */
    fun draw(obj: NativeObject) = apply {
        obj["player"] = this
        Renderer.drawPlayer(obj)
    }

    private fun getPlayerName(playerListEntry: PlayerInfo?): TextComponent {
        return playerListEntry?.tabListDisplayName?.let { TextComponent(it) }
            ?: TextComponent(
                MCTeam.formatNameForTeam(
                    playerListEntry?.team,
                    Component.literal(playerListEntry?.profile?.name.orEmpty())
                )
            )
    }

    private fun getPlayerInfo() = Client.getConnection()?.getPlayerInfo(mcValue.uuid)
}
