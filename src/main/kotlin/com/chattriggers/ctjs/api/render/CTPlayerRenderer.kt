package com.chattriggers.ctjs.api.render

import net.minecraft.client.renderer.entity.EntityRendererProvider

/**
 * Holds the legacy CTJS player-preview options.
 *
 * Minecraft 26.1 renders GUI entities from extracted render state rather than a
 * second mutable entity renderer. Renderer.drawPlayer uses that pipeline; these
 * flags remain centralized here so layer filtering can be restored without an
 * API change when equivalent avatar render-state hooks are available.
 */
internal class CTPlayerRenderer(
    @Suppress("UNUSED_PARAMETER") ctx: EntityRendererProvider.Context,
    @Suppress("UNUSED_PARAMETER") slim: Boolean,
) {
    var showArmor = true
    var showHeldItem = true
    var showArrows = true
    var showCape = true
    var showElytra = true
    var showParrot = true
    var showStingers = true
    var showNametag = true

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
    }
}
