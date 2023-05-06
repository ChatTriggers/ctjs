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
    void injectJoinWorld(ClientWorld world, CallbackInfo ci) {
        // TODO(breaking): does not pass the event
        TriggerType.ServerConnect.triggerAll();
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
    void injectDisconnect(Screen screen, CallbackInfo ci) {
        // TODO(breaking): does not pass the event
        TriggerType.ServerDisconnect.triggerAll();
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    void injectScreenOpened(Screen screen, CallbackInfo ci) {
        if (screen != null)
            TriggerType.GuiOpened.triggerAll(screen, ci);
    }
}
