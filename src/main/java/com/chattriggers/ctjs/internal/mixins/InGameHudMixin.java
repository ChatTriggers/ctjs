package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.world.Scoreboard;
import com.chattriggers.ctjs.internal.engine.CTEvents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void injectRenderScoreboard(DrawContext matrices, ScoreboardObjective objective, CallbackInfo ci) {
        if (!Scoreboard.getShouldRender())
            ci.cancel();
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderStatusEffectOverlay(Lnet/minecraft/client/gui/DrawContext;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void injectRenderOverlay(DrawContext drawContext, float tickDelta, CallbackInfo ci) {
        CTEvents.RENDER_OVERLAY.invoker().render(drawContext.getMatrices(), tickDelta);
    }
}
