package com.chattriggers.ctjs.internal.mixins.sound;

import net.minecraft.client.sound.Sound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Sound.class)
public interface SoundAccessor {
    @Accessor
    @Mutable
    void setAttenuation(int attenuation);
}
