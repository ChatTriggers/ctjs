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
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;getOverlay()Lnet/minecraft/client/gui/screen/Overlay;",
            ordinal = 1
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectPreRenderOverlay(float tickDelta, long startTime, boolean tick, CallbackInfo ci, int i, int j, Window window, Matrix4f matrix4f, MatrixStack matrixStack, MatrixStack matrixStack2) {
        MinecraftClient client = MinecraftClient.getInstance();
        Overlay overlay = Objects.requireNonNull(client.getOverlay());
        CTEvents.PRE_RENDER_OVERLAY.invoker().render(matrixStack2, i, j, overlay, client.getLastFrameDuration());
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;getOverlay()Lnet/minecraft/client/gui/screen/Overlay;",
            ordinal = 1,
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectPostRenderOverlay(float tickDelta, long startTime, boolean tick, CallbackInfo ci, int i, int j, Window window, Matrix4f matrix4f, MatrixStack matrixStack, MatrixStack matrixStack2) {
        MinecraftClient client = MinecraftClient.getInstance();
        Overlay overlay = Objects.requireNonNull(client.getOverlay());
        CTEvents.POST_RENDER_OVERLAY.invoker().render(matrixStack2, i, j, overlay, client.getLastFrameDuration());
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/Screen;renderWithTooltip(Lnet/minecraft/client/util/math/MatrixStack;IIF)V",
            ordinal = 0,
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectPostRenderScreen(float tickDelta, long startTime, boolean tick, CallbackInfo ci, int i, int j, MatrixStack matrixStack, MatrixStack matrixStack2) {
        MinecraftClient client = MinecraftClient.getInstance();
        Screen screen = Objects.requireNonNull(client.currentScreen);
        CTEvents.POST_RENDER_SCREEN.invoker().render(matrixStack2, i, j, screen, client.getLastFrameDuration());
    }

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void injectPreRenderWorld(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo ci) {
        CTEvents.PRE_RENDER_WORLD.invoker().render(matrices, tickDelta);
    }

    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void injectPostRenderWorld(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo ci) {
        CTEvents.POST_RENDER_WORLD.invoker().render(matrices, tickDelta);
    }
}
