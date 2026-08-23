package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.engine.CTEvents;
import com.chattriggers.ctjs.api.triggers.TriggerType;
import com.chattriggers.ctjs.internal.engine.module.ModuleManager;
import com.chattriggers.ctjs.internal.triggers.TriggerContractAdapters;
import com.chattriggers.ctjs.internal.triggers.TriggerLifecycleTransitions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {
    @Shadow @Nullable public ClientLevel level;

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void injectWorldUnload(ClientLevel world, CallbackInfo ci) {
        TriggerLifecycleTransitions.dispatchBeforeSetLevel(this.level != null, world != null);
    }

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void injectWorldLoad(ClientLevel world, CallbackInfo ci) {
        TriggerLifecycleTransitions.dispatchAfterSetLevel(world != null);
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void injectScreenOpened(Screen screen, CallbackInfo ci) {
        if (TriggerContractAdapters.shouldTriggerGuiOpened(screen))
            TriggerType.GUI_OPENED.triggerAll(screen, ci);
    }

    @Inject(method = "run", at = @At("HEAD"))
    private void injectRun(CallbackInfo ci) {
        // Minecraft.run executes on the client/render thread. Module entrypoints may
        // call ChatLib, create render resources, or otherwise access the client, so
        // they must not be moved to an arbitrary worker thread.
        ModuleManager.INSTANCE.entryPass();
        TriggerType.GAME_LOAD.triggerAll();
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void injectRender(boolean tick, CallbackInfo ci) {
        CTEvents.RENDER_GAME.invoker().run();
    }
}
