package com.chattriggers.ctjs.minecraft.listeners

import com.chattriggers.ctjs.engine.langs.js.JSContextFactory
import com.chattriggers.ctjs.engine.langs.js.JSLoader
import com.chattriggers.ctjs.minecraft.CTEvents
import com.chattriggers.ctjs.minecraft.libs.ChatLib
import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer
import com.chattriggers.ctjs.minecraft.wrappers.World
import com.chattriggers.ctjs.minecraft.wrappers.entity.Entity
import com.chattriggers.ctjs.minecraft.wrappers.world.block.Block
import com.chattriggers.ctjs.minecraft.wrappers.world.block.BlockFace
import com.chattriggers.ctjs.minecraft.wrappers.world.block.BlockPos
import com.chattriggers.ctjs.minecraft.wrappers.world.block.BlockType
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
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelPromise
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.network.packet.Packet
import net.minecraft.util.ActionResult
import net.minecraft.util.TypedActionResult
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

        CTEvents.PACKET_RECECIVED.register { packet, ctx ->
            JSLoader.wrapInContext(packetContext) {
                TriggerType.PacketReceived.triggerAll(packet, ctx)
            }
        }

        CTEvents.RENDER_TICK.register {
            TriggerType.Step.triggerAll()
        }

        CTEvents.POST_RENDER_SCREEN.register { stack, mouseX, mouseY, screen, partialTicks ->
            renderTrigger(stack, partialTicks) {
                TriggerType.PostGuiRender.triggerAll(mouseX, mouseY, screen, partialTicks)
            }
        }

        CTEvents.RENDER_OVERLAY.register { stack ->
            Renderer.matrixStack = UMatrixStack(stack)
            Renderer.pushMatrix()
            TriggerType.RenderOverlay.triggerAll()
            Renderer.popMatrix()
        }

        CTEvents.PRE_RENDER_WORLD.register { stack, partialTicks ->
            renderTrigger(stack, partialTicks) {
                TriggerType.PreRenderWorld.triggerAll(partialTicks)
            }
        }

        CTEvents.POST_RENDER_WORLD.register { stack, partialTicks ->
            renderTrigger(stack, partialTicks) {
                TriggerType.PreRenderWorld.triggerAll(partialTicks)
            }
        }

        CTEvents.RENDER_ENTITY.register { stack, entity, partialTicks, ci ->
            renderTrigger(stack, partialTicks) {
                // TODO(breaking): Don't pass the position into the trigger (they can get it
                //                 from the entity if its needed)
                TriggerType.RenderEntity.triggerAll(Entity(entity), partialTicks, ci)
            }
        }

        AttackBlockCallback.EVENT.register { _, _, _, pos, direction ->
            val event = CancellableEvent()

            TriggerType.PlayerInteract.triggerAll(
                PlayerInteraction.AttackBlock,
                World.getBlockAt(BlockPos(pos)).withFace(BlockFace.fromMC(direction)),
                event,
            )

            if (event.isCancelled()) ActionResult.FAIL else ActionResult.PASS
        }

        AttackEntityCallback.EVENT.register { _, _, _, entity, _ ->
            val event = CancellableEvent()

            TriggerType.PlayerInteract.triggerAll(
                PlayerInteraction.AttackEntity,
                Entity(entity),
                event,
            )

            if (event.isCancelled()) ActionResult.FAIL else ActionResult.PASS
        }

        PlayerBlockBreakEvents.BEFORE.register { _, _, pos, state, _ ->
            val event = CancellableEvent()

            TriggerType.PlayerInteract.triggerAll(
                PlayerInteraction.BreakBlock,
                Block(BlockType(state.block), BlockPos(pos)),
                event,
            )

            !event.isCancelled()
        }

        UseBlockCallback.EVENT.register { _, _, _, hitResult ->
            val event = CancellableEvent()

            TriggerType.PlayerInteract.triggerAll(
                PlayerInteraction.UseBlock,
                World.getBlockAt(BlockPos(hitResult.blockPos)).withFace(BlockFace.fromMC(hitResult.side)),
                event,
            )

            if (event.isCancelled()) ActionResult.FAIL else ActionResult.PASS
        }

        UseEntityCallback.EVENT.register { _, _, _, entity, _ ->
            val event = CancellableEvent()

            TriggerType.PlayerInteract.triggerAll(
                PlayerInteraction.UseEntity,
                Entity(entity),
                event
            )

            if (event.isCancelled()) ActionResult.FAIL else ActionResult.PASS
        }

        UseItemCallback.EVENT.register { _, _, _ ->
            val event = CancellableEvent()

            TriggerType.PlayerInteract.triggerAll(
                PlayerInteraction.UseItem,
                // TODO,
                event
            )

            if (event.isCancelled()) TypedActionResult.fail(null) else TypedActionResult.success(null)
        }
    }

    fun addTask(delay: Int, callback: () -> Unit) {
        tasks.add(Task(delay, callback))
    }

    private fun renderTrigger(stack: MatrixStack, partialTicks: Float, block: () -> Unit) {
        Renderer.partialTicks = partialTicks
        Renderer.matrixStack = UMatrixStack(stack)
        Renderer.pushMatrix()
        block()
        Renderer.popMatrix()
    }

    enum class PlayerInteraction(val isLeftHand: Boolean) {
        AttackBlock(true),
        AttackEntity(true),
        BreakBlock(true),
        UseBlock(false),
        UseEntity(false),
        UseItem(false),
    }
}
