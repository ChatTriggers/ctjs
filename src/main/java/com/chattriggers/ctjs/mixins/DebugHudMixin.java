package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.minecraft.CTEvents;
import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer;
import com.chattriggers.ctjs.minecraft.listeners.CancellableEvent;
import com.chattriggers.ctjs.triggers.TriggerType;
import gg.essential.universal.UMatrixStack;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugHud.class)
public class DebugHudMixin {
    @Inject(method = "render", at = @At("HEAD"))
    void injectRender(MatrixStack matrices, CallbackInfo ci) {
        CTEvents.RENDER_OVERLAY.invoker().render(matrices);
    }
}
