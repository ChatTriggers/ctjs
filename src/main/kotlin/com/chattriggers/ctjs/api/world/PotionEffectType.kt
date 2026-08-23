package com.chattriggers.ctjs.api.world

import com.chattriggers.ctjs.api.message.TextComponent
import net.minecraft.world.effect.MobEffect
import net.minecraft.core.registries.BuiltInRegistries
import java.awt.Color

class PotionEffectType(val type: MobEffect) {
    /**
     * The Int associated with this type
     */
    val rawId get() = BuiltInRegistries.MOB_EFFECT.getId(type)

    /**
     * Whether this effect is instant (e.g. instant health)
     */
    val isInstant get() = type.isInstantenous

    /**
     * The raw key used for this effect type
     */
    val translationKey get() = type.descriptionId

    /**
     * The user-friendly name of this type as a [TextComponent]
     */
    val name get() = TextComponent(type.displayName)

    /**
     * The [net.minecraft.world.effect.MobEffectCategory] of this type
     */
    val category get() = type.category

    /**
     * The color of this type
     */
    val color get() = Color(type.color)
}
