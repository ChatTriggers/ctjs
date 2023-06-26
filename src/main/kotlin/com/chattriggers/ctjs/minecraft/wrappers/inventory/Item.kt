package com.chattriggers.ctjs.minecraft.wrappers.inventory

import com.chattriggers.ctjs.TooltipOverridable
import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer
import com.chattriggers.ctjs.minecraft.wrappers.CTWrapper
import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.minecraft.wrappers.World
import com.chattriggers.ctjs.minecraft.wrappers.entity.Entity
import com.chattriggers.ctjs.minecraft.wrappers.inventory.nbt.NBTTagCompound
import com.chattriggers.ctjs.minecraft.wrappers.world.block.Block
import com.chattriggers.ctjs.minecraft.wrappers.world.block.BlockPos
import com.chattriggers.ctjs.utils.MCNbtCompound
import com.chattriggers.ctjs.utils.asMixin
import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.block.pattern.CachedBlockPosition
import net.minecraft.client.item.TooltipContext
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries

class Item(override val mcValue: ItemStack) : CTWrapper<ItemStack> {
    val type: ItemType = ItemType(mcValue.item)

    fun getHolder(): Entity? = mcValue.holder?.let(Entity::fromMC)

    fun getStackSize(): Int = mcValue.count

    fun getEnchantments() = EnchantmentHelper.get(mcValue).mapKeys {
        EnchantmentHelper.getEnchantmentId(it.key)!!.toTranslationKey()?.replace("enchantment.", "")
    }

    fun isEnchantable() = mcValue.isEnchantable

    fun isEnchanted() = mcValue.hasEnchantments()

    fun canPlaceOn(pos: BlockPos) = mcValue.canPlaceOn(Registries.BLOCK, CachedBlockPosition(World.toMC(), pos.toMC(), false))

    fun canPlaceOn(block: Block) = canPlaceOn(block.pos)

    fun canHarvest(pos: BlockPos) = mcValue.canDestroy(Registries.BLOCK, CachedBlockPosition(World.toMC(), pos.toMC(), false))

    fun canHarvest(block: Block) = canHarvest(block.pos)

    fun getDurability() = getMaxDamage() - getDamage()

    fun getMaxDamage() = mcValue.maxDamage

    fun getDamage() = mcValue.damage

    fun isDamageable() = mcValue.isDamageable

    fun getName(): String = UTextComponent(mcValue.name).formattedText

    @JvmOverloads
    fun getLore(advanced: Boolean = false): List<UTextComponent>? = (getHolder()?.toMC() as? PlayerEntity)?.let {
        mcValue.getTooltip(it, if (advanced) TooltipContext.ADVANCED else TooltipContext.BASIC).map(::UTextComponent)
    }

    fun setLore(lore: List<UTextComponent>) {
        mcValue.asMixin<TooltipOverridable>().apply {
            setTooltip(lore)
            setShouldOverrideTooltip(true)
        }
    }

    fun resetLore() {
        mcValue.asMixin<TooltipOverridable>().setShouldOverrideTooltip(false)
    }

    fun getNBT() = mcValue.nbt?.let(::NBTTagCompound) ?: NBTTagCompound(MCNbtCompound())

    /**
     * Renders the item icon to the client's overlay, with customizable overlay information.
     *
     * @param x the x location
     * @param y the y location
     * @param scale the scale
     * @param z the z level to draw the item at
     */
    @JvmOverloads
    fun draw(x: Float = 0f, y: Float = 0f, scale: Float = 1f, z: Float = 200f) {
        val itemRenderer = Client.getMinecraft().itemRenderer

        Renderer.scale(scale, scale, 1f)
        Renderer.translate(x / scale, y / scale, z)
        Renderer.colorize(1f, 1f, 1f, 1f)

        itemRenderer.renderInGui(Renderer.matrixStack.toMC(), mcValue, 0, 0)

        Renderer.resetTransformsIfNecessary()
    }

    override fun toString(): String = "Item{name=${getName()}, type=${type.getRegistryName()}, size=${getStackSize()}}"
}
