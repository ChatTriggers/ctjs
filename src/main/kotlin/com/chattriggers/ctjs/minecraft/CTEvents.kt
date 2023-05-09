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

    fun interface MouseButtonCallback {
        fun process(mouseX: Double, mouseY: Double, button: Int, pressed: Boolean)
    }

    fun interface MouseScrollCallback {
        fun process(mouseX: Double, mouseY: Double, direction: Int)
    }

    fun interface MouseDraggedCallback {
        fun process(dx: Double, dy: Double, mouseX: Double, mouseY: Double, button: Int)
    }

    @JvmField
    val PRE_RENDER_OVERLAY = make<RenderCallback> { listeners ->
        RenderCallback { stack, mouseX, mouseY, drawable, partialTicks ->
            listeners.forEach { it.render(stack, mouseX, mouseY, drawable, partialTicks) }
        }
    }

    @JvmField
    val POST_RENDER_OVERLAY = make<RenderCallback> { listeners ->
        RenderCallback { stack, mouseX, mouseY, drawable, partialTicks ->
            listeners.forEach { it.render(stack, mouseX, mouseY, drawable, partialTicks) }
        }
    }

    @JvmField
    val POST_RENDER_SCREEN = make<RenderCallback> { listeners ->
        RenderCallback { stack, mouseX, mouseY, drawable, partialTicks ->
            listeners.forEach { it.render(stack, mouseX, mouseY, drawable, partialTicks) }
        }
    }

    @JvmField
    val PACKET_RECECIVED = make<PacketReceivedCallback> { listeners ->
        PacketReceivedCallback { packet, cb -> listeners.forEach { it.receive(packet, cb) } }
    }

    @JvmField
    val RENDER_TICK = make<VoidCallback> { listeners ->
        VoidCallback { listeners.forEach(VoidCallback::invoke) }
    }

    @JvmField
    val MOUSE_CLICKED = make<MouseButtonCallback> { listeners ->
        MouseButtonCallback { mouseX, mouseY, button, pressed ->
            listeners.forEach { it.process(mouseX, mouseY, button, pressed) }
        }
    }

    @JvmField
    val MOUSE_SCROLLED = make<MouseScrollCallback> { listeners ->
        MouseScrollCallback { mouseX, mouseY, direction ->
            listeners.forEach { it.process(mouseX, mouseY, direction) }
        }
    }

    @JvmField
    val MOUSE_DRAGGED = make<MouseDraggedCallback> { listeners ->
        MouseDraggedCallback { dx, dy, mouseX, mouseY, button ->
            listeners.forEach { it.process(dx, dy, mouseX, mouseY, button) }
        }
    }

    private inline fun <reified T> make(noinline reducer: (Array<T>) -> T) = EventFactory.createArrayBacked(T::class.java, reducer)
}
