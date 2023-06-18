package com.chattriggers.ctjs.minecraft.wrappers.entity

import com.chattriggers.ctjs.minecraft.wrappers.world.PotionEffect
import com.chattriggers.ctjs.minecraft.wrappers.world.PotionEffectType
import com.chattriggers.ctjs.utils.MCEntity
import com.chattriggers.ctjs.utils.MCLivingEntity
import net.minecraft.entity.effect.StatusEffect

// TODO(breaking): Rename from EntityLivingBase
open class LivingEntity(override val mcValue: MCLivingEntity) : Entity(mcValue) {
    // TODO(breaking): Remove addPotionEffect

    // TODO(breaking): Remove clearPotionEffects

    fun getActivePotionEffects(): List<PotionEffect> {
        return mcValue.statusEffects.map(::PotionEffect)
    }

    fun canSeeEntity(other: MCEntity) = mcValue.canSee(other)

    fun canSeeEntity(other: Entity) = canSeeEntity(other.toMC())

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

    fun getHP() = mcValue.health

    // TODO(breaking): Remove setHP

    fun getMaxHP() = mcValue.maxHealth

    fun getAbsorption() = mcValue.absorptionAmount

    // TODO(breaking): Remove setAbsorption

    fun getAge() = mcValue.age

    fun getArmorValue() = mcValue.armor

    fun isPotionActive(id: Int) = mcValue.hasStatusEffect(StatusEffect.byRawId(id))

    fun isPotionActive(type: PotionEffectType) = mcValue.hasStatusEffect(type.type)

    fun isPotionActive(effect: PotionEffect) = isPotionActive(effect.type)

    override fun toString() = "LivingEntity{name=${getName()}, entity=${super.toString()}}"
}
