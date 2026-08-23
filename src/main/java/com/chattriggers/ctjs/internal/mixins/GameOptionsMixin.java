package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.BoundKeyUpdater;
import net.minecraft.client.Options;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class GameOptionsMixin implements BoundKeyUpdater {
    @Unique
    private Options.FieldAccess fieldAccess;

    @Inject(method = "processOptions", at = @At("HEAD"))
    private void captureFieldAccess(Options.FieldAccess fieldAccess, CallbackInfo ci) {
        this.fieldAccess = fieldAccess;
    }

    @Override
    public void ctjs_updateBoundKey(KeyMapping keyBinding) {
        String string = keyBinding.saveString();
        String string2 = fieldAccess.process("key_" + keyBinding.getName(), string);
        if (!string.equals(string2)) {
            keyBinding.setKey(InputConstants.getKey(string2));
        }
    }
}
