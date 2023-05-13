package com.chattriggers.ctjs.minecraft.wrappers.inventory

import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer
import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.minecraft.wrappers.World
import com.chattriggers.ctjs.minecraft.wrappers.entity.Entity
import com.chattriggers.ctjs.minecraft.wrappers.world.block.Block
import com.chattriggers.ctjs.minecraft.wrappers.world.block.BlockPos
import com.chattriggers.ctjs.mixins.ItemStackMixin
import com.chattriggers.ctjs.utils.asMixin
import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.block.pattern.CachedBlockPosition
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries

// TODO(breaking): Completely redid this API
class Item(val stack: ItemStack) {
    val type: ItemType = ItemType(stack.item)

    fun getHolder(): Entity? = stack.holder?.let(::Entity)

    fun getStackSize(): Int = stack.count

    // TODO: nbt
    // fun getEnchantments() = stack.enchantments

    fun isEnchantable() = stack.isEnchantable

    fun isEnchated() = stack.hasEnchantments()

    fun canPlaceOn(pos: BlockPos) = stack.canPlaceOn(Registries.BLOCK, CachedBlockPosition(World.getWorld(), pos.toMC(), false))

    fun canPlaceOn(block: Block) = canPlaceOn(block.pos)

    fun canHarvest(pos: BlockPos) = stack.canDestroy(Registries.BLOCK, CachedBlockPosition(World.getWorld(), pos.toMC(), false))

    fun canHarvest(block: Block) = canHarvest(block.pos)

    fun getDurability() = getMaxDamage() - getDamage()

    fun getMaxDamage() = stack.maxDamage

    fun getDamage() = stack.damage

    fun isDamagable() = stack.isDamageable

    @JvmOverloads
    fun getLore(advanced: Boolean = false): List<UTextComponent>? = (getHolder()?.entity as? PlayerEntity)?.let {
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

        Renderer.finishDraw()
    }
}
