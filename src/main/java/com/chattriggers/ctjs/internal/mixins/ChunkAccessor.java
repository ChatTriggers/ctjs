package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ChunkAccess.class)
public interface ChunkAccessor {
    @Accessor
    Map<BlockPos, BlockEntity> getBlockEntities();
}
