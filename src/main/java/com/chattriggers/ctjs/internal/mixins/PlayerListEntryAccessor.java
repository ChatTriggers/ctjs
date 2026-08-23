package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerInfo.class)
public interface PlayerListEntryAccessor {
    @Invoker("setLatency")
    void invokeSetLatency(int latency);
}
