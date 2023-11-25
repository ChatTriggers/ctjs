package com.chattriggers.ctjs.internal.engine

import com.chattriggers.ctjs.internal.engine.CTEvents.BreakBlockCallback
import com.chattriggers.ctjs.internal.engine.CTEvents.GuiMouseDragCallback
import com.chattriggers.ctjs.internal.engine.CTEvents.MouseButtonCallback
import com.chattriggers.ctjs.internal.engine.CTEvents.MouseDraggedCallback
import com.chattriggers.ctjs.internal.engine.CTEvents.MouseScrollCallback
import com.chattriggers.ctjs.internal.engine.CTEvents.NetworkCommandDispatcherRegisterCallback
import com.chattriggers.ctjs.internal.engine.CTEvents.PacketReceivedCallback
import com.chattriggers.ctjs.internal.engine.CTEvents.RenderBlockEntityCallback
import com.chattriggers.ctjs.internal.engine.CTEvents.RenderEntityCallback
import com.chattriggers.ctjs.internal.engine.CTEvents.RenderOverlayCallback
import com.chattriggers.ctjs.internal.engine.CTEvents.RenderWorldCallback
import com.chattriggers.ctjs.internal.engine.CTEvents.VoidCallback
import com.chattriggers.ctjs.MCBlockEntity
import com.chattriggers.ctjs.MCBlockPos
import com.chattriggers.ctjs.MCEntity
import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.util.math.MatrixStack
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
        fun render(matrixStack: MatrixStack, entity: MCEntity, partialTicks: Float, ci: CallbackInfo)
    }

    fun interface RenderBlockEntityCallback {
        fun render(matrixStack: MatrixStack, entity: MCBlockEntity, partialTicks: Float, ci: CallbackInfo)
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
        fun process(mouseX: Double, mouseY: Double, delta: Double)
    }

    fun interface MouseDraggedCallback {
        fun process(dx: Double, dy: Double, mouseX: Double, mouseY: Double, button: Int)
    }

    fun interface GuiMouseDragCallback {
        fun process(dx: Double, dy: Double, mouseX: Double, mouseY: Double, button: Int, gui: Screen, ci: CallbackInfo)
    }

    fun interface BreakBlockCallback {
        fun breakBlock(pos: MCBlockPos)
    }

    fun interface NetworkCommandDispatcherRegisterCallback {
        fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>)
    }

    @JvmField
    val RENDER_GAME = make<Runnable> { listeners ->
        Runnable { listeners.forEach(Runnable::run) }
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
    val RENDER_BLOCK_ENTITY = make<RenderBlockEntityCallback> { listeners ->
        RenderBlockEntityCallback { stack, blockEntity, partialTicks, ci ->
            listeners.forEach { it.render(stack, blockEntity, partialTicks, ci) }
        }
    }

    @JvmField
    val PACKET_RECEIVED = make<PacketReceivedCallback> { listeners ->
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
        MouseScrollCallback { mouseX, mouseY, delta ->
            listeners.forEach { it.process(mouseX, mouseY, delta) }
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

    @JvmField
    val BREAK_BLOCK = make<BreakBlockCallback> { listeners ->
        BreakBlockCallback { pos ->
            listeners.forEach { it.breakBlock(pos) }
        }
    }

    @JvmField
    val NETWORK_COMMAND_DISPATCHER_REGISTER = make<NetworkCommandDispatcherRegisterCallback> { listeners ->
        NetworkCommandDispatcherRegisterCallback { dispatcher ->
            listeners.forEach { it.register(dispatcher) }
        }
    }

    private inline fun <reified T> make(noinline reducer: (Array<T>) -> T): Event<T> {
        return EventFactory.createArrayBacked(T::class.java, reducer)
    }
}
