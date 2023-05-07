package com.chattriggers.ctjs.minecraft.listeners

import com.chattriggers.ctjs.engine.langs.js.JSContextFactory
import com.chattriggers.ctjs.engine.langs.js.JSLoader
import com.chattriggers.ctjs.minecraft.CTEvents
import com.chattriggers.ctjs.minecraft.libs.ChatLib
import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer
import com.chattriggers.ctjs.minecraft.wrappers.World
import com.chattriggers.ctjs.triggers.ChatTrigger
import com.chattriggers.ctjs.triggers.TriggerType
import com.chattriggers.ctjs.utils.Config
import com.chattriggers.ctjs.utils.Initializer
import com.chattriggers.ctjs.utils.console.printToConsole
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UMinecraft
import gg.essential.universal.wrappers.message.UTextComponent
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.network.packet.Packet
import org.mozilla.javascript.Context
import java.util.concurrent.CopyOnWriteArrayList

object ClientListener : Initializer {
    private var ticksPassed: Int = 0
    val chatHistory = mutableListOf<UTextComponent>()
    val actionBarHistory = mutableListOf<UTextComponent>()
    private val tasks = CopyOnWriteArrayList<Task>()
    private lateinit var packetContext: Context

    class Task(var delay: Int, val callback: () -> Unit)

    override fun init() {
        packetContext = JSContextFactory.enterContext()
        Context.exit()

        // TODO(breaking): Users now get the full message from the event by doing
        //                 "event.message" instead of "EventLib.getMessage(message)"
        ClientReceiveMessageEvents.ALLOW_CHAT.register { message, _, _, _, _ ->
            val textComponent = UTextComponent(message)
            chatHistory += textComponent
            if (chatHistory.size > 1000)
                chatHistory.removeAt(0)

            val event = ChatTrigger.Event(textComponent)
            TriggerType.Chat.triggerAll(event)

            // print to console
            if (Config.printChatToConsole)
                "[CHAT] ${ChatLib.replaceFormatting(UTextComponent(message).formattedText)}".printToConsole()

            !event.isCancelled()
        }

        ClientReceiveMessageEvents.ALLOW_GAME.register { message, overlay ->
            if (!overlay)
                return@register true

            val textComponent = UTextComponent(message)
            actionBarHistory += textComponent
            if (actionBarHistory.size > 1000)
                actionBarHistory.removeAt(0)

            val event = ChatTrigger.Event(textComponent)
            TriggerType.ActionBar.triggerAll(event)
            !event.isCancelled()
        }

        ClientTickEvents.START_CLIENT_TICK.register {
            tasks.removeAll {
                if (it.delay-- <= 0) {
                    UMinecraft.getMinecraft().submit(it.callback)
                    true
                } else false
            }

            if (World.isLoaded()) {
                TriggerType.Tick.triggerAll(ticksPassed)
                ticksPassed++

                // TODO
                // Scoreboard.resetCache()
            }
        }

        CTEvents.CONNECTION_CREATED.register { ctx ->
            ctx.channel().pipeline().addAfter("packet_handler", "ct:packet_handler", object : ChannelDuplexHandler() {
                override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                    val packetReceivedEvent = CancellableEvent()

                    if (msg is Packet<*>) {
                        JSLoader.wrapInContext(packetContext) {
                            TriggerType.PacketReceived.triggerAll(msg, packetReceivedEvent)
                        }
                    }

                    if (!packetReceivedEvent.isCancelled())
                        super.channelRead(ctx, msg)
                }

                override fun write(ctx: ChannelHandlerContext?, msg: Any?, promise: ChannelPromise?) {
                    val packetSentEvent = CancellableEvent()

                    if (msg is Packet<*>) {
                        JSLoader.wrapInContext(packetContext) {
                            TriggerType.PacketSent.triggerAll(msg, packetSentEvent)
                        }
                    }

                    if (!packetSentEvent.isCancelled())
                        ctx?.write(msg, promise)
                }
            })
        }

        CTEvents.RENDER_TICK.register {
            TriggerType.Step.triggerAll()

            // TODO:
            // if (World.isLoaded())
            //     MouseListener.handleDragged()
        }

        CTEvents.POST_RENDER_SCREEN.register { stack, mouseX, mouseY, screen, partialTicks ->
            Renderer.matrixStack = UMatrixStack(stack)
            Renderer.partialTicks = partialTicks
            TriggerType.PostGuiRender.triggerAll(mouseX, mouseY, screen)
        }

