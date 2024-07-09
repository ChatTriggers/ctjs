package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.triggers.TriggerType;
import com.chattriggers.ctjs.api.world.TabList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void injectRenderPlayerList(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
        TriggerType.RENDER_PLAYER_LIST.triggerAll(ci);
    }

    @Inject(method = "setHeader", at = @At("HEAD"), cancellable = true)
    private void ctjs$keepCustomHeader(Text header, CallbackInfo ci) {
        if (TabList.INSTANCE.getCustomHeader$ctjs()) {
            ci.cancel();
        }
    }

    @Inject(method = "setFooter", at = @At("HEAD"), cancellable = true)
    private void ctjs$keepCustomFooter(Text footer, CallbackInfo ci) {
        if (TabList.INSTANCE.getCustomFooter$ctjs()) {
            ci.cancel();
        }
    }
}
