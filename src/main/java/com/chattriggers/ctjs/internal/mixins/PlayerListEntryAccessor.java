package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerListEntry.class)
public interface PlayerListEntryAccessor {
    @Invoker
    void invokeSetLatency(int latency);
}
