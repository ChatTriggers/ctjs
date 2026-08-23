package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.client.multiplayer.ClientChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientChunkCache.class)
public interface ClientChunkManagerAccessor {
    @Accessor("storage")
    ClientChunkCache.Storage getChunks();
}
