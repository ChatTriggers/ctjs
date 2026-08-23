package com.chattriggers.ctjs.api.entity

import com.chattriggers.ctjs.api.inventory.Item
import com.chattriggers.ctjs.api.world.PotionEffect
import com.chattriggers.ctjs.api.world.PotionEffectType
import com.chattriggers.ctjs.MCEntity
import com.chattriggers.ctjs.MCLivingEntity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.core.registries.BuiltInRegistries

open class LivingEntity(override val mcValue: MCLivingEntity) : Entity(mcValue) {
    fun getActivePotionEffects(): List<PotionEffect> {
        return mcValue.activeEffects.map(::PotionEffect)
    }

    fun canSeeEntity(other: MCEntity) = mcValue.hasLineOfSight(other)

    fun canSeeEntity(other: Entity) = canSeeEntity(other.toMC())

    /**
     * Gets the item currently in the entity's specified inventory slot.
     * 0 for main hand, 1 for offhand, 2-5 for armor
     *
     * @param slot the slot to access
     * @return the item in said slot
     */
    fun getStackInSlot(slot: Int): Item? {
        return mcValue.getItemBySlot(EquipmentSlot.entries[slot]).let(Item::fromMC)
    }

    fun getHP() = mcValue.health

    fun getMaxHP() = mcValue.maxHealth

    fun getAbsorption() = mcValue.absorptionAmount

    fun getAge() = mcValue.tickCount

    fun getArmorValue() = mcValue.armorValue

    fun isPotionActive(id: Int) = mcValue.hasEffect(BuiltInRegistries.MOB_EFFECT.get(id).orElseThrow())

    fun isPotionActive(type: PotionEffectType) = mcValue.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(type.type))

    fun isPotionActive(effect: PotionEffect) = isPotionActive(effect.type)
}
