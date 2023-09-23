package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.triggers.TriggerType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
    @Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    private void injectAddParticle(Particle particle, CallbackInfo ci) {
        TriggerType.SPAWN_PARTICLE.triggerAll(new com.chattriggers.ctjs.api.entity.Particle(particle), ci);
    }
}
