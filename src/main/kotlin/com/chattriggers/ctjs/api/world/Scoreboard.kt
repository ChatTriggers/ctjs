package com.chattriggers.ctjs.api.world

import com.chattriggers.ctjs.MCTeam
import com.chattriggers.ctjs.api.CTWrapper
import com.chattriggers.ctjs.api.entity.Team
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.internal.mixins.`Scoreboard$1Accessor`
import com.chattriggers.ctjs.internal.utils.ResettableState
import com.chattriggers.ctjs.internal.utils.asMixin
import net.minecraft.scoreboard.ScoreAccess
import net.minecraft.scoreboard.ScoreboardDisplaySlot
import net.minecraft.scoreboard.ScoreboardObjective
import net.minecraft.scoreboard.ScoreboardScore
import net.minecraft.scoreboard.number.NumberFormat
import net.minecraft.scoreboard.number.StyledNumberFormat
import net.minecraft.text.Style
import org.mozilla.javascript.NativeObject

object Scoreboard {
    private var needsUpdate = true
    private var scoreboardNames = mutableListOf<Score>()
    private var scoreboardTitle = TextComponent("")
    private var shouldRender = true
    internal var customTitle = false
    private val customLines = mutableMapOf<Int, ScoreboardScore>()

    @JvmStatic
    fun toMC() = World.toMC()?.scoreboard

    @Deprecated("Use toMC", ReplaceWith("toMC()"))
    @JvmStatic
    fun getScoreboard() = toMC()

