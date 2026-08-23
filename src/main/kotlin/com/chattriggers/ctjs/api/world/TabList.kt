package com.chattriggers.ctjs.api.world

import com.chattriggers.ctjs.MCTeam
import com.chattriggers.ctjs.api.CTWrapper
import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.client.Player
import com.chattriggers.ctjs.api.entity.Team
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.internal.mixins.ClientPlayNetworkHandlerAccessor
import com.chattriggers.ctjs.internal.mixins.PlayerListEntryAccessor
import com.chattriggers.ctjs.internal.mixins.PlayerListHudAccessor
import com.chattriggers.ctjs.internal.utils.asMixin
import com.google.common.collect.ComparisonChain
import com.google.common.collect.Ordering
import com.mojang.authlib.GameProfile
import gg.essential.elementa.state.BasicState
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.network.chat.Component
import net.minecraft.world.level.GameType
import java.util.*
import java.util.concurrent.CompletableFuture

object TabList {
    private var needsUpdate = true
    private var tabListNames = mutableListOf<Name>()
    private val playerComparator = Ordering.from(PlayerComparator())
    private val overrides = TabListOverrideState<Component>()
    internal val customHeader get() = overrides.customHeader
    internal val customFooter get() = overrides.customFooter
    private var tabListHeader: TextComponent? = null
    private var tabListFooter: TextComponent? = null

    @JvmStatic
    fun toMC() = Client.getTabGui()

    /**
     * Gets the scoreboard objective corresponding to the tab list, or null if it doesn't exist
     */
    @JvmStatic
    fun getObjective(): Objective? = Scoreboard.toMC()?.getDisplayObjective(DisplaySlot.LIST)

    /**
     * Gets the tab list header as a [TextComponent]
     *
     * @return the header
     */
    @JvmStatic
    fun getHeaderComponent(): TextComponent? {
        if (needsUpdate) {
            updateNames()
            needsUpdate = false
        }

        return tabListHeader
    }

