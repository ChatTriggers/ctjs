package com.chattriggers.ctjs.api.triggers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TriggerContractTableTest {
    private enum class Cancellation {
        YES,
        NO,
        CONDITIONAL,
        CALLER_DEFINED,
    }

    private data class Contract(
        val arguments: List<String>,
        val cancellation: Cancellation,
        val variableArity: Boolean = false,
    )

    /**
     * The JS-visible callback contract. Strings intentionally describe union and
     * nullable types that cannot be represented by a single JVM Class.
     */
    private val contracts = mapOf(
        TriggerType.CHAT to Contract(listOf("criteria captures...", "ChatTrigger.Event"), Cancellation.YES, true),
        TriggerType.ACTION_BAR to Contract(listOf("criteria captures...", "ChatTrigger.Event"), Cancellation.YES, true),
        TriggerType.TICK to Contract(listOf("Long"), Cancellation.NO),
        TriggerType.STEP to Contract(listOf("Long"), Cancellation.NO),
        TriggerType.GAME_UNLOAD to Contract(emptyList(), Cancellation.NO),
        TriggerType.GAME_LOAD to Contract(emptyList(), Cancellation.NO),
        TriggerType.CLICKED to Contract(listOf("Double", "Double", "Int", "Boolean"), Cancellation.NO),
        TriggerType.SCROLLED to Contract(listOf("Double", "Double", "Double"), Cancellation.NO),
        TriggerType.DRAGGED to Contract(listOf("Double", "Double", "Double", "Double", "Int"), Cancellation.NO),
        TriggerType.GUI_OPENED to Contract(listOf("Screen", "CallbackInfo"), Cancellation.YES),
        TriggerType.MESSAGE_SENT to Contract(listOf("String", "CancellableEvent"), Cancellation.YES),
        TriggerType.ITEM_TOOLTIP to Contract(listOf("MutableList<TextComponent>", "Item", "CallbackInfo"), Cancellation.YES),
        TriggerType.PLAYER_INTERACT to Contract(listOf("PlayerInteraction", "Entity|Block|Item", "CancellableEvent"), Cancellation.YES),
        TriggerType.GUI_KEY to Contract(listOf("String?", "Int", "Screen", "CancellableEvent"), Cancellation.YES),
        TriggerType.GUI_MOUSE_CLICK to Contract(listOf("Double", "Double", "Int", "Boolean", "Screen", "CancellableEvent"), Cancellation.YES),
        TriggerType.GUI_MOUSE_DRAG to Contract(listOf("Double", "Double", "Double", "Double", "Int", "Screen", "CallbackInfo"), Cancellation.YES),
        TriggerType.PACKET_SENT to Contract(listOf("Packet", "CallbackInfo"), Cancellation.YES),
        TriggerType.PACKET_RECEIVED to Contract(listOf("Packet", "CallbackInfo"), Cancellation.YES),
        TriggerType.SERVER_CONNECT to Contract(emptyList(), Cancellation.NO),
        TriggerType.SERVER_DISCONNECT to Contract(emptyList(), Cancellation.NO),
        TriggerType.GUI_CLOSED to Contract(listOf("Screen"), Cancellation.NO),
        TriggerType.DROP_ITEM to Contract(listOf("Item", "Boolean", "CancellableEvent|CallbackInfo"), Cancellation.YES),
        TriggerType.PRE_RENDER_WORLD to Contract(listOf("Float"), Cancellation.NO),
        TriggerType.POST_RENDER_WORLD to Contract(listOf("Float"), Cancellation.NO),
        TriggerType.BLOCK_HIGHLIGHT to Contract(listOf("BlockPos", "CancellableEvent"), Cancellation.YES),
        TriggerType.RENDER_OVERLAY to Contract(emptyList(), Cancellation.NO),
        TriggerType.RENDER_PLAYER_LIST to Contract(listOf("CallbackInfo"), Cancellation.YES),
        TriggerType.RENDER_ENTITY to Contract(listOf("Entity", "Float", "CallbackInfo"), Cancellation.YES),
        TriggerType.RENDER_BLOCK_ENTITY to Contract(listOf("BlockEntity", "Float", "CallbackInfo"), Cancellation.YES),
        TriggerType.GUI_RENDER to Contract(listOf("Int", "Int", "Screen"), Cancellation.NO),
        TriggerType.POST_GUI_RENDER to Contract(listOf("Int", "Int", "Screen", "Float"), Cancellation.NO),
        TriggerType.SOUND_PLAY to Contract(listOf("Vec3f", "String", "Float", "Float", "SoundSource", "CallbackInfo"), Cancellation.YES),
        TriggerType.WORLD_LOAD to Contract(emptyList(), Cancellation.NO),
        TriggerType.WORLD_UNLOAD to Contract(emptyList(), Cancellation.NO),
        TriggerType.SPAWN_PARTICLE to Contract(listOf("Particle", "CallbackInfo"), Cancellation.YES),
        TriggerType.ENTITY_DEATH to Contract(listOf("Entity"), Cancellation.NO),
        TriggerType.ENTITY_DAMAGE to Contract(listOf("Entity"), Cancellation.NO),
        TriggerType.COMMAND to Contract(listOf("String arguments..."), Cancellation.NO, true),
        TriggerType.OTHER to Contract(listOf("caller-defined arguments..."), Cancellation.CALLER_DEFINED, true),
    )

    @Test
    fun `every public trigger type has exactly one contract row`() {
        assertEquals(TriggerType.entries.toSet(), contracts.keys)
        assertEquals(TriggerType.entries.size, contracts.size)
    }

    @Test
    fun `fixed P0 callback argument order and types are explicit`() {
        assertEquals(listOf("Long"), contracts.getValue(TriggerType.STEP).arguments)
        assertEquals(listOf("Entity"), contracts.getValue(TriggerType.ENTITY_DEATH).arguments)
        assertEquals(listOf("BlockPos", "CancellableEvent"), contracts.getValue(TriggerType.BLOCK_HIGHLIGHT).arguments)
        assertEquals(
            listOf("MutableList<TextComponent>", "Item", "CallbackInfo"),
            contracts.getValue(TriggerType.ITEM_TOOLTIP).arguments,
        )
        assertEquals(listOf("Screen", "CallbackInfo"), contracts.getValue(TriggerType.GUI_OPENED).arguments)
        assertTrue(contracts.getValue(TriggerType.SERVER_DISCONNECT).arguments.isEmpty())
    }

    @Test
    fun `fixed P0 cancellation semantics are explicit`() {
        assertEquals(Cancellation.YES, contracts.getValue(TriggerType.ITEM_TOOLTIP).cancellation)
        assertEquals(Cancellation.YES, contracts.getValue(TriggerType.GUI_OPENED).cancellation)
        assertEquals(Cancellation.YES, contracts.getValue(TriggerType.BLOCK_HIGHLIGHT).cancellation)
        assertEquals(Cancellation.NO, contracts.getValue(TriggerType.ENTITY_DEATH).cancellation)
        assertEquals(Cancellation.NO, contracts.getValue(TriggerType.SERVER_DISCONNECT).cancellation)
        assertFalse(contracts.getValue(TriggerType.STEP).variableArity)
    }

    @Test
    fun `P1 edge callback types and cancellation are explicit`() {
        assertEquals("String?", contracts.getValue(TriggerType.GUI_KEY).arguments.first())
        assertEquals("SoundSource", contracts.getValue(TriggerType.SOUND_PLAY).arguments[4])
        assertEquals(Cancellation.YES, contracts.getValue(TriggerType.DROP_ITEM).cancellation)
        assertEquals(Cancellation.YES, contracts.getValue(TriggerType.PLAYER_INTERACT).cancellation)
    }
}
