package com.chattriggers.ctjs.minecraft.wrappers

import com.chattriggers.ctjs.minecraft.objects.TextComponent
import com.chattriggers.ctjs.utils.MCScoreboard
import com.chattriggers.ctjs.utils.MCTeam
import gg.essential.universal.utils.MCITextComponent
import net.minecraft.scoreboard.ScoreboardObjective
import net.minecraft.scoreboard.ScoreboardPlayerScore

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
    fun setLine(score: Int, line: String, override: Boolean) {
        val scoreboard = toMC() ?: return
        val sidebarObjective = getSidebar() ?: return

        val scores = scoreboard.getAllPlayerScores(sidebarObjective)

        if (override) {
            scores.filter {
                it.score == score
            }.forEach {
                scoreboard.resetPlayerScore(it.playerName, sidebarObjective)
            }
        }

        scoreboard.getPlayerScore(line, sidebarObjective).score = score
    }

    @JvmStatic
    fun setLine(score: Int, line: TextComponent, override: Boolean) {
        setLine(score, line.formattedText, override)
    }

    @JvmStatic
    fun setShouldRender(shouldRender: Boolean) {
        this.shouldRender = shouldRender
    }

    @JvmStatic
    fun getShouldRender() = shouldRender

    private fun updateNames() {
        scoreboardNames.clear()
        scoreboardTitle = TextComponent("")

        val scoreboard = toMC() ?: return
        val objective = scoreboard.objectives.singleOrNull() ?: return

        scoreboardTitle = TextComponent(objective.displayName)
        scoreboardNames = scoreboard.getAllPlayerScores(objective).filter {
            it.playerName != null && !it.playerName.startsWith("#")
        }.map(::Score).toMutableList()
    }

    internal fun resetCache() {
        needsUpdate = true
    }

    class Score(private val mcValue: ScoreboardPlayerScore) {
        fun toMC() = mcValue

        /**
         * Gets the score point value for this score,
         * i.e. the number on the right of the board
         *
         * @return the actual point value
         */
        fun getPoints(): Int = mcValue.score

        /**
         * Gets the display string of this score
         *
         * @return the display name
         */
        fun getName() = TextComponent(MCTeam.decorateName(
            Scoreboard.toMC()!!.getPlayerTeam(mcValue.playerName),
            TextComponent(mcValue.playerName),
        ))

        override fun toString(): String = getName().formattedText
    }
}
