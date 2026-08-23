package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.client.Player;
import com.chattriggers.ctjs.api.inventory.Item;
import com.chattriggers.ctjs.api.triggers.TriggerType;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class ClientPlayerEntityMixin {
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void injectDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        // dropping item while not in gui
        Item stack = Player.getHeldItem();
        if (stack != null && !stack.getMcValue().isEmpty()) {
            TriggerType.DROP_ITEM.triggerAll(stack, entireStack, cir);
        }
    }
}