    /**
     * Gets the tab list header as a formatted string.
     *
     * @return the header
     */
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
        val component: Component? = when (header) {
            is TextComponent? -> {
                tabListHeader = header
                header
            }
            is CharSequence, is Component -> {
                tabListHeader = TextComponent(header)
                tabListHeader
            }
            else -> return
        }
        overrides.beginCustomHeader()
        try {
            toMC()?.setHeader(component)
        } finally {
            overrides.endCustomHeader()
        }
    }

    @JvmStatic
    fun clearHeader() = setHeader(null)

    /**
     * Gets the tab list footer as a [TextComponent]
     *
     * @return the footer
     */
    @JvmStatic
    fun getFooterComponent(): TextComponent? {
        if (needsUpdate) {
            updateNames()
            needsUpdate = false
        }

        return tabListFooter
    }

    /**
     * Gets the tab list footer as a string.
     * Be aware that this can contain color codes.
     *
     * @return the footer
     */
    @JvmStatic
    fun getFooter() = getFooterComponent()?.formattedText

    /**
     * Sets the footer text for the TabList.
     * If [footer] is null, it will remove the footer entirely
     *
     * @param footer the footer to set, or null to clear
     */
    @JvmStatic
    fun setFooter(footer: Any?) {
        val component: Component? = when (footer) {
            is TextComponent? -> {
                tabListFooter = footer
                footer
            }
            is CharSequence, is Component -> {
                tabListFooter = TextComponent(footer)
                tabListFooter
            }
            else -> return
        }
        overrides.beginCustomFooter()
        try {
            toMC()?.setFooter(component)
        } finally {
            overrides.endCustomFooter()
        }
    }

    @JvmStatic
    fun clearFooter() = setFooter(null)

    /**
     * Gets names set in scoreboard objectives
     *
     * @return The formatted names
     */
    @JvmStatic
    fun getNamesByObjectives(): List<String> {
        val scoreboard = Scoreboard.toMC() ?: return emptyList()
        val tabListObjective = getObjective() ?: return emptyList()

        val scores = scoreboard.listPlayerScores(tabListObjective)

        return scores.map {
            val team = scoreboard.getPlayersTeam(it.owner)
            TextComponent(MCTeam.formatNameForTeam(team, TextComponent(it.owner))).formattedText
        }
    }

    /**
     * Get all names on the tab list
     *
     * @return the list of names
     */
    @JvmStatic
    fun getNames(): List<Name> {
        if (needsUpdate) {
            updateNames()
            needsUpdate = false
        }

        return tabListNames
    }

    /**
     * Gets all names in tabs without formatting
     *
     * @return the unformatted names
     */
    @JvmStatic
    fun getUnformattedNames(): List<String> {
        if (needsUpdate) {
            updateNames()
            needsUpdate = false
        }

        return tabListNames.map { it.toMC().profile.name }
    }

    /**
     * Adds a new name to the tab list
     *
     * @param name the formatted name to add
     * @param useExistingSkin whether to use the skin of the associated Minecraft account using [name].
     * If false, will use a random default skin (Steve, Alex, etc)
     */
    @JvmStatic
    @JvmOverloads
    fun addName(name: TextComponent, useExistingSkin: Boolean = true) {
        val connection = Client.getConnection() ?: return
        val connectionAccessor = connection.asMixin<ClientPlayNetworkHandlerAccessor>()
        val listedPlayerListEntries = connectionAccessor.listedPlayers
        val playerListEntries = connectionAccessor.playerListEntries

        val username = name.unformattedText

        val uuid = UUID.randomUUID()
        val fakeEntry = PlayerInfo(GameProfile(uuid, name.unformattedText), false)
        fakeEntry.tabListDisplayName = name

        listedPlayerListEntries += fakeEntry
        playerListEntries[uuid] = fakeEntry

        if (!useExistingSkin) {
            updateNames()
            return
        }

        val mc = Client.getMinecraft()
        CompletableFuture.supplyAsync { mc.services().profileResolver().fetchByName(username) }.thenAcceptAsync({
            if (it.isPresent) {
                val result = it.get()

                val entry = PlayerInfo(result, true)
                entry.tabListDisplayName = name

                listedPlayerListEntries += entry
                playerListEntries[result.id] = entry

                listedPlayerListEntries -= fakeEntry
                playerListEntries.remove(uuid)

                updateNames()
            }
        }, mc)
    }

    @JvmStatic
    @JvmOverloads
    fun addName(name: String, useExistingSkin: Boolean = true) = addName(TextComponent(name), useExistingSkin)

    /**
     * Removes all names from the tab list with a certain name
     *
     * @param name the name of the entry to remove
     */
    @JvmStatic
    fun removeNames(name: TextComponent) {
        tabListNames.filter {
            it.getName().style == name.style && it.getName().string == name.string
        }.forEach(Name::remove)
    }

    @JvmStatic
    fun removeNames(name: String) {
        tabListNames.filter {
            it.getName().string == name
        }.forEach(Name::remove)
    }

    private fun updateNames() {
        tabListNames.clear()

        if (!customHeader)
            tabListHeader = null

        if (!customFooter)
            tabListFooter = null

        val hud = toMC()?.asMixin<PlayerListHudAccessor>() ?: return
        val player = Player.toMC() ?: return

        if (!customHeader)
            tabListHeader = hud.header?.let { TextComponent(it) }

        if (!customFooter)
            tabListFooter = hud.footer?.let { TextComponent(it) }

        tabListNames = playerComparator
            .sortedCopy(Client.getConnection()?.listedOnlinePlayers ?: emptyList())
            .mapTo(mutableListOf(), ::Name)
    }

    internal fun resetCache() {
        needsUpdate = true
    }

    internal fun clearCustom() {
        tabListNames.forEach(Name::restoreOriginalName)
        tabListNames.clear()
        val (serverHeader, serverFooter) = overrides.clear()
        tabListHeader = serverHeader?.let { TextComponent(it) }
        tabListFooter = serverFooter?.let { TextComponent(it) }
        needsUpdate = true
        toMC()?.apply {
            setHeader(serverHeader)
            setFooter(serverFooter)
        }
    }

    internal fun observeServerHeader(header: Component?): Boolean {
        val cancel = overrides.observeServerHeader(header)
        if (!cancel)
            tabListHeader = header?.let { TextComponent(it) }
        return cancel
    }

    internal fun observeServerFooter(footer: Component?): Boolean {
        val cancel = overrides.observeServerFooter(footer)
        if (!cancel)
            tabListFooter = footer?.let { TextComponent(it) }
        return cancel
    }

    class Name(override val mcValue: PlayerInfo) : CTWrapper<PlayerInfo> {
        private val originalName = mcValue.tabListDisplayName
        private var customName = false
        private val latencyState = BasicState(mcValue.latency)
        private val teamState = BasicState(mcValue.team)
        private val nameState = BasicState(mcValue.tabListDisplayName)

        /**
         * Gets the latency associated with this name
         *
         * @return the latency
         */
        fun getLatency(): Int = latencyState.get()

        /**
         * Sets the latency associated with this name.
         * - latency between 0 and 149 represents all 5 bars
         * - latency between 150 and 299 represents 4 bars
         * - latency between 300 and 599 represents 3 bars
         * - latency between 600 and 999 represents 2 bars
         * - latency between 1000 and more represents 1 bar
         *
         * @param latency the latency to set
         * @return the name to allow for method chaining
         */
        fun setLatency(latency: Int) = apply {
            latencyState.set(latency)
            mcValue.asMixin<PlayerListEntryAccessor>().invokeSetLatency(latency)
        }

        /**
         * Gets the team associated with this name, if it exists
         *
         * @return the team, or null if it does not exist
         */
        fun getTeam(): Team? = teamState.get()?.let(::Team)

        /**
         * Sets the team associated with this name
         *
         * @param team the new team to set for this name. Custom teams can be created
         * using [Scoreboard.createTeam]
         * @return the score to allow for method chaining
         */
        fun setTeam(team: Team?) = apply {
            val scoreboard = Scoreboard.toMC()!!
            val name = mcValue.profile.name

            if (team == null) {
                scoreboard.removePlayerFromTeam(name)
            } else {
                scoreboard.addPlayerToTeam(name, team.toMC())
            }

            teamState.set(team?.toMC())
        }

        /**
         * Gets the display text of this name
         *
         * @return the display name
         */
        fun getName(): TextComponent {
            val name = mcValue.profile.name

            return TextComponent(
                MCTeam.formatNameForTeam(
                    getTeam()?.mcValue,
                    TextComponent(nameState.get() ?: name),
                )
            )
        }

        /**
         * Sets the display name of this name
         *
         * @param name the new name
         * @return the name to allow for method chaining
         */
        fun setName(name: TextComponent?) = apply {
            customName = true
            nameState.set(name)
            mcValue.tabListDisplayName = name
        }

        internal fun restoreOriginalName() {
            if (!customName)
                return
            customName = false
            nameState.set(originalName)
            mcValue.tabListDisplayName = originalName
        }

        /**
         * Removes this name from the tab list
         */
        fun remove() {
            val connection = Client.getConnection() ?: return
            val connectionAccessor = connection.asMixin<ClientPlayNetworkHandlerAccessor>()
            val listedPlayerListEntries = connectionAccessor.listedPlayers
            val playerListEntries = connectionAccessor.playerListEntries

            listedPlayerListEntries.remove(mcValue)
            playerListEntries.remove(mcValue.profile.id)

            updateNames()
        }

        override fun toString(): String = getName().formattedText
    }

    internal class PlayerComparator internal constructor() : Comparator<PlayerInfo> {
        override fun compare(playerOne: PlayerInfo, playerTwo: PlayerInfo): Int {
            val teamOne = playerOne.team
            val teamTwo = playerTwo.team

            return ComparisonChain
                .start()
                .compareTrueFirst(
                    playerOne.gameMode != GameType.SPECTATOR,
                    playerTwo.gameMode != GameType.SPECTATOR
                )
                .compare(teamOne?.name ?: "", teamTwo?.name ?: "")
                .compare(playerOne.profile.name, playerTwo.profile.name)
                .result()
        }
    }
}

internal class TabListOverrideState<T> {
    var customHeader = false
        private set
    var customFooter = false
        private set
    private var applyingHeader = false
    private var applyingFooter = false
    private var serverHeader: T? = null
    private var serverFooter: T? = null

    fun beginCustomHeader() {
        customHeader = true
        applyingHeader = true
    }

    fun endCustomHeader() {
        applyingHeader = false
    }

    fun beginCustomFooter() {
        customFooter = true
        applyingFooter = true
    }

    fun endCustomFooter() {
        applyingFooter = false
    }

    fun observeServerHeader(value: T?): Boolean {
        if (applyingHeader)
            return false
        serverHeader = value
        return customHeader
    }

    fun observeServerFooter(value: T?): Boolean {
        if (applyingFooter)
            return false
        serverFooter = value
        return customFooter
    }

    fun clear(): Pair<T?, T?> {
        customHeader = false
        customFooter = false
        applyingHeader = false
        applyingFooter = false
        return serverHeader to serverFooter
    }
}
