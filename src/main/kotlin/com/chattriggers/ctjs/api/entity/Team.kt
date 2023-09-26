package com.chattriggers.ctjs.api.entity

import com.chattriggers.ctjs.api.CTWrapper
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.internal.utils.MCTeam
import net.minecraft.scoreboard.AbstractTeam

class Team(override val mcValue: MCTeam) : CTWrapper<MCTeam> {
    /**
     * Gets the registered name of the team
     */
    fun getRegisteredName(): String = mcValue.name

    /**
     * Gets the display name of the team
     */
    fun getName() = TextComponent(mcValue.displayName).formattedText

    /**
     * Sets the display name of the team
     * @param name the new display name
     * @return the team for method chaining
     */
    fun setName(name: TextComponent) = apply {
        mcValue.displayName = name
    }

    /**
     * Sets the display name of the team
     * @param name the new display name
     * @return the team for method chaining
     */
    fun setName(name: String) = setName(TextComponent(name))

    /**
     * Gets the list of names on the team
     */
    fun getMembers(): List<String> = mcValue.playerList.toList()

    /**
     * Gets the team prefix
     */
    fun getPrefix() = TextComponent(mcValue.prefix).formattedText

    /**
     * Sets the team prefix
     * @param prefix the prefix to set
     * @return the team for method chaining
     */
    fun setPrefix(prefix: TextComponent) = apply {
        mcValue.prefix = prefix
    }

    /**
     * Sets the team prefix
     * @param prefix the prefix to set
     * @return the team for method chaining
     */
    fun setPrefix(prefix: String) = setPrefix(TextComponent(prefix))

    /**
     * Gets the team suffix
     */
    fun getSuffix() = TextComponent(mcValue.suffix).formattedText

    /**
     * Sets the team suffix
     * @param suffix the suffix to set
     * @return the team for method chaining
     */
    fun setSuffix(suffix: TextComponent) = apply {
        mcValue.suffix = suffix
    }

    /**
     * Sets the team suffix
     * @param suffix the suffix to set
     * @return the team for method chaining
     */
    fun setSuffix(suffix: String) = setSuffix(TextComponent(suffix))

    /**
     * Gets the team's friendly fire setting
     */
    fun getFriendlyFire(): Boolean = mcValue.isFriendlyFireAllowed

    /**
     * Gets whether the team can see invisible players on the same team
     */
    fun canSeeInvisibleTeammates(): Boolean = mcValue.shouldShowFriendlyInvisibles()

    /**
     * Gets the team's name tag visibility
     */
    fun getNameTagVisibility() = Visibility.fromMC(mcValue.nameTagVisibilityRule)

    /**
     * Gets the team's death message visibility
     */
    fun getDeathMessageVisibility() = Visibility.fromMC(mcValue.deathMessageVisibilityRule)

    enum class Visibility(override val mcValue: AbstractTeam.VisibilityRule) : CTWrapper<AbstractTeam.VisibilityRule> {
        ALWAYS(AbstractTeam.VisibilityRule.ALWAYS),
        NEVER(AbstractTeam.VisibilityRule.NEVER),
        HIDE_FOR_OTHERS_TEAMS(AbstractTeam.VisibilityRule.HIDE_FOR_OTHER_TEAMS),
        HIDE_FOR_OWN_TEAM(AbstractTeam.VisibilityRule.HIDE_FOR_OWN_TEAM);

        companion object {
            @JvmStatic
            fun fromMC(mcValue: AbstractTeam.VisibilityRule) = values().first { it.mcValue == mcValue }
        }
    }
}
