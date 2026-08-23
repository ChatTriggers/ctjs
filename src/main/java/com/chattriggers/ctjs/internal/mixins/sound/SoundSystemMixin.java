package com.chattriggers.ctjs.internal.mixins.sound;

import com.chattriggers.ctjs.api.triggers.TriggerType;
import com.chattriggers.ctjs.api.vec.Vec3f;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class SoundSystemMixin {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void injectPlay(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> ci) {
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
            sound.getIdentifier().toString(),
            volume,
            pitch,
            sound.getSource(),
            ci
        );

        if (ci.isCancelled()) {
            ci.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}
