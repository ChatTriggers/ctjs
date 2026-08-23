package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.triggers.TriggerType;
import com.chattriggers.ctjs.api.world.TabList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Objective;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerTabOverlay.class)
public class PlayerListHudMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void injectRenderPlayerList(GuiGraphicsExtractor context, int scaledWindowWidth, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
        TriggerType.RENDER_PLAYER_LIST.triggerAll(ci);
    }

    @Inject(method = "setHeader", at = @At("HEAD"), cancellable = true)
    private void ctjs$keepCustomHeader(Component header, CallbackInfo ci) {
        if (TabList.INSTANCE.observeServerHeader$ctjs(header)) {
            ci.cancel();
        }
    }

    @Inject(method = "setFooter", at = @At("HEAD"), cancellable = true)
    private void ctjs$keepCustomFooter(Component footer, CallbackInfo ci) {
        if (TabList.INSTANCE.observeServerFooter$ctjs(footer)) {
            ci.cancel();
        }
    }
}
