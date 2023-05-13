package com.chattriggers.ctjs.minecraft.listeners

import com.chattriggers.ctjs.triggers.TriggerType
import com.chattriggers.ctjs.utils.Initializer
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.util.math.BlockPos

object WorldListener : Initializer {
    override fun init() {
        WorldRenderEvents.BLOCK_OUTLINE.register { _, ctx ->
            val event = CancellableEvent()
            TriggerType.BlockHighlight.triggerAll(BlockPos(ctx.blockPos()), event)
            !event.isCancelled()
        }
    }
}
