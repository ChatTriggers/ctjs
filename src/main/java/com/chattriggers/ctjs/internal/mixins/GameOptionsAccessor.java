package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.client.Options;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Options.class)
public interface GameOptionsAccessor {
    @Accessor("keyMappings")
    void setAllKeys(KeyMapping[] keys);
}
