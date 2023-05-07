package com.chattriggers.ctjs.minecraft.wrappers.world

import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance

// TODO(breaking): Redid this API a bit
/**
 * Represents a specific instance of a [PotionEffectType]
 */
class PotionEffect(val effect: StatusEffectInstance) {
    /**
     * The type of this potion
     */
    val type get() = PotionEffectType(effect.effectType)

    /**
     * Returns the translation key of the potion.
     * Ex: "potion.poison"
     */
    val name get() = effect.translationKey

    /**
     * Returns the localized name of the potion that
     * is displayed in the player's inventory.
     * Ex: "Poison"
     */
    val localizedName get() = UTextComponent(effect.effectType.name).unformattedText

    val amplifier get() = effect.amplifier

    val duration get() = effect.duration

    val id get() = StatusEffect.getRawId(effect.effectType)

    val ambient get() = effect.isAmbient

    // TODO(breaking): Renamed this method
    val isInfinite get() = effect.isInfinite

    val showsParticles get() = effect.shouldShowParticles()

    override fun toString(): String = effect.toString()
}
