package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.minecraft.wrappers.Scoreboard;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void injectRenderScoreboard(MatrixStack matrices, ScoreboardObjective objective, CallbackInfo ci) {
        if (!Scoreboard.getShouldRender())
            ci.cancel();
    }
}
