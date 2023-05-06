package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.minecraft.CTEvents;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTickCounter.class)
public class RenderTickCounterMixin {
    @Inject(method = "beginRenderTick", at = @At("HEAD"))
    void injectBeginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        CTEvents.RENDER_TICK.invoker().invoke();
    }
}
