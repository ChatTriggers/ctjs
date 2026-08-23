package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(CompoundTag.class)
public interface NbtCompoundAccessor {
    @Accessor("tags")
    Map<String, Tag> getEntries();
}
