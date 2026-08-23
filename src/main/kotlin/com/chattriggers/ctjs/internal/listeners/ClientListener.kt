package com.chattriggers.ctjs.internal.listeners

import com.chattriggers.ctjs.api.entity.BlockEntity
import com.chattriggers.ctjs.api.entity.Entity
import com.chattriggers.ctjs.api.entity.PlayerInteraction
import com.chattriggers.ctjs.api.inventory.Item
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.api.render.Renderer
import com.chattriggers.ctjs.api.triggers.CancellableEvent
import com.chattriggers.ctjs.api.triggers.ChatTrigger
import com.chattriggers.ctjs.api.triggers.TriggerType
import com.chattriggers.ctjs.api.world.Scoreboard
import com.chattriggers.ctjs.api.world.TabList
import com.chattriggers.ctjs.api.world.World
import com.chattriggers.ctjs.api.world.block.BlockFace
import com.chattriggers.ctjs.api.world.block.BlockPos
import com.chattriggers.ctjs.internal.engine.CTEvents
import com.chattriggers.ctjs.internal.engine.JSContextFactory
import com.chattriggers.ctjs.internal.engine.JSLoader
import com.chattriggers.ctjs.internal.lifecycle.GenerationTaskQueue
import com.chattriggers.ctjs.internal.lifecycle.RuntimeOwner
import com.chattriggers.ctjs.internal.utils.Initializer
import gg.essential.universal.UMinecraft
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.fabricmc.fabric.api.event.player.*
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import org.lwjgl.glfw.GLFW
import org.mozilla.javascript.Context

object ClientListener : Initializer {
    private var ticksPassed: Int = 0
    val chatHistory = mutableListOf<TextComponent>()
    val actionBarHistory = mutableListOf<TextComponent>()
    private val tasks = GenerationTaskQueue()
    private lateinit var packetContext: Context

    override fun init() {
        packetContext = JSContextFactory.enterContext()
        Context.exit()

        ClientReceiveMessageEvents.ALLOW_CHAT.register { message, _, _, _, _ ->
            handleChatMessage(message, actionBar = false)
        }

        ClientReceiveMessageEvents.ALLOW_GAME.register { message, overlay ->
            handleChatMessage(message, actionBar = overlay)
        }

        ClientTickEvents.START_CLIENT_TICK.register {
            tasks.tick { callback ->
                UMinecraft.getMinecraft().submit(callback)
            }

            if (World.isLoaded() && World.toMC()?.tickRateManager()?.runsNormally() == true) {
                TriggerType.TICK.triggerAll(ticksPassed)
                ticksPassed++

                Scoreboard.resetCache()
                TabList.resetCache()
            }
        }

        ClientSendMessageEvents.ALLOW_CHAT.register { message ->
            val event = CancellableEvent()
            TriggerType.MESSAGE_SENT.triggerAll(message, event)

            !event.isCancelled()
        }

        ClientSendMessageEvents.ALLOW_COMMAND.register { message ->
            val event = CancellableEvent()
            TriggerType.MESSAGE_SENT.triggerAll("/$message", event)

            !event.isCancelled()
        }

        ScreenEvents.BEFORE_INIT.register { _, screen, _, _ ->
            // TODO: Why does Renderer.drawString not work in here?
            ScreenEvents.beforeExtract(screen).register { _, extractor, mouseX, mouseY, partialTicks ->
                Renderer.withGuiGraphics(extractor, partialTicks) {
                    TriggerType.GUI_RENDER.triggerAll(mouseX, mouseY, screen)
                }
            }

            // TODO: Why does Renderer.drawString not work in here?
            ScreenEvents.afterExtract(screen).register { _, extractor, mouseX, mouseY, partialTicks ->
                Renderer.withGuiGraphics(extractor, partialTicks) {
                    TriggerType.POST_GUI_RENDER.triggerAll(mouseX, mouseY, screen, partialTicks)
                }
            }

            ScreenKeyboardEvents.allowKeyPress(screen).register { _, keyEvent ->
                val event = CancellableEvent()
                TriggerType.GUI_KEY.triggerAll(
                    // GLFW returns null for non-printable keys. This is the public nullable contract.
                    GLFW.glfwGetKeyName(keyEvent.key, keyEvent.scancode),
                    keyEvent.key,
                    screen,
                    event,
                )
                !event.isCancelled()
            }
        }

        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            ScreenEvents.remove(screen).register {
                TriggerType.GUI_CLOSED.triggerAll(screen)
            }
        }

        CTEvents.PACKET_RECEIVED.register { packet, ctx ->
            JSLoader.wrapInContext(packetContext) {
                TriggerType.PACKET_RECEIVED.triggerAll(packet, ctx)
            }
        }

