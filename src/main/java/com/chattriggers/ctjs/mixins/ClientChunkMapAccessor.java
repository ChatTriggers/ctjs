package com.chattriggers.ctjs.mixins;

import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.atomic.AtomicReferenceArray;

@Mixin(targets = "net/minecraft/client/world/ClientChunkManager$ClientChunkMap")
public interface ClientChunkMapAccessor {
    @Accessor
    AtomicReferenceArray<WorldChunk> getChunks();
}
