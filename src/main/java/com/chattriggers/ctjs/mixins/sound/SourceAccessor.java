package com.chattriggers.ctjs.mixins.sound;

import net.minecraft.client.sound.Source;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Source.class)
public interface SourceAccessor {
    @Invoker
    int invokeGetSourceState();
}
