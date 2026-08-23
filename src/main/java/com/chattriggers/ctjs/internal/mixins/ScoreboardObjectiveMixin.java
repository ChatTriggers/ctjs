package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.world.Scoreboard;
import net.minecraft.world.scores.Objective;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Objective.class)
public class ScoreboardObjectiveMixin {
    @Inject(method = "setDisplayName", at = @At("HEAD"), cancellable = true)
    private void chattriggers$keepCustomName(Component name, CallbackInfo ci) {
        if (Scoreboard.INSTANCE.getCustomTitle$ctjs()) {
            ci.cancel();
        }
    }
}
