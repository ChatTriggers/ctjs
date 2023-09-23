package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.triggers.CancellableEvent;
import com.chattriggers.ctjs.api.inventory.Item;
import com.chattriggers.ctjs.api.triggers.TriggerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {
    @Inject(method = "dropInventory", at = @At("HEAD"))
    private void injectDropInventory(PlayerEntity player, Inventory inventory, CallbackInfo ci) {
        // dropping items for guis that don't keep items in them while the gui is closed
        if (inventory != player.playerScreenHandler.getCraftingInput()) {
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.getStack(i);
                if (!stack.isEmpty()) {
                    TriggerType.DROP_ITEM.triggerAll(Item.fromMC(stack), true, new CancellableEvent());
                }
            }
        }
    }
}
