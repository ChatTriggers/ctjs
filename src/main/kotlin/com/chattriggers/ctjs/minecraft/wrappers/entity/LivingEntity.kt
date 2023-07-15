package com.chattriggers.ctjs.minecraft.wrappers.entity

import com.chattriggers.ctjs.minecraft.wrappers.inventory.Item
import com.chattriggers.ctjs.minecraft.wrappers.world.PotionEffect
import com.chattriggers.ctjs.minecraft.wrappers.world.PotionEffectType
import com.chattriggers.ctjs.utils.MCEntity
import com.chattriggers.ctjs.utils.MCLivingEntity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.effect.StatusEffect

open class LivingEntity(override val mcValue: MCLivingEntity) : Entity(mcValue) {
    fun getActivePotionEffects(): List<PotionEffect> {
        return mcValue.statusEffects.map(::PotionEffect)
    }

    fun canSeeEntity(other: MCEntity) = mcValue.canSee(other)

    fun canSeeEntity(other: Entity) = canSeeEntity(other.toMC())

    /**
     * Gets the item currently in the entity's specified inventory slot.
     * 0 for main hand, 1 for offhand, 2-5 for armor
     *
     * @param slot the slot to access
     * @return the item in said slot
     */
     fun getStackInSlot(slot: Int): Item? {
         return mcValue.getEquippedStack(EquipmentSlot.values()[slot])?.let(::Item)
     }

    fun getHP() = mcValue.health

    fun getMaxHP() = mcValue.maxHealth

    fun getAbsorption() = mcValue.absorptionAmount

    fun getAge() = mcValue.age

    fun getArmorValue() = mcValue.armor

    fun isPotionActive(id: Int) = mcValue.hasStatusEffect(StatusEffect.byRawId(id))

    fun isPotionActive(type: PotionEffectType) = mcValue.hasStatusEffect(type.type)

    fun isPotionActive(effect: PotionEffect) = isPotionActive(effect.type)
}
