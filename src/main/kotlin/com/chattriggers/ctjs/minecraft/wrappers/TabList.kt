package com.chattriggers.ctjs.minecraft.wrappers

import com.chattriggers.ctjs.minecraft.objects.TextComponent
import com.chattriggers.ctjs.mixins.PlayerListHudAccessor
import com.chattriggers.ctjs.utils.MCTeam
import com.chattriggers.ctjs.utils.asMixin
import com.google.common.collect.ComparisonChain
import com.google.common.collect.Ordering
import net.minecraft.client.gui.hud.PlayerListHud
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.text.Text
import net.minecraft.world.GameMode

object TabList : CTWrapper<PlayerListHud?> {
    override val mcValue get() = Client.getTabGui()

    private val playerComparator = Ordering.from(PlayerComparator())

    /**
     * Gets names set in scoreboard objectives
     *
     * @return The formatted names
     */
    fun getNamesByObjectives(): List<String> {
        val scoreboard = Scoreboard.toMC() ?: return emptyList()
        val sidebarObjective = scoreboard.getObjectiveForSlot(0) ?: return emptyList()

        val scores = scoreboard.getAllPlayerScores(sidebarObjective)

        return scores.map {
            val team = scoreboard.getTeam(it.playerName)
            TextComponent(MCTeam.decorateName(team, TextComponent(it.playerName))).formattedText
        }
    }

    fun getNames(): List<String> {
        if (toMC() == null) return listOf()

        return playerComparator
            .sortedCopy(Player.toMC()!!.networkHandler.playerList)
            .map { TextComponent(toMC()!!.getPlayerName(it)).formattedText }
    }

    /**
     * Gets all names in tabs without formatting
     *
     * @return the unformatted names
     */
    fun getUnformattedNames(): List<String> {
        if (Player.toMC() == null) return listOf()

        return Client.getConnection()?.playerList?.let {
            playerComparator.sortedCopy(it)
        }?.map {
            it.profile.name
        } ?: emptyList()
    }

    fun getHeaderComponent() = toMC()?.asMixin<PlayerListHudAccessor>()?.header?.let(::TextComponent)

    fun getHeader() = getHeaderComponent()?.formattedText

    /**
     * Sets the header text for the TabList.
     * If [header] is null, it will remove the header entirely
     *
     * @param header the header to set, or null to clear
     */
    fun setHeader(header: Any?) {
        when (header) {
            is String -> toMC()?.setHeader(TextComponent(header))
            is TextComponent -> toMC()?.setHeader(header)
            is Text -> toMC()?.setHeader(header)
            null -> toMC()?.setHeader(null)
        }
    }

    fun clearHeader() = setHeader(null)

    fun getFooterComponent() = toMC()?.asMixin<PlayerListHudAccessor>()?.footer?.let(::TextComponent)

    fun getFooter() = getFooterComponent()?.formattedText

    /**
     * Sets the footer text for the TabList.
     * If [footer] is null, it will remove the footer entirely
     *
     * @param footer the footer to set, or null to clear
     */
    fun setFooter(footer: Any?) {
        when (footer) {
            is String -> toMC()?.setFooter(TextComponent(footer))
            is TextComponent -> toMC()?.setFooter(footer)
            is Text -> toMC()?.setFooter(footer)
            null -> toMC()?.setFooter(null)
        }
    }

    fun clearFooter() = setFooter(null)

    internal class PlayerComparator internal constructor() : Comparator<PlayerListEntry> {
        override fun compare(playerOne: PlayerListEntry, playerTwo: PlayerListEntry): Int {
            val teamOne = playerOne.scoreboardTeam
            val teamTwo = playerTwo.scoreboardTeam

            return ComparisonChain
                .start()
                .compareTrueFirst(playerOne.gameMode != GameMode.SPECTATOR, playerTwo.gameMode != GameMode.SPECTATOR)
                .compare(teamOne?.name ?: "", teamTwo?.name ?: "")
                .compare(playerOne.profile.name, playerTwo.profile.name)
                .result()
        }
    }
}
