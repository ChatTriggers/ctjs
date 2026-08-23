package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.inventory.Item;
import com.chattriggers.ctjs.api.triggers.CancellableEvent;
import com.chattriggers.ctjs.api.triggers.TriggerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryMenu.class)
public abstract class PlayerScreenHandlerMixin {
    @Shadow
    public abstract CraftingContainer getCraftSlots();

    @Inject(
            method = "removed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/ResultContainer;clearContent()V",
                    shift = At.Shift.AFTER
            )
    )
    private void injectOnClosed(Player player, CallbackInfo ci) {
        // dropping items for player's crafting slots. needs a whole injection point due to there
        // being an extra if to make sure it only calls dropInventory server-side
        if (player.level().isClientSide()) {
            CraftingContainer craftSlots = getCraftSlots();
            for (int i = 0; i < craftSlots.getContainerSize(); i++) {
                ItemStack stack = craftSlots.getItem(i);
                if (!stack.isEmpty()) {
                    TriggerType.DROP_ITEM.triggerAll(Item.fromMC(stack), true, new CancellableEvent());
                }
            }
        }
    }
}
