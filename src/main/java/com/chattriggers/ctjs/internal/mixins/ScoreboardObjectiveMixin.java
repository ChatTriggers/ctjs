package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.world.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScoreboardObjective.class)
public class ScoreboardObjectiveMixin {
    @Inject(method = "setDisplayName", at = @At("HEAD"), cancellable = true)
    private void chattriggers$keepCustomName(Text name, CallbackInfo ci) {
        if (Scoreboard.INSTANCE.getCustomTitle$ctjs()) {
            ci.cancel();
        }
    }
}
