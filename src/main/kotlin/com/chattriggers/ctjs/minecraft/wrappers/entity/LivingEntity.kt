package com.chattriggers.ctjs.minecraft.wrappers.entity

import com.chattriggers.ctjs.minecraft.wrappers.world.PotionEffect
import com.chattriggers.ctjs.minecraft.wrappers.world.PotionEffectType
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.Entity as MCEntity
import net.minecraft.entity.LivingEntity as MCLivingEntity

// TODO(breaking): Rename from EntityLivingBase
open class LivingEntity(val livingEntity: MCLivingEntity) : Entity(livingEntity) {
    fun addPotionEffect(effect: PotionEffect) {
        livingEntity.addStatusEffect(effect.effect)
    }

    fun clearPotionEffects() {
        livingEntity.clearStatusEffects()
    }

    fun getActivePotionEffects(): List<PotionEffect> {
        return livingEntity.statusEffects.map(::PotionEffect)
    }

    fun canSeeEntity(other: MCEntity) = livingEntity.canSee(other)

    fun canSeeEntity(other: Entity) = canSeeEntity(other.entity)

    /**
     * Gets the item currently in the entity's specified inventory slot.
     * 0 for main hand, 1-4 for armor
     * (2 for offhand in 1.12.2, and everything else shifted over).
     *
     * @param slot the slot to access
     * @return the item in said slot
     */
    // fun getItemInSlot(slot: Int): Item? {
    //     return entityLivingBase.getItemStackFromSlot(EntityEquipmentSlot.values()[slot])?.let(::Item)
    // }

    fun getHP() = livingEntity.health

    fun setHP(health: Float) = apply {
        livingEntity.health = health
    }

    fun getMaxHP() = livingEntity.maxHealth

    fun getAbsorption() = livingEntity.absorptionAmount

    fun setAbsorption(absorption: Float) = apply {
        livingEntity.absorptionAmount = absorption
    }

    fun getAge() = livingEntity.age

    fun getArmorValue() = livingEntity.armor

    fun isPotionActive(id: Int) = livingEntity.hasStatusEffect(StatusEffect.byRawId(id))

    fun isPotionActive(type: PotionEffectType) = livingEntity.hasStatusEffect(type.type)

    fun isPotionActive(effect: PotionEffect) = isPotionActive(effect.type)

    override fun toString() = "LivingEntity{name=${getName()}, entity=${super.toString()}}"
}
