package com.chattriggers.ctjs.api.inventory

import com.chattriggers.ctjs.api.CTWrapper
import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.client.Player
import com.chattriggers.ctjs.api.entity.Entity
import com.chattriggers.ctjs.api.inventory.nbt.NBTTagCompound
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.api.render.Renderer
import com.chattriggers.ctjs.api.world.World
import com.chattriggers.ctjs.api.world.block.Block
import com.chattriggers.ctjs.api.world.block.BlockPos
import com.chattriggers.ctjs.internal.Skippable
import com.chattriggers.ctjs.internal.TooltipOverridable
import com.chattriggers.ctjs.MCNbtCompound
import com.chattriggers.ctjs.internal.utils.asMixin
import net.minecraft.world.level.block.state.pattern.BlockInWorld
import com.mojang.blaze3d.platform.Lighting
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.ItemEnchantments
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.ReportedException
import net.minecraft.CrashReport
import kotlin.jvm.optionals.getOrNull

class Item(override val mcValue: ItemStack) : CTWrapper<ItemStack> {
    val type: ItemType = ItemType(mcValue.item)

    init {
        require(!mcValue.isEmpty) {
            "Can not wrap empty ItemStack as an Item"
        }
    }

    constructor(type: ItemType) : this(type.toMC().defaultInstance)

    fun getHolder(): Entity? = null // ItemStack no longer retains an owning entity in 26.1.

    fun getStackSize(): Int = mcValue.count

    fun setStackSize(size: Int) = apply {
        mcValue.count = size
    }

    fun getEnchantments() = mcValue.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet().associate {
        it.key.unwrapKey().getOrNull() to it.intValue
    }

    fun isEnchantable() = mcValue.isEnchantable

    fun isEnchanted() = mcValue.isEnchanted

    fun canPlaceOn(pos: BlockPos) =
        World.toMC()?.let { mcValue.canPlaceOnBlockInAdventureMode(BlockInWorld(it, pos.toMC(), false)) } ?: false

    fun canPlaceOn(block: Block) = canPlaceOn(block.pos)

    fun canHarvest(pos: BlockPos) =
        World.toMC()?.let { mcValue.canBreakBlockInAdventureMode(BlockInWorld(it, pos.toMC(), false)) } ?: false

    fun canHarvest(block: Block) = canHarvest(block.pos)

    fun getDurability() = getMaxDamage() - getDamage()

    fun getMaxDamage() = mcValue.maxDamage

    fun getDamage() = mcValue.damageValue

    fun isDamageable() = mcValue.isDamageableItem

    fun getName(): String = TextComponent(mcValue.hoverName).formattedText

    fun setName(name: TextComponent?) = apply {
        mcValue.set(DataComponents.CUSTOM_NAME, name)
    }

    fun resetName() {
        setName(null)
    }

    @JvmOverloads
    fun getLore(advanced: Boolean = false): List<TextComponent> {
        mcValue.asMixin<Skippable>().ctjs_setShouldSkip(true)
        val tooltip = mcValue.getTooltipLines(
            TooltipContext.of(World.toMC()),
            Player.toMC(),
            if (advanced) TooltipFlag.ADVANCED else TooltipFlag.NORMAL,
        ).mapTo(mutableListOf()) { TextComponent(it) }

        mcValue.asMixin<Skippable>().ctjs_setShouldSkip(false)

        return tooltip
    }

    fun setLore(lore: List<TextComponent>) {
        mcValue.asMixin<TooltipOverridable>().apply {
            ctjs_setTooltip(lore)
            ctjs_setShouldOverrideTooltip(true)
        }
    }

    fun resetLore() {
        mcValue.asMixin<TooltipOverridable>().ctjs_setShouldOverrideTooltip(false)
    }

    // TODO: make a component wrapper?
    fun getNBT() = mcValue.components

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
        Renderer.drawItemStack(mcValue, x, y, scale)
    }

    override fun toString(): String = "Item{name=${getName()}, type=${type.getRegistryName()}, size=${getStackSize()}}"

    companion object {
        @JvmStatic
        fun fromMC(mcValue: ItemStack): Item? {
            return if (mcValue.isEmpty) {
                null
            } else {
                Item(mcValue)
            }
        }
    }
}
