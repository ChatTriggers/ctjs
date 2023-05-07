package com.chattriggers.ctjs.minecraft

import com.chattriggers.ctjs.minecraft.CTEvents.PacketReceivedCallback
import com.chattriggers.ctjs.minecraft.CTEvents.RenderCallback
import com.chattriggers.ctjs.minecraft.CTEvents.VoidCallback
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.client.gui.Drawable
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.network.packet.Packet
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

internal object CTEvents {
    fun interface VoidCallback {
        fun invoke()
    }

    fun interface RenderCallback {
        fun render(matrixStack: MatrixStack, mouseX: Int, mouseY: Int, drawable: Drawable, partialTicks: Float)
    }

    fun interface PacketReceivedCallback {
        fun receive(packet: Packet<*>, cb: CallbackInfo)
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
    val PACKET_RECECIVED = EventFactory.createArrayBacked(PacketReceivedCallback::class.java) { listeners ->
        PacketReceivedCallback { packet, cb -> listeners.forEach { it.receive(packet, cb) } }
    }

    @JvmField
    val RENDER_TICK = EventFactory.createArrayBacked(VoidCallback::class.java) { listeners ->
        VoidCallback { listeners.forEach(VoidCallback::invoke) }
    }
}
