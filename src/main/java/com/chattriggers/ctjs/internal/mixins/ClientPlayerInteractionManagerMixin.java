package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.engine.CTEvents;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(
        method = "destroyBlock",
        at = @At("HEAD"),
        cancellable = true
    )
    private void injectBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!CTEvents.BREAK_BLOCK.invoker().breakBlock(pos)) {
            cir.setReturnValue(false);
        }
    }
}
