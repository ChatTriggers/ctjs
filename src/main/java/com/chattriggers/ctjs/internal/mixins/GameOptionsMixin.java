package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.BoundKeyUpdater;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptions.class)
public class GameOptionsMixin implements BoundKeyUpdater {
    @Unique
    private GameOptions.Visitor visitor;

    @Inject(method = "accept", at = @At("HEAD"))
    private void captureVisitor(GameOptions.Visitor visitor, CallbackInfo ci) {
        this.visitor = visitor;
    }

    @Override
    public void ctjs_updateBoundKey(KeyBinding keyBinding) {
        String string = keyBinding.getBoundKeyTranslationKey();
        String string2 = visitor.visitString("key_" + keyBinding.getTranslationKey(), string);
        if (!string.equals(string2)) {
            keyBinding.setBoundKey(InputUtil.fromTranslationKey(string2));
        }
    }
}
