package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.minecraft.CTEvents;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;getOverlay()Lnet/minecraft/client/gui/screen/Overlay;",
            ordinal = 1
        )
    )
    private void injectPreRender(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        CTEvents.PRE_RENDER_OVERLAY.invoker().render();
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;getOverlay()Lnet/minecraft/client/gui/screen/Overlay;",
            ordinal = 1,
            shift = At.Shift.AFTER
        )
    )
    private void injectPostRender(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        CTEvents.POST_RENDER_OVERLAY.invoker().render();
    }
}
