package com.chattriggers.ctjs.internal.triggers

import com.chattriggers.ctjs.api.triggers.TriggerType
import com.chattriggers.ctjs.api.world.Scoreboard
import com.chattriggers.ctjs.api.world.TabList

/**
 * Classifies Minecraft world changes independently from `/ct reload`'s synthetic
 * WORLD_UNLOAD/WORLD_LOAD pair. A direct loaded-to-loaded replacement is a world
 * transition, not a server disconnect. A transition through no world produces one
 * disconnect and, if another world is installed later, one subsequent connect.
 */
object TriggerLifecycleTransitions {
    enum class Event {
        SERVER_CONNECT,
        SERVER_DISCONNECT,
        WORLD_LOAD,
        WORLD_UNLOAD,
    }

    @JvmStatic
    fun beforeSetLevel(wasLoaded: Boolean, willBeLoaded: Boolean): List<Event> = buildList {
        if (!wasLoaded && willBeLoaded)
            add(Event.SERVER_CONNECT)
        else if (wasLoaded && !willBeLoaded)
            add(Event.SERVER_DISCONNECT)

        if (wasLoaded)
            add(Event.WORLD_UNLOAD)
    }

    @JvmStatic
    fun afterSetLevel(willBeLoaded: Boolean): List<Event> =
        if (willBeLoaded) listOf(Event.WORLD_LOAD) else emptyList()

    @JvmStatic
    fun dispatchBeforeSetLevel(wasLoaded: Boolean, willBeLoaded: Boolean) {
        beforeSetLevel(wasLoaded, willBeLoaded).forEach(::dispatch)
    }

    @JvmStatic
    fun dispatchAfterSetLevel(willBeLoaded: Boolean) {
        afterSetLevel(willBeLoaded).forEach(::dispatch)
    }

    private fun dispatch(event: Event) {
        when (event) {
            Event.SERVER_CONNECT -> TriggerType.SERVER_CONNECT.triggerAll()
            Event.SERVER_DISCONNECT -> TriggerType.SERVER_DISCONNECT.triggerAll()
            Event.WORLD_LOAD -> TriggerType.WORLD_LOAD.triggerAll()
            Event.WORLD_UNLOAD -> {
                TriggerType.WORLD_UNLOAD.triggerAll()
                Scoreboard.clearCustom()
                TabList.clearCustom()
            }
        }
    }
}
