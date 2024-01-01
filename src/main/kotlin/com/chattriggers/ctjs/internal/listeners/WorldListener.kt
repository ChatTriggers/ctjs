package com.chattriggers.ctjs.internal.listeners

import com.chattriggers.ctjs.api.render.Renderer
import com.chattriggers.ctjs.api.triggers.CancellableEvent
import com.chattriggers.ctjs.api.triggers.TriggerType
import com.chattriggers.ctjs.internal.utils.Initializer
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.util.math.BlockPos

object WorldListener : Initializer {
    override fun init() {
        WorldRenderEvents.BLOCK_OUTLINE.register { _, ctx ->
            val event = CancellableEvent()
            TriggerType.BLOCK_HIGHLIGHT.triggerAll(BlockPos(ctx.blockPos()), event)
            !event.isCancelled()
        }

        WorldRenderEvents.START.register { ctx ->
            Renderer.withMatrix(ctx.matrixStack(), ctx.tickDelta()) {
                TriggerType.PRE_RENDER_WORLD.triggerAll(ctx.tickDelta())
            }
        }

        WorldRenderEvents.LAST.register { ctx ->
            Renderer.withMatrix(ctx.matrixStack(), ctx.tickDelta()) {
                TriggerType.POST_RENDER_WORLD.triggerAll(ctx.tickDelta())
            }
        }
    }
}
