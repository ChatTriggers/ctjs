package com.chattriggers.ctjs.minecraft.wrappers

import com.chattriggers.ctjs.mixins.PlayerListHudAccessor
import com.chattriggers.ctjs.utils.MCTeam
import com.chattriggers.ctjs.utils.asMixin
import com.google.common.collect.ComparisonChain
import com.google.common.collect.Ordering
import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.text.Text
import net.minecraft.world.GameMode

object TabList {
    private val playerComparator = Ordering.from(PlayerComparator())

    /**
     * Gets names set in scoreboard objectives
     *
     * @return The formatted names
     */
    // TODO(breaking): Return List<UTextComponent> instead of List<String>
    @JvmStatic
    fun getNamesByObjectives(): List<UTextComponent> {
        val scoreboard = Scoreboard.getScoreboard() ?: return emptyList()
        val sidebarObjective = scoreboard.getObjectiveForSlot(0) ?: return emptyList()

        val scores = scoreboard.getAllPlayerScores(sidebarObjective)

        return scores.map {
            val team = scoreboard.getTeam(it.playerName)
            UTextComponent(MCTeam.decorateName(team, UTextComponent(it.playerName)))
        }
    }

    // TODO(breaking): Return UTextComponent
    @JvmStatic
    fun getNames(): List<UTextComponent> {
        if (Client.getTabGui() == null) return listOf()

        return playerComparator
            .sortedCopy(Player.getPlayer()!!.networkHandler.playerList)
            .map { UTextComponent(Client.getTabGui()!!.getPlayerName(it)) }
    }

    /**
     * Gets all names in tabs without formatting
     *
     * @return the unformatted names
     */
    @JvmStatic
    fun getUnformattedNames(): List<String> {
        if (Player.getPlayer() == null) return listOf()

        return Client.getConnection()?.playerList?.let {
            playerComparator.sortedCopy(it)
        }?.map {
            it.profile.name
        } ?: emptyList()
    }

    // TODO(breaking): Rename and return UTextComponent instead of UMessage
    @JvmStatic
    fun getHeaderComponent() = Client.getTabGui()?.asMixin<PlayerListHudAccessor>()?.header?.let(::UTextComponent)

    @JvmStatic
    fun getHeader() = getHeaderComponent()?.formattedText

    /**
     * Sets the header text for the TabList.
     * If [header] is null, it will remove the header entirely
     *
     * @param header the header to set, or null to clear
     */
    @JvmStatic
    fun setHeader(header: Any?) {
        when (header) {
            is String -> Client.getTabGui()?.setHeader(UTextComponent(header))
            is UTextComponent -> Client.getTabGui()?.setHeader(header)
            is Text -> Client.getTabGui()?.setHeader(header)
            null -> Client.getTabGui()?.setHeader(null)
        }
    }

    @JvmStatic
    fun clearHeader() = setHeader(null)

    // TODO(breaking): Rename and return UTextComponent instead of UMessage
    @JvmStatic
    fun getFooterComponent() = Client.getTabGui()?.asMixin<PlayerListHudAccessor>()?.footer?.let(::UTextComponent)

    @JvmStatic
    fun getFooter() = getFooterComponent()?.formattedText

    // TODO(breaking): Rename and return UTextComponent instead of UMessage
    /**
     * Sets the footer text for the TabList.
     * If [footer] is null, it will remove the footer entirely
     *
     * @param footer the footer to set, or null to clear
     */
    @JvmStatic
    fun setFooter(footer: Any?) {
        when (footer) {
            is String -> Client.getTabGui()?.setFooter(UTextComponent(footer))
            is UTextComponent -> Client.getTabGui()?.setFooter(footer)
            is Text -> Client.getTabGui()?.setFooter(footer)
            null -> Client.getTabGui()?.setFooter(null)
        }
    }

    @JvmStatic
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
