package com.chattriggers.ctjs.api.world

import com.chattriggers.ctjs.api.message.TextComponent
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.registry.Registries
import java.awt.Color

class PotionEffectType(val type: StatusEffect) {
    /**
     * The Int associated with this type
     */
    val rawId get() = Registries.STATUS_EFFECT.getRawId(type)

    /**
     * Whether this effect is instant (e.g. instant health)
     */
    val isInstant get() = type.isInstant

    /**
     * The raw key used for this effect type
     */
    val translationKey get() = type.translationKey

    /**
     * The user-friendly name of this type as a [TextComponent]
     */
    val name get() = TextComponent(type.name)

    /**
     * The [net.minecraft.entity.effect.StatusEffectCategory] of this type
     */
    val category get() = type.category

    /**
     * The color of this type
     */
    val color get() = Color(type.color)
}
