package com.chattriggers.ctjs.minecraft.listeners

import com.chattriggers.ctjs.engine.js.JSContextFactory
import com.chattriggers.ctjs.engine.js.JSLoader
import com.chattriggers.ctjs.minecraft.CTEvents
import com.chattriggers.ctjs.minecraft.libs.ChatLib
import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer
import com.chattriggers.ctjs.minecraft.wrappers.World
import com.chattriggers.ctjs.minecraft.wrappers.entity.BlockEntity
import com.chattriggers.ctjs.minecraft.wrappers.entity.Entity
import com.chattriggers.ctjs.minecraft.wrappers.world.block.BlockFace
import com.chattriggers.ctjs.minecraft.wrappers.world.block.BlockPos
import com.chattriggers.ctjs.triggers.ChatTrigger
import com.chattriggers.ctjs.triggers.TriggerType
import com.chattriggers.ctjs.utils.Config
import com.chattriggers.ctjs.utils.Initializer
import com.chattriggers.ctjs.console.printToConsole
import com.chattriggers.ctjs.engine.module.ModuleManager
import com.chattriggers.ctjs.minecraft.wrappers.inventory.Item
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UMinecraft
import gg.essential.universal.wrappers.message.UTextComponent
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import org.lwjgl.glfw.GLFW
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

        // TODO*(breaking): Users now get the full message from the event by doing
        //                 "event.message" instead of "EventLib.getMessage(message)"
        ClientReceiveMessageEvents.ALLOW_CHAT.register { message, _, _, _, _ ->
            val textComponent = UTextComponent(message)
            chatHistory += textComponent
            if (chatHistory.size > 1000)
                chatHistory.removeAt(0)

            val event = ChatTrigger.Event(textComponent)
            TriggerType.CHAT.triggerAll(event)

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
            TriggerType.ACTION_BAR.triggerAll(event)
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
                TriggerType.TICK.triggerAll(ticksPassed)
                ticksPassed++

                // TODO
                // Scoreboard.resetCache()
            }
        }

        ClientSendMessageEvents.ALLOW_CHAT.register { message ->
            val event = CancellableEvent()
            TriggerType.MESSAGE_SENT.triggerAll(message, event)

            !event.isCancelled()
        }

        ClientSendMessageEvents.ALLOW_COMMAND.register { message ->
            val event = CancellableEvent()
            TriggerType.MESSAGE_SENT.triggerAll(message, event)

            !event.isCancelled()
        }

        ScreenEvents.BEFORE_INIT.register { _, screen, _, _ ->
            ScreenEvents.beforeRender(screen).register { _, stack, mouseX, mouseY, partialTicks ->
                renderTrigger(stack, partialTicks) {
                    TriggerType.GUI_RENDER.triggerAll(mouseX, mouseY, screen)
                }
            }

            ScreenEvents.afterRender(screen).register { _, stack, mouseX, mouseY, partialTicks ->
                renderTrigger(stack, partialTicks) {
                    TriggerType.POST_GUI_RENDER.triggerAll(mouseX, mouseY, screen, partialTicks)
                }
            }

            ScreenKeyboardEvents.allowKeyPress(screen).register { _, key, scancode, _ ->
                val event = CancellableEvent()
                TriggerType.GUI_KEY.triggerAll(GLFW.glfwGetKeyName(key, scancode), key, screen, event)
                !event.isCancelled()
            }
        }

        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            ScreenEvents.remove(screen).register {
                TriggerType.GUI_CLOSED.triggerAll(screen)
            }
        }

        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            TriggerType.WORLD_LOAD.triggerAll()
            ModuleManager.reportOldVersions()
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            TriggerType.WORLD_UNLOAD.triggerAll()
        }

        CTEvents.PACKET_RECECIVED.register { packet, ctx ->
            JSLoader.wrapInContext(packetContext) {
                TriggerType.PACKET_RECEIVED.triggerAll(packet, ctx)
            }
        }

        CTEvents.RENDER_TICK.register {
            TriggerType.STEP.triggerAll()
        }

        CTEvents.RENDER_OVERLAY.register { stack, partialTicks ->
            renderTrigger(stack, partialTicks) {
                TriggerType.RENDER_OVERLAY.triggerAll(CancellableEvent())
            }
        }

        CTEvents.PRE_RENDER_WORLD.register { stack, partialTicks ->
            renderTrigger(stack, partialTicks) {
                TriggerType.PRE_RENDER_WORLD.triggerAll(partialTicks)
            }
        }

        CTEvents.POST_RENDER_WORLD.register { stack, partialTicks ->
            renderTrigger(stack, partialTicks) {
                TriggerType.POST_RENDER_WORLD.triggerAll(partialTicks)
            }
        }

        CTEvents.RENDER_ENTITY.register { stack, entity, partialTicks, ci ->
            renderTrigger(stack, partialTicks) {
                TriggerType.RENDER_ENTITY.triggerAll(Entity.fromMC(entity), partialTicks, ci)
            }
        }

        CTEvents.RENDER_BLOCK_ENTITY.register { stack, blockEntity, partialTicks, ci ->
            renderTrigger(stack, partialTicks) {
                TriggerType.RENDER_BLOCK_ENTITY.triggerAll(BlockEntity(blockEntity), partialTicks, ci)
            }
        }

        AttackBlockCallback.EVENT.register { player, _, _, pos, direction ->
            if (!player.world.isClient) return@register ActionResult.PASS
            val event = CancellableEvent()

            TriggerType.PLAYER_INTERACT.triggerAll(
                PlayerInteraction.AttackBlock,
                World.getBlockAt(BlockPos(pos)).withFace(BlockFace.fromMC(direction)),
                event,
            )

            if (event.isCancelled()) ActionResult.FAIL else ActionResult.PASS
        }

        AttackEntityCallback.EVENT.register { player, _, _, entity, _ ->
            if (!player.world.isClient) return@register ActionResult.PASS
            val event = CancellableEvent()

            TriggerType.PLAYER_INTERACT.triggerAll(
                PlayerInteraction.AttackEntity,
                Entity.fromMC(entity),
                event,
            )

            if (event.isCancelled()) ActionResult.FAIL else ActionResult.PASS
        }

        CTEvents.BREAK_BLOCK.register { pos ->
            TriggerType.PLAYER_INTERACT.triggerAll(
                PlayerInteraction.BreakBlock,
                World.getBlockAt(BlockPos(pos)),
                CancellableEvent(),
            )
        }

        UseBlockCallback.EVENT.register { player, _, hand, hitResult ->
            if (!player.world.isClient) return@register ActionResult.PASS
            val event = CancellableEvent()

            TriggerType.PLAYER_INTERACT.triggerAll(
                PlayerInteraction.UseBlock(hand),
                World.getBlockAt(BlockPos(hitResult.blockPos)).withFace(BlockFace.fromMC(hitResult.side)),
                event,
            )

            if (event.isCancelled()) ActionResult.FAIL else ActionResult.PASS
        }

        UseEntityCallback.EVENT.register { player, _, hand, entity, _ ->
            if (!player.world.isClient) return@register ActionResult.PASS
            val event = CancellableEvent()

            TriggerType.PLAYER_INTERACT.triggerAll(
                PlayerInteraction.UseEntity(hand),
                Entity.fromMC(entity),
                event,
            )

            if (event.isCancelled()) ActionResult.FAIL else ActionResult.PASS
        }

        UseItemCallback.EVENT.register { player, _, hand ->
            if (!player.world.isClient) return@register TypedActionResult.pass(null)
            val event = CancellableEvent()

            val stack = player.getStackInHand(hand)

            TriggerType.PLAYER_INTERACT.triggerAll(
                PlayerInteraction.UseItem(hand),
                if (stack.isEmpty) null else Item(stack),
                event,
            )

            if (event.isCancelled()) TypedActionResult.fail(null) else TypedActionResult.pass(null)
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

    // TODO(breaking): Difference cases here
    sealed class PlayerInteraction(val name: String, val mainHand: Boolean) {
        object AttackBlock : PlayerInteraction("AttackBlock", true)
        object AttackEntity : PlayerInteraction("AttackEntity", true)
        object BreakBlock : PlayerInteraction("BreakBlock", true)
        class UseBlock(hand: Hand) : PlayerInteraction("UseBlock", hand == Hand.MAIN_HAND)
        class UseEntity(hand: Hand) : PlayerInteraction("UseEntity", hand == Hand.MAIN_HAND)
        class UseItem(hand: Hand) : PlayerInteraction("UseItem", hand == Hand.MAIN_HAND)

        override fun toString(): String = name
    }
}
