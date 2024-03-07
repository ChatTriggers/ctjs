package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.triggers.TriggerType;
import com.chattriggers.ctjs.internal.NameTagOverridable;
import com.chattriggers.ctjs.api.message.TextComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements NameTagOverridable {
    @Unique
    private TextComponent overriddenNametagName;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyVariable(method = "getDisplayName", at = @At(value = "STORE", ordinal = 0))
    private MutableText injectGetName(MutableText original) {
        if (overriddenNametagName != null)
            return overriddenNametagName.toMutableText$ctjs();
        return original;
    }

    @Inject(
        method = "attack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"
        )
    )
    private void chattriggers$entityDamage(Entity target, CallbackInfo ci) {
        if (getWorld().isClient) {
            TriggerType.ENTITY_DAMAGE.triggerAll(com.chattriggers.ctjs.api.entity.Entity.fromMC(target));
        }
    }

    @Inject(
        method = "attack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"
        )
    )
    private void chattriggers$entityDamageSweeping(Entity target, CallbackInfo ci) {
        if (getWorld().isClient) {
            TriggerType.ENTITY_DAMAGE.triggerAll(com.chattriggers.ctjs.api.entity.Entity.fromMC(target));
        }
    }

    @Override
    public void ctjs_setOverriddenNametagName(@Nullable TextComponent component) {
        overriddenNametagName = component;
    }
}
