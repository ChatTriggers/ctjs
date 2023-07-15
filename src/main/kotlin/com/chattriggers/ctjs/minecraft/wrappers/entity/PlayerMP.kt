package com.chattriggers.ctjs.minecraft.wrappers.entity

import com.chattriggers.ctjs.NameTagOverridable
import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer
import com.chattriggers.ctjs.minecraft.objects.TextComponent
import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.utils.MCTeam
import com.chattriggers.ctjs.utils.asMixin
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
        mcValue.asMixin<NameTagOverridable>().setOverriddenNametagName(textComponent)
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
        return playerListEntry?.displayName?.let(::TextComponent)
            ?: TextComponent(MCTeam.decorateName(
                playerListEntry?.scoreboardTeam,
                Text.of(playerListEntry?.profile?.name)
            ))
    }

    private fun getPlayerInfo() = Client.getConnection()?.getPlayerListEntry(mcValue.uuid)
}
