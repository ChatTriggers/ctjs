package com.chattriggers.ctjs.api.world

import com.chattriggers.ctjs.api.message.TextComponent
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.core.registries.BuiltInRegistries

/**
 * Represents a specific instance of a [PotionEffectType]
 */
class PotionEffect(val effect: MobEffectInstance) {
    /**
     * The type of this potion
     */
    val type get() = PotionEffectType(effect.effect.value())

    /**
     * Returns the translation key of the potion.
     * Ex: "potion.poison"
     */
    val name get() = effect.descriptionId

    /**
     * Returns the localized name of the potion that
     * is displayed in the player's inventory.
     * Ex: "Poison"
     */
    val localizedName get() = TextComponent(effect.effect.value().displayName).unformattedText

    val amplifier get() = effect.amplifier

    val duration get() = effect.duration

    val id get() = BuiltInRegistries.MOB_EFFECT.getId(effect.effect.value())

    val ambient get() = effect.isAmbient

    val isInfinite get() = effect.isInfiniteDuration

    val showsParticles get() = effect.isVisible

    override fun toString(): String = effect.toString()
}
