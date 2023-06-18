package com.chattriggers.ctjs.minecraft.wrappers.entity

import com.chattriggers.ctjs.minecraft.wrappers.CTWrapper
import com.chattriggers.ctjs.utils.MCTeam
import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.scoreboard.AbstractTeam

class Team(override val mcValue: MCTeam) : CTWrapper<MCTeam> {
    /**
     * Gets the registered name of the team
     */
    fun getRegisteredName(): String = mcValue.name

    /**
     * Gets the display name of the team
     */
    fun getName() = UTextComponent(mcValue.displayName).formattedText

    /**
     * Sets the display name of the team
     * @param name the new display name
     * @return the team for method chaining
     */
    fun setName(name: UTextComponent) = apply {
        mcValue.displayName = name
    }

    /**
     * Sets the display name of the team
     * @param name the new display name
     * @return the team for method chaining
     */
    fun setName(name: String) = setName(UTextComponent(name))

    /**
     * Gets the list of names on the team
     */
    fun getMembers(): List<String> = mcValue.playerList.toList()

    /**
     * Gets the team prefix
     */
    fun getPrefix() = UTextComponent(mcValue.prefix).formattedText

    /**
     * Sets the team prefix
     * @param prefix the prefix to set
     * @return the team for method chaining
     */
    fun setPrefix(prefix: UTextComponent) = apply {
        mcValue.prefix = prefix
    }

    /**
     * Sets the team prefix
     * @param prefix the prefix to set
     * @return the team for method chaining
     */
    fun setPrefix(prefix: String) = setPrefix(UTextComponent(prefix))

    /**
     * Gets the team suffix
     */
    fun getSuffix() = UTextComponent(mcValue.suffix).formattedText

    /**
     * Sets the team suffix
     * @param suffix the suffix to set
     * @return the team for method chaining
     */
    fun setSuffix(suffix: UTextComponent) = apply {
        mcValue.suffix = suffix
    }

    /**
     * Sets the team suffix
     * @param suffix the suffix to set
     * @return the team for method chaining
     */
    fun setSuffix(suffix: String) = setSuffix(UTextComponent(suffix))

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
    // TODO(breaking): Use enum instead of String
    fun getNameTagVisibility() = Visibility.fromMC(mcValue.nameTagVisibilityRule)

    /**
     * Gets the team's death message visibility
     */
    // TODO(breaking): Use enum instead of String
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
