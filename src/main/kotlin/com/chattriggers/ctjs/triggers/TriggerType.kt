package com.chattriggers.ctjs.triggers

import com.chattriggers.ctjs.engine.js.JSLoader
import com.chattriggers.ctjs.utils.InternalApi

@InternalApi
sealed interface ITriggerType {
    val name: String

    @InternalApi
    fun triggerAll(vararg args: Any?) {
        JSLoader.exec(this, args)
    }
}

internal enum class TriggerType : ITriggerType {
    // client
    CHAT,
    ACTION_BAR,
    TICK,
    STEP,
    GAME_UNLOAD,
    GAME_LOAD,
    CLICKED,
    SCROLLED,
    DRAGGED,
    GUI_OPENED,
    MESSAGE_SENT,
    ITEM_TOOLTIP,
    PLAYER_INTERACT,
    GUI_KEY,
    GUI_MOUSE_CLICK,
    GUI_MOUSE_DRAG,
    PACKET_SENT,
    PACKET_RECEIVED,
    SERVER_CONNECT,
    SERVER_DISCONNECT,
    GUI_CLOSED,
    DROP_ITEM,

    // rendering
    PRE_RENDER_WORLD,
    POST_RENDER_WORLD,
    BLOCK_HIGHLIGHT,
    RENDER_OVERLAY,
    RENDER_PLAYER_LIST,
    RENDER_ENTITY,
    RENDER_BLOCK_ENTITY,
    GUI_RENDER,
    POST_GUI_RENDER,

    // world
    SOUND_PLAY,
    WORLD_LOAD,
    WORLD_UNLOAD,
    SPAWN_PARTICLE,
    ENTITY_DEATH,
    ENTITY_DAMAGE,

    // misc
    MIXIN,
    COMMAND,
    OTHER
}

internal data class CustomTriggerType(override val name: String) : ITriggerType
