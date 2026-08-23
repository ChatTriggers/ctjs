package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientLevel.class)
public interface ClientWorldAccessor {
    @Accessor("chunkSource")
    ClientChunkCache getChunkManager();
}
