package com.chattriggers.ctjs.api.triggers

import com.chattriggers.ctjs.internal.engine.JSLoader
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents

sealed interface ITriggerType {
    val name: String

    fun triggerAll(vararg args: Any?) {
        JSLoader.exec(this, args)
    }
}

enum class TriggerType : ITriggerType {
    RENDER_OVERLAY,
    RENDER_LEVEL_EXTRACTION,

    CHAT,
    ACTION_BAR,
    MESSAGE_SENT,

    TICK,
}

enum class RenderContextTriggerType(val register: ((LevelRenderContext) -> Unit) -> Unit) : ITriggerType {
    END_MAIN({ LevelRenderEvents.END_MAIN.register(it) }),
    BEFORE_GIZMOS({ LevelRenderEvents.BEFORE_GIZMOS.register(it) }),
    AFTER_TRANSLUCENT_TERRAIN({ LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(it) }),
    AFTER_SOLID_FEATURES({ LevelRenderEvents.AFTER_SOLID_FEATURES.register(it) }),
    AFTER_TRANSLUCENT_FEATURES({ LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(it) }),
    BEFORE_TRANSLUCENT_TERRAIN({ LevelRenderEvents.BEFORE_TRANSLUCENT_TERRAIN.register(it) }),
}

data class CustomTriggerType(override val name: String) : ITriggerType
