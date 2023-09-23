package com.chattriggers.ctjs.internal.mixins.sound;

import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(SoundSystem.class)
public interface SoundSystemAccessor {
    @Accessor
    Channel getChannel();

    @Accessor
    Map<SoundInstance, Channel.SourceManager> getSources();

    @Invoker
    void invokeStart();

    @Invoker
    void invokeTick();
}
