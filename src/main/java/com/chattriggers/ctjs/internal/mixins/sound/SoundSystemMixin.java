package com.chattriggers.ctjs.internal.mixins.sound;

import com.chattriggers.ctjs.api.triggers.TriggerType;
import com.chattriggers.ctjs.api.vec.Vec3f;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void injectPlay(SoundInstance sound, CallbackInfo ci) {
        float volume = 0f;
        float pitch = 0f;

        try {
            volume = sound.getVolume();
        } catch (Throwable ignored) {
        }

        try {
            pitch = sound.getPitch();
        } catch (Throwable ignored) {
        }

        TriggerType.SOUND_PLAY.triggerAll(
            new Vec3f((float) sound.getX(), (float) sound.getY(), (float) sound.getZ()),
            sound.getId().toString(),
            volume,
            pitch,
            sound.getCategory(),
            ci
        );
    }
}
