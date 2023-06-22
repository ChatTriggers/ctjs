package com.chattriggers.ctjs.mixins;

import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    private String overriddenNametagName;

    public void setOverriddenNametagName(@Nullable String component) {
        overriddenNametagName = component;
    }

    @Inject(method = "getEntityName", at = @At("HEAD"), cancellable = true)
    private void injectGetName(CallbackInfoReturnable<String> cir) {
        if (overriddenNametagName != null)
            cir.setReturnValue(overriddenNametagName);
    }
}
