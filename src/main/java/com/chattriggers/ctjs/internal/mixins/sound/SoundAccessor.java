package com.chattriggers.ctjs.internal.mixins.sound;

import net.minecraft.client.resources.sounds.Sound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Sound.class)
public interface SoundAccessor {
    @Accessor("attenuationDistance")
    @Mutable
    void setAttenuation(int attenuation);
}
