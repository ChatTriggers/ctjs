package com.chattriggers.ctjs.api.world

import com.chattriggers.ctjs.api.message.TextComponent
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.registry.Registries

/**
 * Represents a specific instance of a [PotionEffectType]
 */
class PotionEffect(val effect: StatusEffectInstance) {
    /**
     * The type of this potion
     */
    val type get() = PotionEffectType(effect.effectType.value())

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
    val localizedName get() = TextComponent(effect.effectType.value().name).unformattedText

    val amplifier get() = effect.amplifier

    val duration get() = effect.duration

    val id get() = Registries.STATUS_EFFECT.getRawId(effect.effectType.value())

    val ambient get() = effect.isAmbient

    val isInfinite get() = effect.isInfinite

    val showsParticles get() = effect.shouldShowParticles()

    override fun toString(): String = effect.toString()
}
