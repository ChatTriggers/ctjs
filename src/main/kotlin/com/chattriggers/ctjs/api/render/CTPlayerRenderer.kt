package com.chattriggers.ctjs.api.render

import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.PlayerEntityRenderer
import net.minecraft.client.render.entity.feature.*
import net.minecraft.client.render.entity.model.ArmorEntityModel
import net.minecraft.client.render.entity.model.EntityModelLayers
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text

internal class CTPlayerRenderer(
    private val ctx: EntityRendererFactory.Context,
    private val slim: Boolean,
) : PlayerEntityRenderer(ctx, slim) {
    var showArmor = true
        set(value) {
            field = value
            reset()
        }
    var showHeldItem = true
        set(value) {
            field = value
            reset()
        }
    var showArrows = true
        set(value) {
            field = value
            reset()
        }
    var showCape = true
        set(value) {
            field = value
            reset()
        }
    var showElytra = true
        set(value) {
            field = value
            reset()
        }
    var showParrot = true
        set(value) {
            field = value
            reset()
        }
    var showStingers = true
        set(value) {
            field = value
            reset()
        }
    var showNametag = true
        set(value) {
            field = value
            reset()
        }

    fun setOptions(
        showNametag: Boolean = true,
        showArmor: Boolean = true,
        showCape: Boolean = true,
        showHeldItem: Boolean = true,
        showArrows: Boolean = true,
        showElytra: Boolean = true,
        showParrot: Boolean = true,
        showStingers: Boolean = true,
    ) {
        this.showNametag = showNametag
        this.showArmor = showArmor
        this.showCape = showCape
        this.showHeldItem = showHeldItem
        this.showArrows = showArrows
        this.showElytra = showElytra
        this.showParrot = showParrot
        this.showStingers = showStingers

        reset()
    }

    override fun hasLabel(livingEntity: AbstractClientPlayerEntity?) = showNametag

    override fun renderLabelIfPresent(
        abstractClientPlayerEntity: AbstractClientPlayerEntity?,
        text: Text?,
        matrixStack: MatrixStack?,
        vertexConsumerProvider: VertexConsumerProvider?,
        i: Int,
        f: Float
    ) {
        if (showNametag)
            super.renderLabelIfPresent(abstractClientPlayerEntity, text, matrixStack, vertexConsumerProvider, i, f)
    }

    private fun reset() {
        features.clear()

        if (showArmor) {
            addFeature(
                ArmorFeatureRenderer(
                    this,
                    ArmorEntityModel(ctx.getPart(if (slim) EntityModelLayers.PLAYER_SLIM_INNER_ARMOR else EntityModelLayers.PLAYER_INNER_ARMOR)),
                    ArmorEntityModel(ctx.getPart(if (slim) EntityModelLayers.PLAYER_SLIM_OUTER_ARMOR else EntityModelLayers.PLAYER_OUTER_ARMOR)),
                    ctx.modelManager
                )
            )
        }
        if (showHeldItem)
            addFeature(PlayerHeldItemFeatureRenderer(this, ctx.heldItemRenderer))
        if (showArrows)
            addFeature(StuckArrowsFeatureRenderer(ctx, this))
        addFeature(Deadmau5FeatureRenderer(this))
        if (showCape)
            addFeature(CapeFeatureRenderer(this))
        if (showArmor)
            addFeature(HeadFeatureRenderer(this, ctx.modelLoader, ctx.heldItemRenderer))
        if (showElytra)
            addFeature(ElytraFeatureRenderer(this, ctx.modelLoader))
        if (showParrot)
            addFeature(ShoulderParrotFeatureRenderer(this, ctx.modelLoader))
        addFeature(TridentRiptideFeatureRenderer(this, ctx.modelLoader))
        if (showStingers)
            addFeature(StuckStingersFeatureRenderer(this))
    }
}
