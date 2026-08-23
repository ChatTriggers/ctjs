package com.chattriggers.ctjs.internal.listeners

import com.chattriggers.ctjs.api.render.Renderer
import com.chattriggers.ctjs.api.triggers.CancellableEvent
import com.chattriggers.ctjs.api.triggers.TriggerType
import com.chattriggers.ctjs.internal.triggers.TriggerContractAdapters
import com.chattriggers.ctjs.internal.utils.Initializer
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft

object WorldListener : Initializer {
    override fun init() {
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register { _, outline ->
            val event = CancellableEvent()
            TriggerType.BLOCK_HIGHLIGHT.triggerAll(TriggerContractAdapters.blockPos(outline.pos()), event)
            !event.isCancelled()
        }

        LevelRenderEvents.BEFORE_GIZMOS.register { ctx ->
            val deltaTicks = Minecraft.getInstance().deltaTracker.getGameTimeDeltaPartialTick(false)
            Renderer.withMatrix(ctx.poseStack(), deltaTicks) {
                TriggerType.PRE_RENDER_WORLD.triggerAll(deltaTicks)
            }
        }

        LevelRenderEvents.END_MAIN.register { ctx ->
            val deltaTicks = Minecraft.getInstance().deltaTracker.getGameTimeDeltaPartialTick(false)
            Renderer.withMatrix(ctx.poseStack(), deltaTicks) {
                TriggerType.POST_RENDER_WORLD.triggerAll(deltaTicks)
            }
        }
    }
}
