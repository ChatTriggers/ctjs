package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.engine.CTEvents;
import com.chattriggers.ctjs.internal.listeners.MouseListener;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {
    @Inject(
        method = "onButton",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;",
            opcode = Opcodes.GETFIELD
        )
    )
    private void injectOnMouseButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        MouseListener.onRawMouseInput(buttonInfo.button(), action);
    }

    @Inject(
        method = "onScroll",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;options:Lnet/minecraft/client/Options;",
            opcode = Opcodes.GETFIELD
        )
    )
    private void injectOnMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MouseListener.onRawMouseScroll(vertical);
    }

    @Redirect(
        method = "handleAccumulatedMovement",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;mouseDragged(Lnet/minecraft/client/input/MouseButtonEvent;DD)Z"
        )
    )
    private boolean redirectGuiMouseDrag(Screen screen, MouseButtonEvent event, double dx, double dy) {
        CallbackInfo ci = new CallbackInfo("mouseDragged", true);
        CTEvents.GUI_MOUSE_DRAG.invoker().process(dx, dy, event.x(), event.y(), event.button(), screen, ci);
        if (ci.isCancelled()) {
            return false;
        }

        return screen.mouseDragged(event, dx, dy);
    }
}
