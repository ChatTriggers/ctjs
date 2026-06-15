package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.CustomKeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class OptionsMixin {
    @Inject(method = "save", at = @At("RETURN"))
    public void save(CallbackInfo ci) {
        CustomKeyMapping.INSTANCE.save();
    }

    @Inject(method = "load", at = @At("RETURN"))
    public void load(CallbackInfo ci) {
        CustomKeyMapping.INSTANCE.load();
    }
}
