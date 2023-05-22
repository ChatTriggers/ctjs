package com.chattriggers.ctjs.minecraft.wrappers.entity

import com.chattriggers.ctjs.minecraft.wrappers.world.PotionEffect
import com.chattriggers.ctjs.minecraft.wrappers.world.PotionEffectType
import com.chattriggers.ctjs.utils.MCEntity
import com.chattriggers.ctjs.utils.MCLivingEntity
import net.minecraft.entity.effect.StatusEffect

// TODO(breaking): Rename from EntityLivingBase
open class LivingEntity(val livingEntity: MCLivingEntity) : Entity(livingEntity) {
    // TODO(breaking): Remove addPotionEffect

    // TODO(breaking): Remove clearPotionEffects

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
//     fun getItemInSlot(slot: Int): Item? {
//         return livingEntity.getItemStackFromSlot(EntityEquipmentSlot.values()[slot])?.let(::Item)
//     }

    fun getHP() = livingEntity.health

    // TODO(breaking): Remove setHP

    fun getMaxHP() = livingEntity.maxHealth

    fun getAbsorption() = livingEntity.absorptionAmount

    // TODO(breaking): Remove setAbsorption

    fun getAge() = livingEntity.age

    fun getArmorValue() = livingEntity.armor

    fun isPotionActive(id: Int) = livingEntity.hasStatusEffect(StatusEffect.byRawId(id))

    fun isPotionActive(type: PotionEffectType) = livingEntity.hasStatusEffect(type.type)

    fun isPotionActive(effect: PotionEffect) = isPotionActive(effect.type)

    override fun toString() = "LivingEntity{name=${getName()}, entity=${super.toString()}}"
}
