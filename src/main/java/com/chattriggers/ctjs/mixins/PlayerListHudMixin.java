package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.triggers.TriggerType;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
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

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    //#if MC>=12000
    private void injectRenderPlayerList(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
    //#else
    //$$ private void injectRenderPlayerList(MatrixStack matrices, int scaledWindowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
    //#endif
        TriggerType.RENDER_PLAYER_LIST.triggerAll(ci);
    }
}
