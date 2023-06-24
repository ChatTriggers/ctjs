package com.chattriggers.ctjs.minecraft.wrappers.entity

import com.chattriggers.ctjs.minecraft.wrappers.inventory.Item
import com.chattriggers.ctjs.minecraft.wrappers.world.PotionEffect
import com.chattriggers.ctjs.minecraft.wrappers.world.PotionEffectType
import com.chattriggers.ctjs.utils.MCEntity
import com.chattriggers.ctjs.utils.MCLivingEntity
import net.minecraft.entity.EquipmentSlot
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

    // TODO(breaking): Rename to getStackInSlot to have same name as Inventory method
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
