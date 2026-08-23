package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.client.multiplayer.ClientChunkCache.Storage;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.atomic.AtomicReferenceArray;

@Mixin(Storage.class)
public interface ClientChunkMapAccessor {
    @Accessor("chunks")
    AtomicReferenceArray<LevelChunk> getChunks();
}
