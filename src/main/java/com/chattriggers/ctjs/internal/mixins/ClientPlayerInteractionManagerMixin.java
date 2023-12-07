package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.engine.CTEvents;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(
        method = "breakBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;onBroken(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V"
        )
    )
    private void injectBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        CTEvents.BREAK_BLOCK.invoker().breakBlock(pos);
    }
}
