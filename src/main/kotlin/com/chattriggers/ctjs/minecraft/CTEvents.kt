package com.chattriggers.ctjs.minecraft

import com.chattriggers.ctjs.minecraft.CTEvents.PacketReceivedCallback
import com.chattriggers.ctjs.minecraft.CTEvents.RenderScreenCallback
import com.chattriggers.ctjs.minecraft.CTEvents.VoidCallback
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.network.packet.Packet
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

internal object CTEvents {
    fun interface VoidCallback {
        fun invoke()
    }

    fun interface RenderScreenCallback {
        fun render(matrixStack: MatrixStack, mouseX: Int, mouseY: Int, drawable: Drawable, partialTicks: Float)
    }

    fun interface RenderWorldCallback {
        fun render(matrixStack: MatrixStack, partialTicks: Float)
    }

    fun interface RenderEntityCallback {
        fun render(matrixStack: MatrixStack, entity: Entity, partialTicks: Float, ci: CallbackInfo)
    }

    fun interface RenderOverlayCallback {
        fun render(matrixStack: MatrixStack, partialTicks: Float)
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

    fun interface GuiMouseDragCallback {
        fun process(dx: Double, dy: Double, mouseX: Double, mouseY: Double, button: Int, gui: Screen, ci: CallbackInfo)
    }

    @JvmField
    val PRE_RENDER_OVERLAY = make<RenderScreenCallback> { listeners ->
        RenderScreenCallback { stack, mouseX, mouseY, drawable, partialTicks ->
            listeners.forEach { it.render(stack, mouseX, mouseY, drawable, partialTicks) }
        }
    }

    @JvmField
    val POST_RENDER_OVERLAY = make<RenderScreenCallback> { listeners ->
        RenderScreenCallback { stack, mouseX, mouseY, drawable, partialTicks ->
            listeners.forEach { it.render(stack, mouseX, mouseY, drawable, partialTicks) }
        }
    }

    @JvmField
    val POST_RENDER_SCREEN = make<RenderScreenCallback> { listeners ->
        RenderScreenCallback { stack, mouseX, mouseY, drawable, partialTicks ->
            listeners.forEach { it.render(stack, mouseX, mouseY, drawable, partialTicks) }
        }
    }

    @JvmField
    val RENDER_OVERLAY = make<RenderOverlayCallback> { listeners ->
        RenderOverlayCallback { stack, partialTicks ->
            listeners.forEach { it.render(stack, partialTicks) }
        }
    }

    @JvmField
    val PRE_RENDER_WORLD = make<RenderWorldCallback> { listeners ->
        RenderWorldCallback { stack, partialTicks ->
            listeners.forEach { it.render(stack, partialTicks) }
        }
    }

    @JvmField
    val POST_RENDER_WORLD = make<RenderWorldCallback> { listeners ->
        RenderWorldCallback { stack, partialTicks ->
            listeners.forEach { it.render(stack, partialTicks) }
        }
    }

    @JvmField
    val RENDER_ENTITY = make<RenderEntityCallback> { listeners ->
        RenderEntityCallback { stack, entity, partialTicks, ci ->
            listeners.forEach { it.render(stack, entity, partialTicks, ci) }
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

    @JvmField
    val GUI_MOUSE_DRAG = make<GuiMouseDragCallback> { listeners ->
        GuiMouseDragCallback { dx, dy, mouseX, mouseY, button, screen, ci ->
            listeners.forEach { it.process(dx, dy, mouseX, mouseY, button, screen, ci) }
        }
    }

    private inline fun <reified T> make(noinline reducer: (Array<T>) -> T) = EventFactory.createArrayBacked(T::class.java, reducer)
}
