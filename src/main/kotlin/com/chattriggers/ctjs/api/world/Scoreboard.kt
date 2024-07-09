package com.chattriggers.ctjs.api.world

import com.chattriggers.ctjs.MCTeam
import com.chattriggers.ctjs.api.CTWrapper
import com.chattriggers.ctjs.api.entity.Team
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.internal.mixins.`Scoreboard$1Accessor`
import com.chattriggers.ctjs.internal.utils.asMixin
import gg.essential.elementa.state.BasicState
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
        it.getScore() == score
    }

    /**
     * Sets a line in the scoreboard to the specified name and score.
     *
     * @param score the score value for this item
     * @param line the [TextComponent] to display on said line
     * @param override whether to remove old lines with the same score
     */
    @JvmStatic
    @JvmOverloads
    fun setLine(score: Int, line: TextComponent, override: Boolean = false) {
        val scoreboard = toMC() ?: return
        val sidebarObjective = getSidebar() ?: return

        if (override) {
            removeScores(score)
            addLine(score, line)
            return
        }

        scoreboard.knownScoreHolders.forEach {
            val scoreboardScore = scoreboard.getScore({ it.nameForScoreboard }, sidebarObjective) as? ScoreboardScore
            if (scoreboardScore?.score == score) {
                scoreboardScore.displayText = line
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun setLine(score: Int, line: String, override: Boolean = false) = setLine(score, TextComponent(line), override)

    /**
     * Adds a line to the scoreboard
     *
     * @param score the score value for this item
     * @param line the [TextComponent] to display on said line
     */
    @JvmStatic
    fun addLine(score: Int, line: TextComponent) {
        val scoreboard = toMC() ?: return
        val sidebarObjective = getSidebar() ?: return

        val newLine = scoreboard.getOrCreateScore({ Math.random().toString() }, sidebarObjective, true)
        newLine.displayText = line
        newLine.score = score

        updateNames()
    }

    @JvmStatic
    fun addLine(score: Int, line: String) = addLine(score, TextComponent(line))

    /**
     * Removes all lines from the scoreboard matching with a certain score
     *
     * @param score the score of the lines to remove
     */
    @JvmStatic
    fun removeScores(score: Int) {
        getLinesByScore(score).forEach(Score::remove)
    }

    /**
     * Removes the line at a certain index
     *
     * @param index the index of the line to remove
     */
    @JvmStatic
    @JvmOverloads
    fun removeIndex(index: Int, descending: Boolean = true) {
        val names = if (descending) scoreboardNames else scoreboardNames.asReversed()
        val line = names.removeAt(index)
        line.remove()
    }

    @JvmStatic
    fun setShouldRender(shouldRender: Boolean) {
        Scoreboard.shouldRender = shouldRender
    }

    @JvmStatic
    fun getShouldRender() = shouldRender

    /**
     * Creates or gets a [Team] with a given name
     *
     * @param name the name of the team
     */
    @JvmStatic
    fun createTeam(name: String): Team = Team(toMC()!!.addTeam(name))

    private fun updateNames() {
        scoreboardNames.clear()

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
        }.mapTo(mutableListOf(), ::Score)

        scoreboardNames = newScores.sortedWith(compareBy<Score> {
            it.getScore()
        }.reversed().thenBy {
            it.getName().formattedText.lowercase()
        }).toMutableList()
    }

    internal fun resetCache() {
        needsUpdate = true
    }

    internal fun clearCustom() {
        scoreboardNames.clear()
        customTitle = false
        scoreboardTitle = TextComponent("")
    }

    class Score(override val mcValue: ScoreAccess) : CTWrapper<ScoreAccess> {
        private val scoreState = BasicState(mcValue.score)
        private val nameState = BasicState(mcValue.displayText)
        private val formatState = BasicState(mcValue.asMixin<`Scoreboard$1Accessor`>().score.numberFormat)
        private val teamState = run {
            val scoreboard = Scoreboard.toMC()!!
            val name = mcValue.asMixin<`Scoreboard$1Accessor`>().holder.nameForScoreboard

            BasicState(scoreboard.getScoreHolderTeam(name))
        }

        /**
         * Gets the team associated with this score, if it exists
         *
         * @return the team, or null if it does not exist
         */
        fun getTeam(): Team? = teamState.get()?.let(::Team)

        /**
         * Sets the team associated with this score
         *
         * @param team the new team to set for this line. Custom teams can be created using [createTeam]
         * @return the score to allow for method chaining
         */
        fun setTeam(team: Team?) = apply {
            val scoreboard = Scoreboard.toMC()!!
            val name = mcValue.asMixin<`Scoreboard$1Accessor`>().holder.nameForScoreboard

            if (team == null) {
                scoreboard.clearTeam(name)
            } else {
                scoreboard.addScoreHolderToTeam(name, team.toMC())
            }

            teamState.set(team?.toMC())
        }

        /**
         * Gets the score value for this score,
         * i.e. the number on the right of the board
         *
         * @return the actual point value
         */
        fun getScore(): Int = scoreState.get()

        /**
         * Sets the score value for this score
         *
         * @param score the new point value
         * @return the score to allow for method chaining
         */
        fun setScore(score: Int) = apply {
            scoreState.set(score)
            mcValue.score = score
        }

        /**
         * Gets the display text of this score
         *
         * @return the display name
         */
        fun getName(): TextComponent {
            val name = mcValue.asMixin<`Scoreboard$1Accessor`>().holder.nameForScoreboard

            return TextComponent(
                MCTeam.decorateName(
                    getTeam()?.mcValue,
                    TextComponent(nameState.get() ?: name),
                )
            )
        }

        /**
         * Sets the name of this score
         *
         * @param name the new name
         * @return the score to allow for method chaining
         */
        fun setName(name: TextComponent?) = apply {
            nameState.set(name)
            mcValue.displayText = name
        }

        /**
         * Gets the number format of this score
         *
         * @return the number format
         */
        fun getNumberFormat(): NumberFormat? = formatState.get()

        /**
         * Sets the number format of this score
         *
         * @param format either a formatting string, i.e. "&6", style in the form of an object, see [TextComponent], a
         * [NumberFormat], or hex value
         * @return the score to allow for method chaining
         *
         * @see [TextComponent]
         */
        fun setNumberFormat(format: Any?) = apply {
            val style = when (format) {
                is CharSequence -> StyledNumberFormat(TextComponent(format.toString()).style)
                is NativeObject -> StyledNumberFormat(TextComponent.jsObjectToStyle(format))
                is NumberFormat -> format
                is Number -> StyledNumberFormat(Style.EMPTY.withColor(format.toInt()))
                else -> null
            }

            formatState.set(style)
            mcValue.setNumberFormat(style)
        }

        /**
         * Removes this score from the scoreboard
         */
        fun remove() {
            val scoreboard = Scoreboard.toMC() ?: return
            val sidebarObjective = getSidebar() ?: return

            scoreboard.removeScore(toMC().asMixin<`Scoreboard$1Accessor`>().holder, sidebarObjective)
            updateNames()
        }

        override fun toString(): String = getName().formattedText
    }
}
