package com.chattriggers.ctjs.api.client

import com.chattriggers.ctjs.api.triggers.RegularTrigger
import com.chattriggers.ctjs.api.triggers.TriggerType
import com.chattriggers.ctjs.api.world.World
import com.chattriggers.ctjs.internal.BoundKeyUpdater
import com.chattriggers.ctjs.internal.mixins.GameOptionsAccessor
import com.chattriggers.ctjs.internal.mixins.KeyBindingAccessor
import com.chattriggers.ctjs.internal.utils.Initializer
import com.chattriggers.ctjs.internal.utils.asMixin
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.resource.language.I18n
import org.apache.commons.lang3.ArrayUtils
import java.util.concurrent.CopyOnWriteArrayList

class KeyBind {
    private val keyBinding: KeyBinding
    private var onKeyPress: RegularTrigger? = null
    private var onKeyRelease: RegularTrigger? = null
    private var onKeyDown: RegularTrigger? = null

    private var down: Boolean = false

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

            // We need to update the bound key for the KeyBind we just made to the previous binding,
            // just in case it existed last time the game was opened. This will only matter for the first
            // time launching the game, as subsequent CT loads will cause possibleDuplicate to be found.
            Client.getMinecraft().options.asMixin<BoundKeyUpdater>().ctjs_updateBoundKey(keyBinding)
            KeyBinding.updateKeysByCode()

            addKeyBinding(keyBinding)
            customKeyBindings.add(keyBinding)
        }

        keyBinds.add(this)
    }

    constructor(keyBinding: KeyBinding) {
        this.keyBinding = keyBinding
        keyBinds.add(this)
    }

    fun registerKeyPress(method: Any) = apply {
        onKeyPress = RegularTrigger(method, TriggerType.OTHER)
    }

    fun registerKeyRelease(method: Any) = apply {
        onKeyRelease = RegularTrigger(method, TriggerType.OTHER)
    }

    fun registerKeyDown(method: Any) = apply {
        onKeyDown = RegularTrigger(method, TriggerType.OTHER)
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
        if (isPressed() && !down) {
            if (keyBinding in customKeyBindings) {
                while (keyBinding.wasPressed()) {
                    // consume the key press if not built-in keybinding
                }
            }

            onKeyPress?.trigger(arrayOf())
            down = true
        }

        if (isKeyDown()) {
            onKeyDown?.trigger(arrayOf())
            down = true
        }

        if (down && !isKeyDown()) {
            while (keyBinding.wasPressed()) {
                // consume the rest of the key presses
            }

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
    fun isPressed(): Boolean = keyBinding.asMixin<KeyBindingAccessor>().timesPressed > 0

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
    fun setState(pressed: Boolean) =
        KeyBinding.setKeyPressed(keyBinding.asMixin<KeyBindingAccessor>().boundKey, pressed)

    override fun toString() = "KeyBind{" +
        "description=${getDescription()}, " +
        "keyCode=${getKeyCode()}, " +
        "category=${getCategory()}" +
        "}"

    companion object : Initializer {
        private val customKeyBindings = mutableSetOf<KeyBinding>()
        private val uniqueCategories = mutableMapOf<String, Int>()
        private val keyBinds = CopyOnWriteArrayList<KeyBind>()

        internal fun getKeyBinds() = keyBinds

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

        internal fun clearKeyBinds() {
            keyBinds.toList().forEach(::removeKeyBind)
            customKeyBindings.clear()
            keyBinds.clear()
        }

        private fun removeKeyBinding(keyBinding: KeyBinding) {
            Client.getMinecraft().options.asMixin<GameOptionsAccessor>().setAllKeys(
                ArrayUtils.removeElement(
                    Client.getMinecraft().options.allKeys,
                    keyBinding
                )
            )
            val category = keyBinding.category

            if (category in uniqueCategories) {
                uniqueCategories[category] = uniqueCategories[category]!! - 1

                if (uniqueCategories[category] == 0) {
                    uniqueCategories.remove(category)
                    KeyBindingAccessor.getKeyCategories().remove(category)
                }
            }
        }

        private fun removeKeyBind(keyBind: KeyBind) {
            val keyBinding = keyBind.keyBinding
            if (keyBinding !in customKeyBindings) return

            removeKeyBinding(keyBinding)
            customKeyBindings.remove(keyBinding)
            keyBinds.remove(keyBind)
        }

        private fun addKeyBinding(keyBinding: KeyBinding): KeyBinding {
            Client.getMinecraft().options.asMixin<GameOptionsAccessor>().setAllKeys(
                ArrayUtils.add(
                    Client.getMinecraft().options.allKeys,
                    keyBinding
                )
            )

            val categoryMap = KeyBindingAccessor.getCategoryMap()
            val maxInt = categoryMap.values.max() ?: 0
            categoryMap[keyBinding.category] = maxInt + 1

            return keyBinding
        }
    }
}
