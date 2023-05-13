package com.chattriggers.ctjs.engine

import com.chattriggers.ctjs.minecraft.listeners.ClientListener
import com.chattriggers.ctjs.triggers.*
import java.lang.reflect.Method

@Suppress("unused")
interface IRegister {
    companion object {
        private val methodMap = mutableMapOf<String, Method>()
    }

    /**
     * Helper method register a trigger.
     *
     * Called by taking the original name of the method, i.e. `registerChat`,
     * removing the word register, and comparing it case-insensitively with
     * the methods below.
     *
     * @param triggerType the type of trigger
     * @param method The name of the method or the actual method to callback when the event is fired
     * @return The trigger for additional modification
     */
    fun register(triggerType: Any, method: Any): Trigger {
        require(triggerType is String) {
            "register() expects a String or Class as its first argument"
        }

        val func = methodMap.getOrPut(triggerType) {
            val name = triggerType.lowercase()

            this::class.java.methods.firstOrNull {
                it.name.lowercase() == "register$name"
            } ?: throw NoSuchMethodException("No trigger type named '$triggerType'")
        }

        return func.invoke(this, method) as Trigger
    }

    /**
     * Registers a new trigger that runs before a chat message is received.
     *
     * Passes through multiple arguments:
     * - Any number of chat criteria variables
     * - The chat event, which can be cancelled
     *
     * Available modifications:
     * - [ChatTrigger.triggerIfCanceled] Sets if triggered if event is already cancelled
     * - [ChatTrigger.setChatCriteria] Sets the chat criteria
     * - [ChatTrigger.setParameter] Sets the chat parameter
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerChat(method: Any): ChatTrigger {
        return ChatTrigger(method, TriggerType.Chat, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs before an action bar message is received.
     *
     * Passes through multiple arguments:
     * - Any number of chat criteria variables
     * - The chat event, which can be cancelled
     *
     * Available modifications:
     * - [ChatTrigger.triggerIfCanceled] Sets if triggered if event is already cancelled
     * - [ChatTrigger.setChatCriteria] Sets the chat criteria
     * - [ChatTrigger.setParameter] Sets the chat parameter
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerActionBar(method: Any): ChatTrigger {
        return ChatTrigger(method, TriggerType.ActionBar, getImplementationLoader())
    }

    /**
     * Registers a trigger that runs before the world loads.
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerWorldLoad(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.WorldLoad, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs before the world unloads.
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerWorldUnload(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.WorldUnload, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs before a mouse button is being pressed or released.
     *
     * Passes through four arguments:
     * - The mouse x position
     * - The mouse y position
     * - The mouse button
     * - The mouse button state (true if button is pressed, false otherwise)
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerClicked(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.Clicked, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs before the mouse is scrolled.
     *
     * Passes through three arguments:
     * - The mouse x position
     * - The mouse y position
     * - The scroll direction
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerScrolled(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.Scrolled, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs while a mouse button is being held down.
     *
     * Passes through five arguments:
     * - The mouse delta x position (relative to last frame)
     * - The mouse delta y position (relative to last frame)
     * - The mouse x position
     * - The mouse y position
     * - The mouse button
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerDragged(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.Dragged, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs before a sound is played.
     *
     * Passes through six arguments:
     * - The sound event's position
     * - The sound event's name
     * - The sound event's volume
     * - The sound event's pitch
     * - The sound event's category's name
     * - The sound event, which can be cancelled
     *
     * Available modifications:
     * - [SoundPlayTrigger.setCriteria] Sets the sound name criteria
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerSoundPlay(method: Any): SoundPlayTrigger {
        return SoundPlayTrigger(method, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs before every game tick.
     *
     * Passes through one argument:
     * - Ticks elapsed
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerTick(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.Tick, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs in predictable intervals. (60 per second by default)
     *
     * Passes through one argument:
     * - Steps elapsed
     *
     * Available modifications:
     * - [StepTrigger.setFps] Sets the fps, i.e. how many times this trigger will fire
     *      per second
     * - [StepTrigger.setDelay] Sets the delay in seconds, i.e. how many seconds it takes
     *      to fire. Overrides [StepTrigger.setFps].
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerStep(method: Any): StepTrigger {
        return StepTrigger(method, getImplementationLoader())
    }

    @Deprecated("Use renderPreWorld", ReplaceWith("registerPreRenderWorld"))
    fun registerRenderWorld(method: Any) = registerPreRenderWorld(method)

    /**
     * Registers a new trigger that runs before the world is drawn.
     *
     * Passes through one argument:
     * - Partial ticks elapsed
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerPreRenderWorld(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.PreRenderWorld, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs after the world is drawn.
     *
     * Passes through one argument:
     * - Partial ticks elapsed
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerPostRenderWorld(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.PostRenderWorld, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs before the overlay is drawn.
     *
     * Passes through one argument:
     * - The render event, which cannot be cancelled
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerRenderOverlay(method: Any): EventTrigger {
        return EventTrigger(method, TriggerType.RenderOverlay, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs before the player list is being drawn.
     *
     * Passes through one argument:
     * - The render event, which can be cancelled
     *
     * Available modifications:
     * - [EventTrigger.triggerIfCanceled] Sets if triggered if event is already cancelled
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerRenderPlayerList(method: Any): EventTrigger {
        return EventTrigger(method, TriggerType.RenderPlayerList, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs before the block highlight box is drawn.
     *
     * Passes through two arguments:
     * - The draw block highlight event's position
     * - The draw block highlight event, which can be cancelled
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerDrawBlockHighlight(method: Any): EventTrigger {
        return EventTrigger(method, TriggerType.BlockHighlight, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs after the game loads.
     *
     * This runs after the initial loading of the game directly after scripts are
     * loaded and after "/ct load" happens.
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerGameLoad(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.GameLoad, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs before the game unloads.
     *
     * This runs before shutdown of the JVM and before "/ct load" happens.
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerGameUnload(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.GameUnload, getImplementationLoader())
    }

    /**
     * Registers a new command that will run the method provided.
     *
     * Passes through multiple arguments:
     * - The arguments supplied to the command by the user
     *
     * Available modifications:
     * - [CommandTrigger.setCommandName] Sets the command name
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerCommand(method: Any): CommandTrigger {
        return CommandTrigger(method, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs when a new gui is first opened.
     *
     * Passes through one argument:
     * - The gui opened event, which can be cancelled
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerGuiOpened(method: Any): EventTrigger {
        return EventTrigger(method, TriggerType.GuiOpened, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs when a gui is closed.
     *
     * Passes through one argument:
     * - The gui that was closed
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerGuiClosed(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.GuiClosed, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs before a message is sent in chat.
     *
     * Passes through two arguments:
     * - The message
     * - The message event, which can be cancelled
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerMessageSent(method: Any): EventTrigger {
        return EventTrigger(method, TriggerType.MessageSent, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs when a tooltip is being rendered.
     * This allows for the user to modify what text is in the tooltip, and even the
     * ability to cancel rendering completely.
     *
     * Passes through three arguments:
     * - The list of lore to modify.
     * - The [Item] that this lore is attached to.
     * - The cancellable event.
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerItemTooltip(method: Any): EventTrigger {
        return EventTrigger(method, TriggerType.Tooltip, getImplementationLoader())
    }

    // TODO(breaking): Changes second argument
    /**
     * Registers a new trigger that runs before the player interacts.
     *
     * Passes through three arguments:
     * - The [ClientListener.PlayerInteraction]
     * - The object of interaction, depending on the interaction type. Either a
     *   [com.chattriggers.ctjs.minecraft.wrappers.entity.Entity],
     *   [com.chattriggers.ctjs.minecraft.wrappers.world.block.Block], or
     *   [com.chattriggers.ctjs.minecraft.wrappers.inventory.Item],
     * - The event, which can be cancelled
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerPlayerInteract(method: Any): EventTrigger {
        return EventTrigger(method, TriggerType.PlayerInteract, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs before the player breaks a block
     *
     * Passes through one argument:
     * - The block
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerBlockBreak(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.BlockBreak, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs before an entity is damaged
     *
     * Passes through two arguments:
     * - The target Entity that is damaged
     * - The PlayerMP attacker
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerEntityDamage(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.EntityDamage, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs before an entity dies
     *
     * Passes through one argument:
     * - The Entity that died
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerEntityDeath(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.EntityDeath, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs as a gui is rendered
     *
     * Passes through three arguments:
     * - The mouse x position
     * - The mouse y position
     * - The gui
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerGuiRender(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.GuiRender, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs whenever a key is typed with a gui open
     *
     * Passes through four arguments:
     * - The character pressed (e.g. 'd')
     * - The key code pressed (e.g. 41)
     * - The gui
     * - The event, which can be cancelled
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerGuiKey(method: Any): EventTrigger {
        return EventTrigger(method, TriggerType.GuiKey, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs whenever the mouse is clicked with a
     * gui open
     *
     * Passes through five arguments:
     * - The mouse x position
     * - The mouse y position
     * - The mouse button
     * - The gui
     * - The event, which can be cancelled
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerGuiMouseClick(method: Any): EventTrigger {
        return EventTrigger(method, TriggerType.GuiMouseClick, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs whenever a mouse button is released
     * with a gui open
     *
     * Passes through five arguments:
     * - The mouse x position
     * - The mouse y position
     * - The mouse button
     * - The gui
     * - The event, which can be cancelled
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerGuiMouseRelease(method: Any): EventTrigger {
        return EventTrigger(method, TriggerType.GuiMouseRelease, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs whenever a mouse button held and dragged
     * with a gui open
     *
     * Passes through five arguments:
     * - The mouse x position
     * - The mouse y position
     * - The mouse button
     * - The gui
     * - The event, which can be cancelled
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerGuiMouseDrag(method: Any): EventTrigger {
        return EventTrigger(method, TriggerType.GuiMouseDrag, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs whenever a packet is sent from the client to the server
     *
     * Passes through two arguments:
     * - The packet
     * - The event, which can be cancelled
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     * - [ClassFilterTrigger.setFilteredClasses] Sets the packet classes which this trigger
     *   gets fired for
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerPacketSent(method: Any): PacketTrigger {
        return PacketTrigger(method, TriggerType.PacketSent, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs whenever a packet is sent to the client from the server
     *
     * Passes through two arguments:
     * - The packet
     * - The event, which can be cancelled
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     * - [ClassFilterTrigger.setFilteredClasses] Sets the packet classes which this trigger
     *   gets fired for
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerPacketReceived(method: Any): PacketTrigger {
        return PacketTrigger(method, TriggerType.PacketReceived, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs whenever the player connects to a server
     *
     * Passes through one argument:
     * - The event, which cannot be cancelled
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerServerConnect(method: Any): EventTrigger {
        return EventTrigger(method, TriggerType.ServerConnect, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs whenever the player disconnects from a server
     *
     * Passes through two arguments:
     * - The event, which cannot be cancelled
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerServerDisconnect(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.ServerDisconnect, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs whenever an entity is rendered
     *
     * Passes through four arguments:
     * - The [com.chattriggers.ctjs.minecraft.wrappers.entity.Entity]
     * - The position as a [com.chattriggers.ctjs.utils.vec.Vec3f]
     * - The partial ticks
     * - The event, which can be cancelled
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     * - [ClassFilterTrigger.setFilteredClasses] Sets the entity classes which this trigger
     *   gets fired for
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    // fun registerRenderEntity(method: Any): RenderEntityTrigger {
    //     return RenderEntityTrigger(method, TriggerType.RenderEntity, getImplementationLoader())
    // }

    /**
     * Registers a new trigger that runs after an entity is rendered
     *
     * Passes through three arguments:
     * - The [com.chattriggers.ctjs.minecraft.wrappers.entity.Entity]
     * - The position as a [com.chattriggers.ctjs.utils.vec.Vec3f]
     * - The partial ticks
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     * - [ClassFilterTrigger.setFilteredClasses] Sets the entity classes which this trigger
     *   gets fired for
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    // fun registerPostRenderEntity(method: Any): RenderEntityTrigger {
    //     return RenderEntityTrigger(method, TriggerType.PostRenderEntity, getImplementationLoader())
    // }

    /**
     * Registers a new trigger that runs whenever a tile entity is rendered
     *
     * Passes through four arguments:
     * - The TileEntity
     * - The position as a [com.chattriggers.ctjs.utils.vec.Vec3f]
     * - The partial ticks
     * - The event, which can be cancelled
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     * - [ClassFilterTrigger.setFilteredClasses] Sets the tile entity classes which this trigger
     *   gets fired for
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    // fun registerRenderTileEntity(method: Any): RenderTileEntityTrigger {
    //     return RenderTileEntityTrigger(method, TriggerType.RenderTileEntity, getImplementationLoader())
    // }

    /**
     * Registers a new trigger that runs after a tile entity is rendered
     *
     * Passes through three arguments:
     * - The TileEntity
     * - The position as a [com.chattriggers.ctjs.utils.vec.Vec3f]
     * - The partial ticks
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     * - [ClassFilterTrigger.setFilteredClasses] Sets the tile entity classes which this trigger
     *   gets fired for
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    // fun registerPostRenderTileEntity(method: Any): RenderTileEntityTrigger {
    //     return RenderTileEntityTrigger(method, TriggerType.PostRenderTileEntity, getImplementationLoader())
    // }

    /**
     * Registers a new trigger that runs after the current screen is rendered
     *
     * Passes through three arguments:
     * - The mouseX
     * - The mouseY
     * - The GuiScreen
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerPostGuiRender(method: Any): RegularTrigger {
        return RegularTrigger(method, TriggerType.PostGuiRender, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs whenever a particle is spawned
     *
     * Passes through three arguments:
     * - The [com.chattriggers.ctjs.minecraft.wrappers.entity.Particle]
     * - The [net.minecraft.util.EnumParticleTypes]
     * - The event, which can be cancelled
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerSpawnParticle(method: Any): EventTrigger {
        return EventTrigger(method, TriggerType.SpawnParticle, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs whenever a block is left clicked
     *
     * Note: this is not continuously called while the block is being broken, only once
     * when first left clicked.
     *
     * Passes through two arguments:
     * - The [com.chattriggers.ctjs.minecraft.wrappers.world.block.Block] being hit
     * - The event, which can be cancelled
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerHitBlock(method: Any): EventTrigger {
        return EventTrigger(method, TriggerType.HitBlock, getImplementationLoader())
    }

    /**
     * Registers a new trigger that runs on a mixin event
     *
     * Passes through all of the argument to the method being injected into.
     * See the wiki for more information (TODO)
     *
     * Available modifications:
     * - [Trigger.setPriority] Sets the priority
     *
     * @param method The method to call when the event is fired
     * @return The trigger for additional modification
     */
    fun registerMixin(method: Any): MixinTrigger {
        return MixinTrigger(method, getImplementationLoader())
    }

    fun getImplementationLoader(): ILoader
}
