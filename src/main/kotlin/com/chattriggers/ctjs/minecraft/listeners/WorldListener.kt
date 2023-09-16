package com.chattriggers.ctjs.minecraft.listeners

import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer3d
import com.chattriggers.ctjs.triggers.TriggerType
import com.chattriggers.ctjs.utils.Initializer
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
            ClientListener.renderTrigger(ctx.matrixStack(), ctx.tickDelta()) {
                TriggerType.PRE_RENDER_WORLD.triggerAll(ctx.tickDelta())
            }
        }

        WorldRenderEvents.LAST.register { ctx ->
            ClientListener.renderTrigger(ctx.matrixStack(), ctx.tickDelta()) {
                TriggerType.POST_RENDER_WORLD.triggerAll(ctx.tickDelta())
            }
        }
    }
}
