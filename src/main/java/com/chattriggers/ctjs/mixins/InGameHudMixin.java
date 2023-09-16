package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.minecraft.CTEvents;
import com.chattriggers.ctjs.minecraft.wrappers.Scoreboard;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12000
import net.minecraft.client.gui.DrawContext;
//#else
//$$ import net.minecraft.client.util.math.MatrixStack;
//#endif

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    //#if MC>=12000
    private void injectRenderScoreboard(DrawContext matrices, ScoreboardObjective objective, CallbackInfo ci) {
    //#else
    //$$ private void injectRenderScoreboard(MatrixStack matrices, ScoreboardObjective objective, CallbackInfo ci) {
    //#endif
        if (!Scoreboard.getShouldRender())
            ci.cancel();
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            //#if MC>=12000
            target = "Lnet/minecraft/client/gui/hud/InGameHud;renderStatusEffectOverlay(Lnet/minecraft/client/gui/DrawContext;)V",
            //#else
            //$$ target = "Lnet/minecraft/client/gui/hud/InGameHud;renderStatusEffectOverlay(Lnet/minecraft/client/util/math/MatrixStack;)V",
            //#endif
            shift = At.Shift.AFTER
        )
    )
    //#if MC>=12000
    private void injectRenderOverlay(DrawContext drawContext, float tickDelta, CallbackInfo ci) {
        CTEvents.RENDER_OVERLAY.invoker().render(drawContext.getMatrices(), tickDelta);
    //#else
    //$$ private void injectRenderOverlay(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
    //$$     CTEvents.RENDER_OVERLAY.invoker().render(matrices, tickDelta);
    //#endif
    }
}
