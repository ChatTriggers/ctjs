package com.chattriggers.ctjs.api.entity

import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.api.render.Renderer
import com.chattriggers.ctjs.internal.NameTagOverridable
import com.chattriggers.ctjs.MCTeam
import com.chattriggers.ctjs.internal.utils.asMixin
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import org.mozilla.javascript.NativeObject

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

    fun setTabDisplayName(textComponent: TextComponent) {
        getPlayerInfo()?.displayName = textComponent
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

    private fun getPlayerName(playerListEntry: PlayerListEntry?): TextComponent {
        return playerListEntry?.displayName?.let { TextComponent(it) }
            ?: TextComponent(
                MCTeam.decorateName(
                    playerListEntry?.scoreboardTeam,
                    Text.of(playerListEntry?.profile?.name)
                )
            )
    }

    private fun getPlayerInfo() = Client.getConnection()?.getPlayerListEntry(mcValue.uuid)
}
