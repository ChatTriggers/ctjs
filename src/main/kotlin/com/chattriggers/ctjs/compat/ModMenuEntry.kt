package com.chattriggers.ctjs.compat

import com.chattriggers.ctjs.utils.Config
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

internal class ModMenuEntry : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory {
            Config.gui()
        }
    }
}
