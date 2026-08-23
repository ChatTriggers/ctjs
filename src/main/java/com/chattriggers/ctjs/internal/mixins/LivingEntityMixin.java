package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.triggers.TriggerType;
import com.chattriggers.ctjs.internal.triggers.TriggerContractAdapters;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setPose(Lnet/minecraft/world/entity/Pose;)V"))
    private void chattriggers$entityDeath(DamageSource damageSource, CallbackInfo ci) {
        if (level().isClientSide()) {
            TriggerType.ENTITY_DEATH.triggerAll(TriggerContractAdapters.entity(this));
        }
    }
}
