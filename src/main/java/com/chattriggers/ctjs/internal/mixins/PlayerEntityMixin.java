package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.triggers.TriggerType;
import com.chattriggers.ctjs.internal.NameTagOverridable;
import com.chattriggers.ctjs.api.message.TextComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity implements NameTagOverridable {
    @Unique
    private TextComponent overriddenNametagName;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @ModifyVariable(method = "getDisplayName", at = @At(value = "STORE", ordinal = 0))
    private MutableComponent injectGetName(MutableComponent original) {
        if (overriddenNametagName != null)
            return overriddenNametagName.toMutableText$ctjs();
        return original;
    }

    @Inject(
        method = "attack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
        )
    )
    private void chattriggers$entityDamage(Entity target, CallbackInfo ci) {
        if (level().isClientSide()) {
            TriggerType.ENTITY_DAMAGE.triggerAll(com.chattriggers.ctjs.api.entity.Entity.fromMC(target));
        }
    }

    @Override
    public void ctjs_setOverriddenNametagName(@Nullable TextComponent component) {
        overriddenNametagName = component;
    }
}
