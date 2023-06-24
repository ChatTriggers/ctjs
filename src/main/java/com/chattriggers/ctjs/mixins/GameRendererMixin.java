package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.minecraft.CTEvents;
import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer;
import com.chattriggers.ctjs.triggers.TriggerType;
import gg.essential.universal.UMatrixStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Objects;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void injectPreRenderWorld(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo ci) {
        CTEvents.PRE_RENDER_WORLD.invoker().render(matrices, tickDelta);
    }

    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void injectPostRenderWorld(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo ci) {
        CTEvents.POST_RENDER_WORLD.invoker().render(matrices, tickDelta);
    }
}