        CTEvents.RENDER_TICK.register {
            TriggerType.STEP.triggerAll()
        }

        CTEvents.RENDER_OVERLAY.register { stack, partialTicks ->
            Renderer.withGuiGraphics(stack, partialTicks) {
                TriggerType.RENDER_OVERLAY.triggerAll()
            }
        }

        CTEvents.RENDER_ENTITY.register { stack, entity, partialTicks, ci ->
            Renderer.withMatrix(stack, partialTicks) {
                TriggerType.RENDER_ENTITY.triggerAll(Entity.fromMC(entity), partialTicks, ci)
            }
        }

        CTEvents.RENDER_BLOCK_ENTITY.register { stack, blockEntity, partialTicks, ci ->
            Renderer.withMatrix(stack, partialTicks) {
                TriggerType.RENDER_BLOCK_ENTITY.triggerAll(BlockEntity(blockEntity), partialTicks, ci)
            }
        }

        AttackBlockCallback.EVENT.register { player, _, _, pos, direction ->
            if (!player.level().isClientSide) return@register InteractionResult.PASS
            val event = CancellableEvent()

            TriggerType.PLAYER_INTERACT.triggerAll(
                PlayerInteraction.AttackBlock,
                World.getBlockAt(BlockPos(pos)).withFace(BlockFace.fromMC(direction)),
                event,
            )

            if (event.isCancelled()) InteractionResult.FAIL else InteractionResult.PASS
        }

        AttackEntityCallback.EVENT.register { player, _, _, entity, _ ->
            if (!player.level().isClientSide) return@register InteractionResult.PASS
            val event = CancellableEvent()

            TriggerType.PLAYER_INTERACT.triggerAll(
                PlayerInteraction.AttackEntity,
                Entity.fromMC(entity),
                event,
            )

            if (event.isCancelled()) InteractionResult.FAIL else InteractionResult.PASS
        }

        CTEvents.BREAK_BLOCK.register { pos ->
            val event = CancellableEvent()
            TriggerType.PLAYER_INTERACT.triggerAll(PlayerInteraction.BreakBlock, World.getBlockAt(BlockPos(pos)), event)
            !event.isCancelled()
        }

        UseBlockCallback.EVENT.register { player, _, hand, hitResult ->
            if (!player.level().isClientSide) return@register InteractionResult.PASS
            val event = CancellableEvent()

            TriggerType.PLAYER_INTERACT.triggerAll(
                PlayerInteraction.UseBlock(hand),
                World.getBlockAt(BlockPos(hitResult.blockPos)).withFace(BlockFace.fromMC(hitResult.direction)),
                event,
            )

            if (event.isCancelled()) InteractionResult.FAIL else InteractionResult.PASS
        }

        UseEntityCallback.EVENT.register { player, _, hand, entity, _ ->
            if (!player.level().isClientSide) return@register InteractionResult.PASS
            val event = CancellableEvent()

            TriggerType.PLAYER_INTERACT.triggerAll(
                PlayerInteraction.UseEntity(hand),
                Entity.fromMC(entity),
                event,
            )

            if (event.isCancelled()) InteractionResult.FAIL else InteractionResult.PASS
        }

        UseItemCallback.EVENT.register { player, _, hand ->
            if (!player.level().isClientSide) return@register InteractionResult.PASS
            val event = CancellableEvent()

            val stack = player.getItemInHand(hand)

            TriggerType.PLAYER_INTERACT.triggerAll(
                PlayerInteraction.UseItem(hand),
                Item.fromMC(stack),
                event,
            )

            if (event.isCancelled()) InteractionResult.FAIL else InteractionResult.PASS
        }
    }

    internal fun addTask(delay: Int, owner: RuntimeOwner, callback: () -> Unit) =
        tasks.schedule(delay, owner, callback)

    internal fun pruneInvalidatedTasks() = tasks.pruneInvalidated()

    internal fun pendingTaskCount() = tasks.pendingCount()

    private fun handleChatMessage(message: Component, actionBar: Boolean): Boolean {
        val textComponent = TextComponent(message)
        val event = ChatTrigger.Event(textComponent)

        return if (actionBar) {
            actionBarHistory += textComponent
            if (actionBarHistory.size > 1000)
                actionBarHistory.removeAt(0)

            TriggerType.ACTION_BAR.triggerAll(event)
            !event.isCancelled()
        } else {
            chatHistory += textComponent
            if (chatHistory.size > 1000)
                chatHistory.removeAt(0)

            TriggerType.CHAT.triggerAll(event)

            !event.isCancelled()
        }
    }
}
