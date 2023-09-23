package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.engine.CTEvents;
import com.chattriggers.ctjs.internal.listeners.MouseListener;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Shadow
    private int activeButton;

    @Inject(
        method = "onMouseButton",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;",
            opcode = Opcodes.GETFIELD
        )
    )
    private void injectOnMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        MouseListener.onRawMouseInput(button, action);
    }

    @Inject(
        method = "onMouseScroll",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/MinecraftClient;options:Lnet/minecraft/client/option/GameOptions;",
            opcode = Opcodes.GETFIELD
        )
    )
    private void injectOnMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MouseListener.onRawMouseScroll(vertical);
    }

    @Inject(
        method = "method_1602(Lnet/minecraft/client/gui/screen/Screen;DDDD)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/Screen;mouseDragged(DDIDD)Z"
        ),
        cancellable = true
    )
    private void injectOnGuiMouseDrag(Screen screen, double d, double e, double f, double g, CallbackInfo ci) {
        if (screen != null) {
            CTEvents.GUI_MOUSE_DRAG.invoker().process(f, g, d, e, activeButton, screen, ci);
        }
    }
}
