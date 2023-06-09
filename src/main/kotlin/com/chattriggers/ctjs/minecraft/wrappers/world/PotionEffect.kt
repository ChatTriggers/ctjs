package com.chattriggers.ctjs.minecraft.wrappers.world

import com.chattriggers.ctjs.minecraft.objects.TextComponent
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance

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
    val localizedName get() = TextComponent(effect.effectType.name).unformattedText

    val amplifier get() = effect.amplifier

    val duration get() = effect.duration

    val id get() = StatusEffect.getRawId(effect.effectType)

    val ambient get() = effect.isAmbient

    val isInfinite get() = effect.isInfinite

    val showsParticles get() = effect.shouldShowParticles()

    override fun toString(): String = effect.toString()
}
