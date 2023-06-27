package com.chattriggers.ctjs.console

import com.chattriggers.ctjs.engine.js.JSLoader
import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.utils.Config
import com.chattriggers.ctjs.utils.Initializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

object ConsoleManager : Initializer {
    val generalConsole = RemoteConsoleHost(null)
    val jsConsole = RemoteConsoleHost(JSLoader)

    private val consoles = listOf(generalConsole, jsConsole)
    private val keyBinds = mutableMapOf<Console, KeyBinding>()

    override fun init() {
        keyBinds[generalConsole] = KeyBindingHelper.registerKeyBinding(KeyBinding(
            "chattriggers.key.binding.console.general",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "chattriggers.key.category.console",
        ))

        keyBinds[jsConsole] = KeyBindingHelper.registerKeyBinding(KeyBinding(
            "chattriggers.key.binding.console.js",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            "chattriggers.key.category.console",
        ))

        ClientTickEvents.END_CLIENT_TICK.register {
            for ((console, binding) in keyBinds) {
                if (binding.wasPressed())
                    console.show()
            }
        }
    }

    fun onConsoleSettingsChanged(settings: Config.ConsoleSettings) {
        consoles.forEach { it.onConsoleSettingsChanged(settings) }
    }

    fun clearConsoles() {
        if (Config.clearConsoleOnLoad) {
            Client.scheduleTask {
                consoles.forEach(Console::clear)
            }
        }
    }

    fun closeConsoles() {
        consoles.forEach(Console::close)
    }
}
