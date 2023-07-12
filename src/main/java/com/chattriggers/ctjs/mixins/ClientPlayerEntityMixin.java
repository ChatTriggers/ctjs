package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.minecraft.wrappers.Player;
import com.chattriggers.ctjs.minecraft.wrappers.inventory.Item;
import com.chattriggers.ctjs.triggers.TriggerType;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void injectDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        // dropping item while not in gui
        Item stack = Player.INSTANCE.getHeldItem();
        if (stack != null && !stack.getMcValue().isEmpty()) {
            TriggerType.DROP_ITEM.triggerAll(stack, entireStack, cir);
        }
    }
}
