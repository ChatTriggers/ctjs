package com.chattriggers.ctjs.minecraft.wrappers

import com.chattriggers.ctjs.minecraft.listeners.ClientListener
import com.chattriggers.ctjs.minecraft.objects.KeyBind
import com.chattriggers.ctjs.minecraft.objects.TextComponent
import com.chattriggers.ctjs.minecraft.wrappers.inventory.Slot
import com.chattriggers.ctjs.mixins.ChatScreenAccessor
import com.chattriggers.ctjs.mixins.HandledScreenAccessor
import com.chattriggers.ctjs.mixins.KeyBindingAccessor
import com.chattriggers.ctjs.utils.asMixin
import gg.essential.universal.UKeyboard
import gg.essential.universal.UMinecraft
import gg.essential.universal.UMouse
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.hud.ChatHud
import net.minecraft.client.gui.hud.PlayerListHud
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.gui.screen.ConnectScreen
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.ServerAddress
import net.minecraft.client.network.ServerInfo
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.realms.gui.screen.RealmsMainScreen
import net.minecraft.network.packet.Packet
import org.lwjgl.glfw.GLFW
import org.lwjgl.system.MemoryUtil
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
    fun getMinecraft(): MinecraftClient = UMinecraft.getMinecraft()

    /**
     * Gets Minecraft's NetHandlerPlayClient object
     *
     * @return The NetHandlerPlayClient object
     */
    fun getConnection(): ClientPlayNetworkHandler? = getMinecraft().networkHandler

    /**
     * Schedule's a task to run on Minecraft's main thread in [delay] ticks.
     * Defaults to the next tick.
     * @param delay The delay in ticks
     * @param callback The task to run on the main thread
     */
    @JvmOverloads
    fun scheduleTask(delay: Int = 0, callback: () -> Unit) {
        ClientListener.addTask(delay, callback)
    }

    /**
     * Quits the client back to the main menu.
     * This acts just like clicking the "Disconnect" or "Save and quit to title" button.
     */
    fun disconnect() {
        scheduleTask {
            World.toMC()?.disconnect()
            getMinecraft().disconnect()

            getMinecraft().setScreen(
                when {
                    getMinecraft().isInSingleplayer -> TitleScreen()
                    getMinecraft().isConnectedToRealms -> RealmsMainScreen(TitleScreen())
                    else -> MultiplayerScreen(TitleScreen())
                }
            )
        }
    }

    /**
     * Connects to the server with the given ip.
     * @param ip The ip to connect to
     */
    fun connect(ip: String, port: Int = 25565) {
        scheduleTask {
            ConnectScreen.connect(
                MultiplayerScreen(TitleScreen()),
                getMinecraft(),
                ServerAddress(ip, port),
                ServerInfo("Server", ip, false),
                //#if MC>=12000
                false,
                //#endif
            )
        }
    }

    /**
     * Gets the Minecraft ChatHud object for the chat gui
     *
     * @return The GuiNewChat object for the chat gui
     */
    fun getChatGUI(): ChatHud? = getMinecraft().inGameHud?.chatHud

    fun isInChat(): Boolean = getMinecraft().currentScreen is ChatScreen

    fun getTabGui(): PlayerListHud? = getMinecraft().inGameHud?.playerListHud

    fun isInTab(): Boolean = getMinecraft().options.playerListKey.isPressed

    /**
     * Gets whether the Minecraft window is active
     * and in the foreground of the user's screen.
     *
     * @return true if the game is active, false otherwise
     */
    fun isTabbedIn(): Boolean = getMinecraft().isWindowFocused

    fun isControlDown(): Boolean = UKeyboard.isCtrlKeyDown()

    fun isShiftDown(): Boolean = UKeyboard.isShiftKeyDown()

    fun isAltDown(): Boolean = UKeyboard.isAltKeyDown()

    fun getFPS(): Int = getMinecraft().currentFps

    fun getVersion(): String = getMinecraft().gameVersion

    fun getMaxMemory(): Long = Runtime.getRuntime().maxMemory()

    fun getTotalMemory(): Long = Runtime.getRuntime().totalMemory()

    fun getFreeMemory(): Long = Runtime.getRuntime().freeMemory()

    fun getMemoryUsage(): Int = ((getTotalMemory() - getFreeMemory()) * 100 / getMaxMemory().toFloat()).roundToInt()

    fun getSystemTime(): Long = (System.nanoTime() - referenceSystemTime) / 1_000_000

    fun getMouseX() = UMouse.Scaled.x

    fun getMouseY() = UMouse.Scaled.y

    fun isInGui(): Boolean = currentGui.get() != null

    /**
     * Gets the chat message currently typed into the chat gui.
     *
     * @return A blank string if the gui isn't open, otherwise, the message
     */
    fun getCurrentChatMessage(): String {
        return if (isInChat()) {
            val chatGui = getMinecraft().currentScreen as ChatScreen
            chatGui.asMixin<ChatScreenAccessor>().chatField.text
        } else ""
    }

    /**
     * Sets the current chat message, if the chat gui is not open, one will be opened.
     *
     * @param message the message to put in the chat text box.
     */
    fun setCurrentChatMessage(message: String) {
        if (isInChat()) {
            val chatGui = getMinecraft().currentScreen as ChatScreen
            chatGui.asMixin<ChatScreenAccessor>().chatField.text = message
        } else getMinecraft().setScreen(ChatScreen(message))
    }

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
    fun showTitle(title: String?, subtitle: String?, fadeIn: Int, time: Int, fadeOut: Int) {
        getMinecraft().inGameHud.apply {
            setTitleTicks(fadeIn, time, fadeOut)
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
    @JvmOverloads
    fun copy(text: String = "") {
        getMinecraft().keyboard.clipboard = text
    }

    /**
     * Get the string currently on the clipboard
     */
    fun paste(): String = getMinecraft().keyboard.clipboard

    /**
     * Get the [KeyBinding] from an already existing Minecraft KeyBinding, otherwise, returns null.
     *
     * @param keyCode the keycode to search for, see Keyboard below. Ex. Keyboard.KEY_A
     * @return the [KeyBinding] from a Minecraft KeyBinding, or null if one doesn't exist
     * @see [org.lwjgl.input.Keyboard](http://legacy.lwjgl.org/javadoc/org/lwjgl/input/Keyboard.html)
     */
    fun getKeyBindFromKey(keyCode: Int): KeyBind? {
        return KeyBind.getKeyBinds().find { it.getKeyCode() == keyCode }
            ?: getMinecraft().options.allKeys
                .find { it.asMixin<KeyBindingAccessor>().boundKey.code == keyCode }
                ?.let(::KeyBind)
    }

    /**
     * Get the [KeyBinding] from an already existing Minecraft KeyBinding, else, return a new one.
     *
     * @param keyCode the keycode which the keybind will respond to, see Keyboard below. Ex. Keyboard.KEY_A
     * @param description the description of the keybind
     * @param category the keybind category the keybind will be in
     * @return the [KeyBinding] from a Minecraft KeyBinding, or a new one if one doesn't exist
     * @see [org.lwjgl.input.Keyboard](http://legacy.lwjgl.org/javadoc/org/lwjgl/input/Keyboard.html)
     */
    fun getKeyBindFromKey(keyCode: Int, description: String, category: String): KeyBind {
        return getKeyBindFromKey(keyCode) ?: KeyBind(description, keyCode, category)
    }

    /**
     * Get the [KeyBinding] from an already existing Minecraft KeyBinding, else, return a new one.
     * This will create the [KeyBinding] with the default category "ChatTriggers".
     *
     * @param keyCode the keycode to search for, see Keyboard below. Ex. Keyboard.KEY_A
     * @param description the description of the keybind
     * @return the [KeyBinding] from a Minecraft KeyBinding, or a new one if one doesn't exist
     * @see [org.lwjgl.input.Keyboard](http://legacy.lwjgl.org/javadoc/org/lwjgl/input/Keyboard.html)
     */
    fun getKeyBindFromKey(keyCode: Int, description: String): KeyBind {
        return getKeyBindFromKey(keyCode, description, "ChatTriggers")
    }

    /**
     * Get the [KeyBinding] from an already existing
     * Minecraft KeyBinding, otherwise, returns null.
     *
     * @param description the description of the keybind
     * @return the [KeyBinding], or null if one doesn't exist
     */
    fun getKeyBindFromDescription(description: String): KeyBind? {
        return KeyBind.getKeyBinds()
            .find { it.getDescription() == description }
            ?: getMinecraft().options.allKeys
                .find { it.translationKey == description }
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
        fun get(): Screen? = getMinecraft().currentScreen

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
            return if (screen is HandledScreen<*>) {
                screen.asMixin<HandledScreenAccessor>().invokeGetSlotAt(getMouseX(), getMouseY())?.let(::Slot)
            } else null
        }

        /**
         * Closes the currently open gui
         */
        fun close() {
            scheduleTask { Player.toMC()?.closeScreen() }
        }
    }

    class CameraWrapper {
        fun getX(): Double = getMinecraft().gameRenderer.camera.pos.x

        fun getY(): Double = getMinecraft().gameRenderer.camera.pos.y

        fun getZ(): Double = getMinecraft().gameRenderer.camera.pos.z
    }
}
