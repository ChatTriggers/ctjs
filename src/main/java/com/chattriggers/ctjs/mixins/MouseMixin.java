package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.minecraft.CTEvents;
import com.chattriggers.ctjs.minecraft.listeners.MouseListener;
import net.minecraft.client.Mouse;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(
        method = "onMouseButton",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;",
            opcode = Opcodes.GETFIELD
        )
    )
    void injectOnMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        MouseListener.onRawMouseInput(button, action, mods);
    }

    @Inject(
        method = "onMouseScroll",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/MinecraftClient;options:Lnet/minecraft/client/option/GameOptions;",
            opcode = Opcodes.GETFIELD
        )
    )
    void injectOnMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MouseListener.onRawMouseScroll(horizontal, vertical);
    }
}
