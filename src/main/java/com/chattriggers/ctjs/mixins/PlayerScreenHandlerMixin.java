package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.minecraft.listeners.CancellableEvent;
import com.chattriggers.ctjs.minecraft.wrappers.inventory.Item;
import com.chattriggers.ctjs.triggers.TriggerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerScreenHandler.class)
public class PlayerScreenHandlerMixin {
    @Shadow
    @Final
    private RecipeInputInventory craftingInput;

    @Inject(
        method = "onClosed",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/inventory/CraftingResultInventory;clear()V",
            shift = At.Shift.AFTER
        )
    )
    private void injectOnClosed(PlayerEntity player, CallbackInfo ci) {
        // dropping items for player's crafting slots. needs a whole injection point due to there
        // being an extra if to make sure it only calls dropInventory server-side
        //#if MC>=12000
        if (player.getWorld().isClient) {
        //#else
        //$$ if (player.world.isClient) {
        //#endif
            for (int i = 0; i < craftingInput.size(); i++) {
                ItemStack stack = craftingInput.getStack(i);
                if (!stack.isEmpty()) {
                    TriggerType.DROP_ITEM.triggerAll(Item.fromMC(stack), true, new CancellableEvent());
                }
            }
        }
    }
}
