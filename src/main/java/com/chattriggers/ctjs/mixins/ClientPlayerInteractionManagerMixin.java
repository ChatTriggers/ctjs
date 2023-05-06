package com.chattriggers.ctjs.mixins;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    void injectAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        // TODO: HitBlock trigger
    }
}
