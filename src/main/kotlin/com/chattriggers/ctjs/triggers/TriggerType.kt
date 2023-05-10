package com.chattriggers.ctjs.triggers

import com.chattriggers.ctjs.engine.module.ModuleManager

// TODO(breaking): Removed a bunch of less-used triggers
enum class TriggerType {
    // client
    Chat,
    ActionBar,
    Tick,
    Step,
    GameUnload,
    GameLoad,
    Clicked,
    Scrolled,
    Dragged,
    GuiOpened,
    MessageSent,
    Tooltip,
    PlayerInteract,
    HitBlock,
    GuiRender,
    GuiKey,
    GuiMouseClick,
    GuiMouseRelease,
    GuiMouseDrag,
    PacketSent,
    PacketReceived,
    ServerConnect,
    ServerDisconnect,
    GuiClosed,

    // rendering
    PreRenderWorld,
    PostRenderWorld,
    BlockHighlight,
    RenderOverlay,
    RenderPlayerList,
    RenderEntity,
    PostGuiRender,

    // world
    SoundPlay,
    WorldLoad,
    WorldUnload,
    BlockBreak,
    SpawnParticle,
    EntityDeath,
    EntityDamage,

    // misc
    Command,
    Other;

    fun triggerAll(vararg args: Any?) {
        ModuleManager.trigger(this, args)
    }
}
