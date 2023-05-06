package com.chattriggers.ctjs.minecraft

import io.netty.channel.ChannelHandlerContext
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.client.gui.Drawable
import net.minecraft.client.util.math.MatrixStack

internal object CTEvents {
    fun interface VoidCallback {
        fun invoke()
    }

    fun interface RenderCallback {
        fun render(matrixStack: MatrixStack, mouseX: Int, mouseY: Int, drawable: Drawable, partialTicks: Float)
    }

    fun interface ConnectionCallback {
        fun connect(context: ChannelHandlerContext)
    }

    @JvmField
    val PRE_RENDER_OVERLAY = EventFactory.createArrayBacked(RenderCallback::class.java) { listeners ->
        RenderCallback { stack, mouseX, mouseY, drawable, partialTicks ->
            listeners.forEach { it.render(stack, mouseX, mouseY, drawable, partialTicks) }
        }
    }

    @JvmField
    val POST_RENDER_OVERLAY = EventFactory.createArrayBacked(RenderCallback::class.java) { listeners ->
        RenderCallback { stack, mouseX, mouseY, drawable, partialTicks ->
            listeners.forEach { it.render(stack, mouseX, mouseY, drawable, partialTicks) }
        }
    }

    @JvmField
    val POST_RENDER_SCREEN = EventFactory.createArrayBacked(RenderCallback::class.java) { listeners ->
        RenderCallback { stack, mouseX, mouseY, drawable, partialTicks ->
            listeners.forEach { it.render(stack, mouseX, mouseY, drawable, partialTicks) }
        }
    }

    @JvmField
    val CONNECTION_CREATED = EventFactory.createArrayBacked(ConnectionCallback::class.java) { listeners ->
        ConnectionCallback { ctx -> listeners.forEach { it.connect(ctx) } }
    }

    @JvmField
    val RENDER_TICK = EventFactory.createArrayBacked(VoidCallback::class.java) { listeners ->
        VoidCallback { listeners.forEach(VoidCallback::invoke) }
    }
}
