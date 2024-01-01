package com.chattriggers.ctjs.internal.compat

import com.chattriggers.ctjs.api.Config
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

internal class ModMenuEntry : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory {
            Config.gui()
        }
    }
}
