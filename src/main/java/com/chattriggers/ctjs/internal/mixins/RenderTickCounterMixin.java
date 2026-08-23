package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.engine.CTEvents;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DeltaTracker.Timer.class)
public class RenderTickCounterMixin {
    @Inject(method = "advanceGameTime(J)I", at = @At("HEAD"))
    private void injectBeginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        CTEvents.RENDER_TICK.invoker().invoke();
    }
}
