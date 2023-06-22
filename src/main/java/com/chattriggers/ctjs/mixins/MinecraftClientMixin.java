package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.triggers.TriggerType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "joinWorld", at = @At("TAIL"))
    private void injectJoinWorld(ClientWorld world, CallbackInfo ci) {
        // TODO(breaking): does not pass the event
        TriggerType.SERVER_CONNECT.triggerAll();
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
    private void injectDisconnect(Screen screen, CallbackInfo ci) {
        // TODO(breaking): does not pass the event
        TriggerType.SERVER_DISCONNECT.triggerAll();
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void injectScreenOpened(Screen screen, CallbackInfo ci) {
        if (screen != null)
            TriggerType.GUI_OPENED.triggerAll(screen, ci);
    }

    @Inject(method = "run", at = @At("HEAD"))
    private void injectRun(CallbackInfo ci) {
        TriggerType.GAME_LOAD.triggerAll();
    }
}