    @JvmStatic
    fun getSidebar(): ScoreboardObjective? = toMC()?.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR)

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
        customTitle = false
        getSidebar()?.displayName = title
        scoreboardTitle = title
        customTitle = true
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
            if (scoreboardScore?.score == score) {
                scoreboardScore.displayText = line
                customLines[score] = scoreboardScore
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun setLine(score: Int, line: String, override: Boolean = false) = setLine(score, TextComponent(line), override)

    @JvmStatic
    fun addLine(score: Int, line: TextComponent) {
        val scoreboard = toMC() ?: return
        val sidebarObjective = getSidebar() ?: return

        val newLine = scoreboard.getOrCreateScore({ line.formattedText }, sidebarObjective, true)
        newLine.score = score

        customLines[score] = scoreboard.getScore({ line.formattedText }, sidebarObjective) as ScoreboardScore

        updateNames()
    }

    @JvmStatic
    fun addLine(score: Int, line: String) = addLine(score, TextComponent(line))

    @JvmStatic
    fun removeScores(score: Int) {
        val scoreboard = toMC() ?: return
        val sidebarObjective = getSidebar() ?: return

        scoreboard.knownScoreHolders.forEach {
            val scoreboardScore = scoreboard.getScore({ it.nameForScoreboard }, sidebarObjective) ?: return@forEach
            if (scoreboardScore.score == score) {
                scoreboard.removeScore(it, sidebarObjective)
            }

            if (customLines[score] == scoreboardScore) {
                customLines.remove(score)
            }
        }
        updateNames()
    }

    @JvmStatic
    @JvmOverloads
    fun removeIndex(index: Int, descending: Boolean = true) {
        val scoreboard = toMC() ?: return
        val sidebarObjective = getSidebar() ?: return

        val names = if (descending) scoreboardNames else scoreboardNames.asReversed()
        val line = names.removeAt(index)

        scoreboard.removeScore(line.toMC().asMixin<`Scoreboard$1Accessor`>().holder, sidebarObjective)
        updateNames()
    }

    @JvmStatic
    fun setShouldRender(shouldRender: Boolean) {
        Scoreboard.shouldRender = shouldRender
    }

    @JvmStatic
    fun getShouldRender() = shouldRender

    @JvmStatic
    fun createTeam(name: String): Team = Team(toMC()!!.addTeam(name))

    private fun updateNames() {
        scoreboardNames = scoreboardNames.filter { it.isCustom || it.getPoints() in customLines }.toMutableList()

        if (!customTitle)
            scoreboardTitle = TextComponent("")

        val scoreboard = toMC() ?: return
        val objective = getSidebar() ?: return

        if (!customTitle)
            scoreboardTitle = TextComponent(objective.displayName)

        val newScores = scoreboard.knownScoreHolders.asSequence().filter {
            objective in scoreboard.getScoreHolderObjectives(it)
        }.map {
            scoreboard.getOrCreateScore(it, objective, true)
        }.filter {
            it.score !in customLines
        }.mapTo(mutableListOf(), ::Score)

        scoreboardNames = (scoreboardNames + newScores).sortedBy { it.getPoints() }.toMutableList()
    }

    internal fun resetCache() {
        needsUpdate = true
    }

    internal fun clearCustom() {
        customLines.clear()
        scoreboardNames.clear()
        customTitle = false
        scoreboardTitle = TextComponent("")
    }

    class Score(override val mcValue: ScoreAccess) : CTWrapper<ScoreAccess> {
        private val pointsState = ResettableState(mcValue.score)
        private val nameState = ResettableState(mcValue.displayText)
        private val formatState =
            ResettableState(mcValue.asMixin<`Scoreboard$1Accessor`>().score.numberFormat)
        private val teamState = ResettableState(getTeam())

        internal val isCustom: Boolean
            get() = !nameState.isOriginalValue || !pointsState.isOriginalValue || !formatState.isOriginalValue || !teamState.isOriginalValue

        fun getTeam(): Team? = teamState.get()

        fun setTeam(team: Team?) = apply {
            val scoreboard = Scoreboard.toMC()!!
            val name = mcValue.asMixin<`Scoreboard$1Accessor`>().holder.nameForScoreboard

            if (team == null) {
                scoreboard.clearTeam(name)
            } else {
                scoreboard.addScoreHolderToTeam(name, team.toMC())
            }

            teamState.set(team)
        }

        fun resetTeam() = apply {
            teamState.reset()
        }

        /**
         * Gets the score point value for this score,
         * i.e. the number on the right of the board
         *
         * @return the actual point value
         */
        fun getPoints(): Int = pointsState.get()

        /**
         * Sets the point value for this score
         * @param points the new point value
         *
         * @return the score to allow for method chaining
         */
        fun setPoints(points: Int) = apply {
            pointsState.set(points)
            mcValue.score = points
        }

        fun resetPoints() = apply {
            pointsState.reset()
            mcValue.score = pointsState.get()
        }

        /**
         * Gets the display string of this score
         *
         * @return the display name
         */
        fun getName(): String {
            val name = mcValue.asMixin<`Scoreboard$1Accessor`>().holder.nameForScoreboard

            val component = TextComponent(
                MCTeam.decorateName(
                    getTeam()?.mcValue,
                    TextComponent(nameState.get() ?: name),
                )
            )

            return component.formattedText
        }

        /**
         * Sets the name of this score
         * @param name the new name
         *
         * @return the score to allow for method chaining
         */
        fun setName(name: TextComponent?) = apply {
            nameState.set(name)
            mcValue.displayText = name
        }

        /**
         * Resets the name of this score
         */
        fun resetName() = apply {
            nameState.reset()
            mcValue.displayText = nameState.get()
        }

        fun getNumberFormat(): NumberFormat? = formatState.get()

        fun setNumberFormat(format: Any?) = apply {
            val style = when (format) {
                is CharSequence -> StyledNumberFormat(TextComponent(format.toString()).style)
                is NativeObject -> StyledNumberFormat(TextComponent.jsObjectToStyle(format))
                is NumberFormat -> format
                is Number -> StyledNumberFormat(Style.EMPTY.withColor(format.toInt()))
                else -> null
            }

            if (style?.format(0) != formatState.get()?.format(0)) {
                formatState.set(style)
                mcValue.setNumberFormat(style)
            }
        }

        fun resetNumberFormat() = apply {
            formatState.reset()
            mcValue.setNumberFormat(formatState.get())
        }

        fun reset() = apply {
            resetName()
            resetPoints()
            resetNumberFormat()
            resetTeam()
        }

        fun remove() {
            val scoreboard = Scoreboard.toMC() ?: return
            val sidebarObjective = getSidebar() ?: return

            scoreboard.removeScore(toMC().asMixin<`Scoreboard$1Accessor`>().holder, sidebarObjective)
            scoreboardNames.remove(this)
            updateNames()
        }

        override fun toString(): String = getName()
    }
}
