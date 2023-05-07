package com.chattriggers.ctjs.minecraft.listeners

import com.chattriggers.ctjs.triggers.TriggerType
import com.chattriggers.ctjs.utils.Initializer
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents

object WorldListener : Initializer {
    override fun init() {
        WorldRenderEvents.BLOCK_OUTLINE.register { _, ctx ->
            val event = CancellableEvent()
            // TODO: Use a BlockPos wrapper for this
            TriggerType.BlockHighlight.triggerAll(ctx.blockPos(), event)
            !event.isCancelled()
        }
    }
}
