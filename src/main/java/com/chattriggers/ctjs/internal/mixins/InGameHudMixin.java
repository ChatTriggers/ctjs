package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.world.Scoreboard;
import com.chattriggers.ctjs.internal.engine.CTEvents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Gui.class)
public class InGameHudMixin {
    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void injectRenderScoreboard(GuiGraphicsExtractor matrices, Objective objective, CallbackInfo ci) {
        if (!Scoreboard.getShouldRender())
            ci.cancel();
    }

    @Inject(
        method = "extractRenderState",
        at = @At("TAIL")
    )
    private void injectRenderOverlay(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        CTEvents.RENDER_OVERLAY.invoker().render(context, tickCounter.getGameTimeDeltaPartialTick(false));
    }
}
