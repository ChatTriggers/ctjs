package com.chattriggers.ctjs.minecraft

import net.fabricmc.fabric.api.event.EventFactory

internal object CTEvents {
    fun interface RenderCallback {
        fun render()
    }

    @JvmField
    val PRE_RENDER_OVERLAY = EventFactory.createArrayBacked(RenderCallback::class.java) { listeners ->
        RenderCallback { listeners.forEach(RenderCallback::render) }
    }

    @JvmField
    val POST_RENDER_OVERLAY = EventFactory.createArrayBacked(RenderCallback::class.java) { listeners ->
        RenderCallback { listeners.forEach(RenderCallback::render) }
    }
}
