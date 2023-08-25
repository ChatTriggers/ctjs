package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.minecraft.CTEvents;
import com.chattriggers.ctjs.triggers.TriggerType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow protected abstract boolean isConnectedToServer();

    @Inject(method = "joinWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ApiServices;create(Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;Ljava/io/File;)Lnet/minecraft/util/ApiServices;", shift = At.Shift.AFTER))
    private void injectJoinWorld(ClientWorld world, CallbackInfo ci) {
        if (!this.isConnectedToServer()) return;
        TriggerType.SERVER_CONNECT.triggerAll();
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
    private void injectDisconnect(Screen screen, CallbackInfo ci) {
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

    @Inject(method = "render", at = @At("HEAD"))
    private void injectRender(boolean tick, CallbackInfo ci) {
        CTEvents.RENDER_GAME.invoker().run();
    }
}
