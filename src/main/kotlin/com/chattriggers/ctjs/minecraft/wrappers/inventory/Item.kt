package com.chattriggers.ctjs.minecraft.wrappers.inventory

import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer
import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.minecraft.wrappers.World
import com.chattriggers.ctjs.minecraft.wrappers.entity.Entity
import com.chattriggers.ctjs.minecraft.wrappers.inventory.nbt.NBTTagCompound
import com.chattriggers.ctjs.minecraft.wrappers.world.block.Block
import com.chattriggers.ctjs.minecraft.wrappers.world.block.BlockPos
import com.chattriggers.ctjs.mixins.ItemStackMixin
import com.chattriggers.ctjs.utils.MCNbtCompound
import com.chattriggers.ctjs.utils.asMixin
import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.block.pattern.CachedBlockPosition
import net.minecraft.client.item.TooltipContext
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries

// TODO(breaking): Completely redid this API
class Item(val stack: ItemStack) {
    val type: ItemType = ItemType(stack.item)

    fun getHolder(): Entity? = stack.holder?.let(Entity::fromMC)

    fun getStackSize(): Int = stack.count

    // TODO: Api that returns the Enchantment object?
    fun getEnchantments() = EnchantmentHelper.get(stack).mapKeys {
        EnchantmentHelper.getEnchantmentId(it.key)!!.toTranslationKey()?.replace("enchantment.", "")
    }

    fun isEnchantable() = stack.isEnchantable

    // TODO(breaking): fix typo in method name
    fun isEnchanted() = stack.hasEnchantments()

    fun canPlaceOn(pos: BlockPos) = stack.canPlaceOn(Registries.BLOCK, CachedBlockPosition(World.toMC(), pos.toMC(), false))

    fun canPlaceOn(block: Block) = canPlaceOn(block.pos)

    fun canHarvest(pos: BlockPos) = stack.canDestroy(Registries.BLOCK, CachedBlockPosition(World.toMC(), pos.toMC(), false))

    fun canHarvest(block: Block) = canHarvest(block.pos)

    fun getDurability() = getMaxDamage() - getDamage()

    fun getMaxDamage() = stack.maxDamage

    fun getDamage() = stack.damage

    // TODO(breaking): Rename isDamagable to isDamageable
    fun isDamageable() = stack.isDamageable

    fun getName(): String = UTextComponent(stack.name).formattedText

    @JvmOverloads
    fun getLore(advanced: Boolean = false): List<UTextComponent>? = (getHolder()?.toMC() as? PlayerEntity)?.let {
        stack.getTooltip(it, if (advanced) TooltipContext.ADVANCED else TooltipContext.BASIC).map(::UTextComponent)
    }

    fun setLore(lore: List<UTextComponent>) {
        stack.asMixin<ItemStackMixin>().apply {
            overriddenTooltip = lore
            overrideTooltip = true
        }
    }

    fun resetLore() {
        stack.asMixin<ItemStackMixin>().overrideTooltip = false
    }

    // TODO(breaking): Removed getRawNBT - was useless

    fun getNBT() = stack.nbt?.let(::NBTTagCompound) ?: NBTTagCompound(MCNbtCompound())

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
        Renderer.translate(x / scale, y / scale, 0f)
        Renderer.colorize(1f, 1f, 1f, 1f)

        // TODO: Removed a few method calls from the old version, make sure they don't really
        //       affect anything
        itemRenderer.renderInGui(Renderer.matrixStack.toMC(), stack, 0, 0)

        Renderer.resetTransformsIfNecessary()
    }

    override fun toString(): String = "Item{name=${getName()}, type=${type.getRegistryName()}, size=${getStackSize()}}"
}
