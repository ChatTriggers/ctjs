package com.chattriggers.ctjs.api.client

import com.chattriggers.ctjs.api.inventory.Slot
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.api.world.World
import com.chattriggers.ctjs.internal.listeners.ClientListener
import com.chattriggers.ctjs.internal.lifecycle.RuntimeGenerations
import com.chattriggers.ctjs.internal.lifecycle.RuntimeOwner
import com.chattriggers.ctjs.internal.mixins.ChatScreenAccessor
import com.chattriggers.ctjs.internal.mixins.HandledScreenAccessor
import com.chattriggers.ctjs.internal.mixins.KeyBindingAccessor
import com.chattriggers.ctjs.internal.utils.asMixin
import gg.essential.universal.UKeyboard
import gg.essential.universal.UMinecraft
import gg.essential.universal.UMouse
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.gui.components.PlayerTabOverlay
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.multiplayer.resolver.ServerAddress
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.KeyMapping
import com.mojang.realmsclient.RealmsMainScreen
import net.minecraft.network.protocol.Packet
import net.minecraft.network.chat.Component
import kotlin.math.roundToInt

object Client {
    internal var referenceSystemTime: Long = 0

    @JvmField
    val currentGui = CurrentGuiWrapper()

    @JvmField
    val camera = CameraWrapper()

    @JvmField
    val settings = Settings

    /**
     * Gets Minecraft's Minecraft object
     *
     * @return The Minecraft object
     */
    @JvmStatic
    fun getMinecraft(): Minecraft = UMinecraft.getMinecraft()

    /**
     * Gets Minecraft's NetHandlerPlayClient object
     *
     * @return The NetHandlerPlayClient object
     */
    @JvmStatic
    fun getConnection(): ClientPacketListener? = getMinecraft().connection

    /**
     * Schedule's a task to run on Minecraft's main thread in [delay] ticks.
     * Defaults to the next tick.
     * @param delay The delay in ticks
     * @param callback The task to run on the main thread
     */
    @JvmStatic
    @JvmOverloads
    fun scheduleTask(delay: Int = 0, callback: () -> Unit) {
        scheduleOwnedTask(RuntimeGenerations.currentOwner(), delay, callback)
    }

    internal fun scheduleSystemTask(delay: Int = 0, callback: () -> Unit) {
        scheduleOwnedTask(RuntimeGenerations.systemOwner(), delay, callback)
    }

    internal fun scheduleOwnedTask(owner: RuntimeOwner, delay: Int = 0, callback: () -> Unit) {
        ClientListener.addTask(delay, owner, callback)
    }

    /**
     * Quits the client back to the main menu.
     * This acts just like clicking the "Disconnect" or "Save and quit to title" button.
     */
    @JvmStatic
    fun disconnect() {
        scheduleTask {
            getMinecraft().disconnectFromWorld(Component.translatable("menu.disconnect"))
        }
    }

    /**
     * Connects to the server with the given ip.
     * @param ip The ip to connect to
     */
    @JvmStatic
    @JvmOverloads
    fun connect(ip: String, port: Int = 25565) {
        scheduleTask {
            ConnectScreen.startConnecting(
                JoinMultiplayerScreen(TitleScreen()),
                getMinecraft(),
                ServerAddress(ip, port),
                ServerData("Server", ip, ServerData.Type.OTHER),
                false,
                null,
            )
        }
    }

    /**
     * Gets the Minecraft ChatComponent object for the chat gui
     *
     * @return The GuiNewChat object for the chat gui
     */
    @JvmStatic
    fun getChatGui(): ChatComponent? = getMinecraft().gui.chat

    @JvmStatic
    fun isInChat(): Boolean = getMinecraft().screen is ChatScreen

    @JvmStatic
    fun getTabGui(): PlayerTabOverlay? = getMinecraft().gui.tabList

    @JvmStatic
    fun isInTab(): Boolean = getMinecraft().options.keyPlayerList.isDown

    /**
     * Gets whether the Minecraft window is active
     * and in the foreground of the user's screen.
     *
     * @return true if the game is active, false otherwise
     */
    @JvmStatic
    fun isTabbedIn(): Boolean = getMinecraft().isWindowActive

    @JvmStatic
    fun isControlDown(): Boolean = UKeyboard.isCtrlKeyDown()

    @JvmStatic
    fun isShiftDown(): Boolean = UKeyboard.isShiftKeyDown()

    @JvmStatic
    fun isAltDown(): Boolean = UKeyboard.isAltKeyDown()

    @JvmStatic
    fun getFPS(): Int = getMinecraft().fps

    @JvmStatic
    fun getVersion(): String = getMinecraft().launchedVersion

    @JvmStatic
    fun getMaxMemory(): Long = Runtime.getRuntime().maxMemory()

    @JvmStatic
    fun getTotalMemory(): Long = Runtime.getRuntime().totalMemory()

    @JvmStatic
    fun getFreeMemory(): Long = Runtime.getRuntime().freeMemory()

    @JvmStatic
    fun getMemoryUsage(): Int = ((getTotalMemory() - getFreeMemory()) * 100 / getMaxMemory().toFloat()).roundToInt()

    @JvmStatic
    fun getSystemTime(): Long = (System.nanoTime() - referenceSystemTime) / 1_000_000

    @JvmStatic
    fun getMouseX() = UMouse.Scaled.x

    @JvmStatic
    fun getMouseY() = UMouse.Scaled.y

    @JvmStatic
    fun isInGui(): Boolean = currentGui.get() != null

