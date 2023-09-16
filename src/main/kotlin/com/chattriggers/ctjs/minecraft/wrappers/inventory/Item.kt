package com.chattriggers.ctjs.minecraft.wrappers.inventory

import com.chattriggers.ctjs.Skippable
import com.chattriggers.ctjs.TooltipOverridable
import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer
import com.chattriggers.ctjs.minecraft.objects.TextComponent
import com.chattriggers.ctjs.minecraft.wrappers.CTWrapper
import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.minecraft.wrappers.Player
import com.chattriggers.ctjs.minecraft.wrappers.World
import com.chattriggers.ctjs.minecraft.wrappers.entity.Entity
import com.chattriggers.ctjs.minecraft.wrappers.inventory.nbt.NBTTagCompound
import com.chattriggers.ctjs.minecraft.wrappers.world.block.Block
import com.chattriggers.ctjs.minecraft.wrappers.world.block.BlockPos
import com.chattriggers.ctjs.utils.MCNbtCompound
import com.chattriggers.ctjs.utils.asMixin
import net.minecraft.block.pattern.CachedBlockPosition
import net.minecraft.client.item.TooltipContext
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries

//#if MC>=12000
import net.minecraft.client.render.DiffuseLighting
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.model.json.ModelTransformationMode
import net.minecraft.util.crash.CrashException
import net.minecraft.util.crash.CrashReport
//#endif

class Item(override val mcValue: ItemStack) : CTWrapper<ItemStack> {
    val type: ItemType = ItemType(mcValue.item)

    init {
        require(!mcValue.isEmpty) {
            "Can not wrap empty ItemStack as an Item"
        }
    }

    constructor(type: ItemType) : this(type.toMC().defaultStack)

    fun getHolder(): Entity? = mcValue.holder?.let(Entity::fromMC)

    fun getStackSize(): Int = mcValue.count

    fun setStackSize(size: Int) = apply {
        mcValue.count = size
    }

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

    fun getName(): String = TextComponent(mcValue.name).formattedText

    fun setName(name: TextComponent?) = apply {
        mcValue.setCustomName(name)
    }

    fun resetName() {
        setName(null)
    }

    @JvmOverloads
    fun getLore(advanced: Boolean = false): List<TextComponent> {
        mcValue.asMixin<Skippable>().ctjs_setShouldSkip(true)
        val tooltip = mcValue.getTooltip(Player.toMC(), if (advanced) TooltipContext.ADVANCED else TooltipContext.BASIC).map(::TextComponent)
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

        Renderer.pushMatrix()
            .scale(scale, scale, 1f)
            .translate(x / scale, y / scale, z)
            .colorize(1f, 1f, 1f, 1f)

        // The item draw method moved to DrawContext in 1.20, which we don't have access
        // to here, so its drawItem method has been copy-pasted here instead
        //#if MC>=12000
        if (mcValue.isEmpty)
            return
        val bakedModel = itemRenderer.getModel(mcValue, World.toMC(), null, 0)
        Renderer.pushMatrix()
        Renderer.translate(x + 8, y + 8, (150f + if (bakedModel.hasDepth()) z else 0f))
        try {
            Renderer.scale(1.0f, -1.0f, 1.0f)
            Renderer.scale(16.0f, 16.0f, 16.0f)
            if (!bakedModel.isSideLit)
                DiffuseLighting.disableGuiDepthLighting()
            val vertexConsumers = Client.getMinecraft().bufferBuilders.entityVertexConsumers
            itemRenderer.renderItem(
                mcValue,
                ModelTransformationMode.GUI,
                false,
                Renderer.matrixStack.toMC(),
                vertexConsumers,
                0xF000F0,
                OverlayTexture.DEFAULT_UV,
                bakedModel,
            )
            Renderer.disableDepth()
            vertexConsumers.draw()
            Renderer.enableDepth()
            if (!bakedModel.isSideLit) {
                DiffuseLighting.enableGuiDepthLighting()
            }
        } catch (e: Throwable) {
            val crashReport = CrashReport.create(e, "Rendering item")
            val crashReportSection = crashReport.addElement("Item being rendered")
            crashReportSection.add("Item Type") { mcValue.item.toString() }
            crashReportSection.add("Item Damage") { mcValue.damage.toString() }
            crashReportSection.add("Item NBT") { mcValue.nbt.toString() }
            crashReportSection.add("Item Foil") { mcValue.hasGlint().toString() }
            throw CrashException(crashReport)
        }
        Renderer.popMatrix()
        //#else
        //$$ itemRenderer.renderInGui(Renderer.matrixStack.toMC(), mcValue, 0, 0)
        //#endif
        Renderer.popMatrix()
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