        CTEvents.PRE_RENDER_OVERLAY.register { stack, _, _, _, partialTicks ->
            Renderer.partialTicks = partialTicks
            Renderer.matrixStack = UMatrixStack(stack)

            Renderer.pushMatrix()

            // TODO: Handle the following events. Will most likely require separate injections in
            //       the respective Screen classes
            // RenderGameOverlayEvent.ElementType.PLAYER_LIST -> TriggerType.RenderPlayerList.triggerAll(event)
            // RenderGameOverlayEvent.ElementType.CROSSHAIRS -> TriggerType.RenderCrosshair.triggerAll(event)
            // RenderGameOverlayEvent.ElementType.DEBUG -> TriggerType.RenderDebug.triggerAll(event)
            // RenderGameOverlayEvent.ElementType.BOSSHEALTH -> TriggerType.RenderBossHealth.triggerAll(event)
            // RenderGameOverlayEvent.ElementType.HEALTH -> TriggerType.RenderHealth.triggerAll(event)
            // RenderGameOverlayEvent.ElementType.ARMOR -> TriggerType.RenderArmor.triggerAll(event)
            // RenderGameOverlayEvent.ElementType.FOOD -> TriggerType.RenderFood.triggerAll(event)
            // RenderGameOverlayEvent.ElementType.HEALTHMOUNT -> TriggerType.RenderMountHealth.triggerAll(event)
            // RenderGameOverlayEvent.ElementType.EXPERIENCE -> TriggerType.RenderExperience.triggerAll(event)
            // RenderGameOverlayEvent.ElementType.HOTBAR -> TriggerType.RenderHotbar.triggerAll(event)
            // RenderGameOverlayEvent.ElementType.AIR -> TriggerType.RenderAir.triggerAll(event)
            // RenderGameOverlayEvent.ElementType.TEXT -> TriggerType.RenderOverlay.triggerAll(event)
            // RenderGameOverlayEvent.ElementType.PORTAL -> TriggerType.RenderPortal.triggerAll(event)
            // RenderGameOverlayEvent.ElementType.JUMPBAR -> TriggerType.RenderJumpBar.triggerAll(event)
            // RenderGameOverlayEvent.ElementType.CHAT -> TriggerType.RenderChat.triggerAll(event)
            // RenderGameOverlayEvent.ElementType.HELMET -> TriggerType.RenderHelmet.triggerAll(event)

            Renderer.popMatrix()
        }
    }

    fun addTask(delay: Int, callback: () -> Unit) {
        tasks.add(Task(delay, callback))
    }

    // TODO
    // fun onLeftClick(e: PlayerInteractEvent) {
    //     val action = when (e) {
    //         is PlayerInteractEvent.EntityInteract, is PlayerInteractEvent.EntityInteractSpecific ->
    //             PlayerInteractAction.RIGHT_CLICK_ENTITY
    //         is PlayerInteractEvent.RightClickBlock -> PlayerInteractAction.RIGHT_CLICK_BLOCK
    //         is PlayerInteractEvent.RightClickItem -> PlayerInteractAction.RIGHT_CLICK_ITEM
    //         is PlayerInteractEvent.RightClickEmpty -> PlayerInteractAction.RIGHT_CLICK_EMPTY
    //         is PlayerInteractEvent.LeftClickBlock -> PlayerInteractAction.LEFT_CLICK_BLOCK
    //         is PlayerInteractEvent.LeftClickEmpty -> PlayerInteractAction.LEFT_CLICK_EMPTY
    //         else -> PlayerInteractAction.UNKNOWN
    //     }
    //
    //     TriggerType.PLAYER_INTERACT.triggerAll(
    //             action,
    //             World.getBlockAt(e.pos.x, e.pos.y, e.pos.z),
    //             e
    //     )
    // }
    //
    // @SubscribeEvent
    // fun onHandRender(e: RenderHandEvent) {
    //     TriggerType.RenderHand.triggerAll(e)
    // }
    //
    // /**
    //  * Used as a pass through argument in [com.chattriggers.ctjs.engine.IRegister.registerPlayerInteract].\n
    //  * Exposed in providedLibs as InteractAction.
    //  */
    // enum class PlayerInteractAction {
    //     RIGHT_CLICK_BLOCK,
    //     RIGHT_CLICK_EMPTY,
    //
    //     RIGHT_CLICK_ENTITY,
    //     RIGHT_CLICK_ITEM,
    //     LEFT_CLICK_EMPTY,
    //     UNKNOWN
    // }
}
