package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.client.sound.AbstractSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSoundInstance.class)
public interface AbstractSoundInstanceAccessor {
    @Accessor
    void setRepeat(boolean repeat);

    @Accessor
    void setRepeatDelay(int delay);
}