    /**
     * Gets the chat message currently typed into the chat gui.
     *
     * @return A blank string if the gui isn't open, otherwise, the message
     */
    @JvmStatic
    fun getCurrentChatMessage(): String {
        return if (isInChat()) {
            val chatGui = getMinecraft().screen as ChatScreen
            chatGui.asMixin<ChatScreenAccessor>().chatField.value
        } else ""
    }

    /**
     * Sets the current chat message, if the chat gui is not open, one will be opened.
     *
     * @param message the message to put in the chat text box.
     */
    @JvmStatic
    fun setCurrentChatMessage(message: String) {
        if (isInChat()) {
            val chatGui = getMinecraft().screen as ChatScreen
            chatGui.asMixin<ChatScreenAccessor>().chatField.value = message
        } else currentGui.set(ChatScreen(message, false))
    }

    @JvmStatic
    fun sendPacket(packet: Packet<*>) {
        getConnection()?.connection?.send(packet)
    }

    /**
     * Display a title.
     *
     * @param title title text
     * @param subtitle subtitle text
     * @param fadeIn time to fade in
     * @param time time to stay on screen
     * @param fadeOut time to fade out
     */
    @JvmStatic
    fun showTitle(title: String?, subtitle: String?, fadeIn: Int, time: Int, fadeOut: Int) {
        getMinecraft().gui.apply {
            setTimes(fadeIn, time, fadeOut)
            if (title != null)
                setTitle(TextComponent(title))
            if (subtitle != null)
                setSubtitle(TextComponent(subtitle))
        }
    }

    /**
     * Copies a string to the clipboard
     *
     * @param text The text to copy
     */
    @JvmStatic
    @JvmOverloads
    fun copy(text: String = "") {
        getMinecraft().keyboardHandler.clipboard = text
    }

    /**
     * Get the string currently on the clipboard
     */
    @JvmStatic
    fun paste(): String = getMinecraft().keyboardHandler.clipboard

    /**
     * Get the [KeyMapping] from an already existing Minecraft KeyMapping, otherwise, returns null.
     *
     * @param keyCode the keycode to search for, see Keyboard below. Ex. Keyboard.KEY_A
     * @return the [KeyMapping] from a Minecraft KeyMapping, or null if one doesn't exist
     * @see [org.lwjgl.input.Keyboard](http://legacy.lwjgl.org/javadoc/org/lwjgl/input/Keyboard.html)
     */
    @JvmStatic
    fun getKeyBindFromKey(keyCode: Int): KeyBind? {
        return KeyBind.getKeyBinds().find { it.getKeyCode() == keyCode }
            ?: getMinecraft().options.keyMappings
                .find { it.asMixin<KeyBindingAccessor>().boundKey.value == keyCode }
                ?.let(::KeyBind)
    }

    /**
     * Get the [KeyMapping] from an already existing Minecraft KeyMapping, else, return a new one.
     *
     * @param keyCode the keycode which the keybind will respond to, see Keyboard below. Ex. Keyboard.KEY_A
     * @param description the description of the keybind
     * @param category the keybind category the keybind will be in
     * @return the [KeyMapping] from a Minecraft KeyMapping, or a new one if one doesn't exist
     * @see [org.lwjgl.input.Keyboard](http://legacy.lwjgl.org/javadoc/org/lwjgl/input/Keyboard.html)
     */
    @JvmStatic
    @JvmOverloads
    fun getKeyBindFromKey(keyCode: Int, description: String, category: String = "ChatTriggers"): KeyBind {
        return getKeyBindFromKey(keyCode) ?: KeyBind(description, keyCode, category)
    }

    /**
     * Get the [KeyMapping] from an already existing
     * Minecraft KeyMapping, otherwise, returns null.
     *
     * @param description the description of the keybind
     * @return the [KeyMapping], or null if one doesn't exist
     */
    @JvmStatic
    fun getKeyBindFromDescription(description: String): KeyBind? {
        return KeyBind.getKeyBinds()
            .find { it.getDescription() == description }
            ?: getMinecraft().options.keyMappings
                .find { it.name == description }
                ?.let(::KeyBind)
    }

    class CurrentGuiWrapper {
        /**
         * Gets the Java class name of the currently open gui, for example, "GuiChest"
         *
         * @return the class name of the current gui
         */
        fun getClassName(): String = get()?.javaClass?.simpleName ?: "null"

        /**
         * Gets the Minecraft gui class that is currently open
         *
         * @return the Minecraft gui
         */
        fun get(): Screen? = getMinecraft().screen

        fun set(screen: Screen?) {
            scheduleTask {
                getMinecraft().setScreen(screen)
            }
        }

        /**
         * Gets the slot under the mouse in the current gui, if one exists.
         *
         * @return the [Slot] under the mouse
         */
        fun getSlotUnderMouse(): Slot? {
            val screen: Screen? = get()
            return if (screen is AbstractContainerScreen<*>) {
                screen.asMixin<HandledScreenAccessor>().invokeGetHoveredSlot(getMouseX(), getMouseY())?.let(::Slot)
            } else null
        }

        /**
         * Closes the currently open gui
         */
        fun close() {
            scheduleTask { Player.toMC()?.closeContainer() }
        }
    }

    class CameraWrapper {
        fun getX(): Double = getMinecraft().gameRenderer.mainCamera.position().x

        fun getY(): Double = getMinecraft().gameRenderer.mainCamera.position().y

        fun getZ(): Double = getMinecraft().gameRenderer.mainCamera.position().z
    }
}
