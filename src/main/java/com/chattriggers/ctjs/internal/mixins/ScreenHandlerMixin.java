package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.inventory.Item;
import com.chattriggers.ctjs.api.triggers.TriggerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class ScreenHandlerMixin {
    @Inject(method = "clearContainer", at = @At("HEAD"), cancellable = true)
    private void injectDropInventory(Player player, Container inventory, CallbackInfo ci) {
        // dropping items for guis that don't keep items in them while the gui is closed
        if (inventory != player.inventoryMenu.getCraftSlots()) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty()) {
                    TriggerType.DROP_ITEM.triggerAll(Item.fromMC(stack), true, ci);
                    if (ci.isCancelled()) {
                        return;
                    }
                }
            }
        }
    }
}
