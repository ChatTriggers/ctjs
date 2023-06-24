package com.chattriggers.ctjs.minecraft.objects

import com.chattriggers.ctjs.engine.ILoader
import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.minecraft.wrappers.World
import com.chattriggers.ctjs.mixins.GameOptionsAccessor
import com.chattriggers.ctjs.mixins.KeyBindingAccessor
import com.chattriggers.ctjs.triggers.RegularTrigger
import com.chattriggers.ctjs.triggers.TriggerType
import com.chattriggers.ctjs.utils.Initializer
import com.chattriggers.ctjs.utils.asMixin
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.resource.language.I18n
import org.apache.commons.lang3.ArrayUtils
import java.util.concurrent.CopyOnWriteArrayList

abstract class KeyBind {
    private val keyBinding: KeyBinding
    private var onKeyPress: RegularTrigger? = null
    private var onKeyRelease: RegularTrigger? = null
    private var onKeyDown: RegularTrigger? = null

    private var down: Boolean = false

    init {
        @Suppress("LeakingThis")
        keyBinds.add(this)
    }

    /**
     * Creates a new keybind, editable in the user's controls.
     *
     * @param description what the keybind does
     * @param keyCode the keycode which the keybind will respond to, see Keyboard below. Ex. Keyboard.KEY_A
     * @param category the keybind category the keybind will be in
     * @see [org.lwjgl.input.Keyboard](http://legacy.lwjgl.org/javadoc/org/lwjgl/input/Keyboard.html)
     */
    @JvmOverloads
    constructor(description: String, keyCode: Int, category: String = "ChatTriggers") {
        val possibleDuplicate = Client.getMinecraft().options.allKeys.find {
            I18n.translate(it.translationKey) == I18n.translate(description) &&
                I18n.translate(it.category) == I18n.translate(category)
        }

        if (possibleDuplicate != null) {
            require(possibleDuplicate in customKeyBindings) {
                "KeyBind already exists! To get a KeyBind from an existing Minecraft KeyBinding, " +
                    "use the other KeyBind constructor or Client.getKeyBindFromKey."
            }
            keyBinding = possibleDuplicate
        } else {
            if (category !in KeyBindingAccessor.getKeyCategories()) {
                uniqueCategories[category] = 0
            }
            uniqueCategories[category] = uniqueCategories[category]!! + 1
            keyBinding = KeyBinding(description, keyCode, category)
            addRawKeyBinding(keyBinding)
            customKeyBindings.add(keyBinding)
        }
    }

    constructor(keyBinding: KeyBinding) {
        this.keyBinding = keyBinding
    }

    fun registerKeyPress(method: Any) = run {
        onKeyPress = RegularTrigger(method, TriggerType.OTHER, getLoader())
        onKeyPress
    }

    fun registerKeyRelease(method: Any) = run {
        onKeyRelease = RegularTrigger(method, TriggerType.OTHER, getLoader())
        onKeyRelease
    }

    fun registerKeyDown(method: Any) = run {
        onKeyDown = RegularTrigger(method, TriggerType.OTHER, getLoader())
        onKeyDown
    }

    fun unregisterKeyPress() = apply {
        onKeyPress?.unregister()
        onKeyPress = null
    }

    fun unregisterKeyRelease() = apply {
        onKeyRelease?.unregister()
        onKeyRelease = null
    }

    fun unregisterKeyDown() = apply {
        onKeyDown?.unregister()
        onKeyDown = null
    }

    internal fun onTick() {
        if (isPressed()) {
            onKeyPress?.trigger(arrayOf())
            down = true
        }

        if (isKeyDown()) {
            onKeyDown?.trigger(arrayOf())
            down = true
        }

        if (down && !isKeyDown()) {
            onKeyRelease?.trigger(arrayOf())
            down = false
        }
    }

    /**
     * Returns true if the key is pressed (used for continuous querying).
     *
     * @return whether the key is pressed
     */
    fun isKeyDown(): Boolean = keyBinding.isPressed

    /**
     * Returns true on the initial key press. For continuous querying use [isKeyDown].
     *
     * @return whether the key has just been pressed
     */
    fun isPressed(): Boolean = keyBinding.wasPressed()

    /**
     * Gets the description of the key.
     *
     * @return the description
     */
    fun getDescription(): String = keyBinding.translationKey

    /**
     * Gets the key code of the key.
     *
     * @return the integer key code
     */
    fun getKeyCode(): Int = keyBinding.asMixin<KeyBindingAccessor>().boundKey.code

    /**
     * Gets the category of the key.
     *
     * @return the category
     */
    fun getCategory(): String = keyBinding.category

    /**
     * Sets the state of the key.
     *
     * @param pressed True to press, False to release
     */
    fun setState(pressed: Boolean) = KeyBinding.setKeyPressed(keyBinding.asMixin<KeyBindingAccessor>().boundKey, pressed)

    internal abstract fun getLoader(): ILoader

    override fun toString() = "KeyBind{" +
        "description=${getDescription()}, " +
        "keyCode=${getKeyCode()}, " +
        "category=${getCategory()}" +
        "}"

    companion object : Initializer {
        private val customKeyBindings = mutableSetOf<KeyBinding>()
        private val uniqueCategories = mutableMapOf<String, Int>()
        private val keyBinds = CopyOnWriteArrayList<KeyBind>()

        fun getKeyBinds() = keyBinds

        override fun init() {
            ClientTickEvents.START_CLIENT_TICK.register {
                if (!World.isLoaded())
                    return@register

                keyBinds.forEach {
                    // This used to cause crashes on legacy sometimes. If it starts crashing again,
                    // we'll add the empty try-catch block back
                    it.onTick()
                }
            }
        }

        private fun removeKeyBinding(keyBinding: KeyBinding) {
            removeRawKeyBinding(keyBinding)
            val category = keyBinding.category

            if (category in uniqueCategories) {
                uniqueCategories[category] = uniqueCategories[category]!! - 1

                if (uniqueCategories[category] == 0) {
                    uniqueCategories.remove(category)
                    KeyBindingAccessor.getKeyCategories().remove(category)
                }
            }
        }

        @JvmStatic
        fun removeKeyBind(keyBind: KeyBind) {
            val keyBinding = keyBind.keyBinding
            if (keyBinding !in customKeyBindings) return

            removeKeyBinding(keyBinding)
            customKeyBindings.remove(keyBinding)
            keyBinds.remove(keyBind)
        }

        @JvmStatic
        fun clearKeyBinds() {
            keyBinds.toList().forEach(::removeKeyBind)
            customKeyBindings.clear()
            keyBinds.clear()
        }

        private fun addRawKeyBinding(keyBinding: KeyBinding) {
            Client.getMinecraft().options.asMixin<GameOptionsAccessor>().setAllKeys(ArrayUtils.add(
                Client.getMinecraft().options.allKeys,
                keyBinding
            ))
        }

        private fun removeRawKeyBinding(keyBinding: KeyBinding) {
            Client.getMinecraft().options.asMixin<GameOptionsAccessor>().setAllKeys(ArrayUtils.removeElement(
                Client.getMinecraft().options.allKeys,
                keyBinding
            ))
        }
    }
}
