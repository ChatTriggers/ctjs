package com.chattriggers.ctjs.minecraft.wrappers.entity

import com.chattriggers.ctjs.minecraft.libs.ChatLib
import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.scoreboard.Team as MCTeam

class Team(val team: MCTeam) {
    /**
     * Gets the registered name of the team
     */
    fun getRegisteredName(): String = team.name

    /**
     * Gets the display name of the team
     */
    // TODO(breaking): Changed from String to UTextComponent
    fun getName() = UTextComponent(team.displayName)

    /**
     * Sets the display name of the team
     * @param name the new display name
     * @return the team for method chaining
     */
    // TODO(breaking): Changed from String to UTextComponent
    fun setName(name: UTextComponent) = apply {
        team.displayName = name
    }

    /**
     * Gets the list of names on the team
     */
    fun getMembers(): List<String> = team.playerList.toList()

    /**
     * Gets the team prefix
     */
    // TODO(breaking): Changed from String to UTextComponent
    fun getPrefix() = UTextComponent(team.prefix)

    /**
     * Sets the team prefix
     * @param prefix the prefix to set
     * @return the team for method chaining
     */
    // TODO(breaking): Changed from String to UTextComponent
    fun setPrefix(prefix: UTextComponent) = apply {
        team.prefix = prefix
    }

    /**
     * Gets the team suffix
     */
    // TODO(breaking): Changed from String to UTextComponent
    fun getSuffix() = UTextComponent(team.suffix)

    /**
     * Sets the team suffix
     * @param suffix the suffix to set
     * @return the team for method chaining
     */
    // TODO(breaking): Changed from String to UTextComponent
    fun setSuffix(suffix: UTextComponent) = apply {
        team.suffix = suffix
    }

    /**
     * Gets the team's friendly fire setting
     */
    fun getFriendlyFire(): Boolean = team.isFriendlyFireAllowed

    /**
     * Gets whether the team can see invisible players on the same team
     */
    fun canSeeInvisibleTeammates(): Boolean = team.shouldShowFriendlyInvisibles()

    /**
     * Gets the team's name tag visibility
     */
    // TODO(breaking): Use enum instead of String
    fun getNameTagVisibility() = team.nameTagVisibilityRule

    /**
     * Gets the team's death message visibility
     */
    // TODO(breaking): Use enum instead of String
    fun getDeathMessageVisibility() = team.deathMessageVisibilityRule
}
