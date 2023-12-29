package com.chattriggers.ctjs.api.world

import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.MCTeam
import net.minecraft.scoreboard.ScoreboardEntry
import net.minecraft.scoreboard.ScoreboardObjective
import net.minecraft.scoreboard.ScoreboardScore

object Scoreboard {
    private var needsUpdate = true
    private var scoreboardNames = mutableListOf<Score>()
    private var scoreboardTitle = TextComponent("")
    private var shouldRender = true

    @JvmStatic
    fun toMC() = World.toMC()?.scoreboard

    @Deprecated("Use toMC", ReplaceWith("toMC()"))
    @JvmStatic
    fun getScoreboard() = toMC()

    @JvmStatic
    fun getSidebar(): ScoreboardObjective? = toMC()?.objectives?.firstOrNull()

    /**
     * Gets the top-most string which is displayed on the scoreboard. (doesn't have a score on the side).
     * Be aware that this can contain color codes.
     *
     * @return the scoreboard title
     */
    @JvmStatic
    fun getTitle(): TextComponent {
        if (needsUpdate) {
            updateNames()
            needsUpdate = false
        }

        return scoreboardTitle
    }

    /**
     * Sets the scoreboard title.
     *
     * @param title the new title
     * @return the scoreboard title
     */
    @JvmStatic
    fun setTitle(title: TextComponent) {
        getSidebar()?.displayName = title
    }

    @JvmStatic
    fun setTitle(title: String) = setTitle(TextComponent(title))

    /**
     * Get all currently visible strings on the scoreboard. (excluding title)
     * Be aware that this can contain color codes.
     *
     * @return the list of lines
     */
    @JvmStatic
    @JvmOverloads
    fun getLines(descending: Boolean = true): List<Score> {
        // the array will only be updated upon request
        if (needsUpdate) {
            updateNames()
            needsUpdate = false
        }

        return if (descending) scoreboardNames else scoreboardNames.asReversed()
    }

    /**
     * Gets the line at the specified index (0 based)
     * Equivalent to Scoreboard.getLines().get(index)
     *
     * @param index the line index
     * @return the score object at the index
     */
    @JvmStatic
    fun getLineByIndex(index: Int): Score = getLines()[index]

    /**
     * Gets a list of lines that have a certain score,
     * i.e. the numbers shown on the right
     *
     * @param score the score to look for
     * @return a list of actual score objects
     */
    @JvmStatic
    fun getLinesByScore(score: Int): List<Score> = getLines().filter {
        it.getPoints() == score
    }

    /**
     * Sets a line in the scoreboard to the specified name and score.
     *
     * @param score the score value for this item
     * @param line the string to display on said line
     * @param override whether to remove old lines with the same score
     */
    @JvmStatic
    @JvmOverloads
    fun setLine(score: Int, line: TextComponent, override: Boolean = false) {
        val scoreboard = toMC() ?: return
        val sidebarObjective = getSidebar() ?: return

        if (override) {
            scoreboard.getScoreboardEntries(sidebarObjective).filter {
                it.value == score
            }.forEach {
                scoreboard.removeScore({ it.owner }, sidebarObjective)
            }
        }

        scoreboard.knownScoreHolders.forEach {
            val scoreboardScore = scoreboard.getScore({ it.nameForScoreboard }, sidebarObjective) as? ScoreboardScore
            if (scoreboardScore?.score == score)
                scoreboardScore.displayText = line
        }
    }

    @JvmStatic
    @JvmOverloads
    fun setLine(score: Int, line: String, override: Boolean = false) = setLine(score, TextComponent(line), override)

    @JvmStatic
    fun setShouldRender(shouldRender: Boolean) {
        Scoreboard.shouldRender = shouldRender
    }

    @JvmStatic
    fun getShouldRender() = shouldRender

    private fun updateNames() {
        scoreboardNames.clear()
        scoreboardTitle = TextComponent("")

        val scoreboard = toMC() ?: return
        val objective = scoreboard.objectives.singleOrNull() ?: return

        scoreboardTitle = TextComponent(objective.displayName)
        scoreboardNames = scoreboard.getScoreboardEntries(objective).filter {
            it.owner != null && !it.owner.startsWith("#")
        }.map(::Score).sortedBy { it.getPoints() }.toMutableList()
    }

    internal fun resetCache() {
        needsUpdate = true
    }

    class Score(private val mcValue: ScoreboardEntry) {
        fun toMC() = mcValue

        /**
         * Gets the score point value for this score,
         * i.e. the number on the right of the board
         *
         * @return the actual point value
         */
        fun getPoints(): Int = mcValue.value

        /**
         * Gets the display string of this score
         *
         * @return the display name
         */
        fun getName() = TextComponent(
            MCTeam.decorateName(
                Scoreboard.toMC()!!.getScoreHolderTeam(mcValue.owner),
                TextComponent(mcValue.owner),
            )
        )

        override fun toString(): String = getName().formattedText
    }
}
