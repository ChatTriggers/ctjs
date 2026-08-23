package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyBindingAccessor {
    @Accessor("key")
    InputConstants.Key getBoundKey();

    @Accessor("clickCount")
    int getTimesPressed();
}
