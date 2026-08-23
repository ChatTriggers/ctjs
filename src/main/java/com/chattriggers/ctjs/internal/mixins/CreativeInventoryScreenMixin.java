package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.triggers.CancellableEvent;
import com.chattriggers.ctjs.api.inventory.Item;
import com.chattriggers.ctjs.api.triggers.TriggerType;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    private CreativeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu screenHandler, Inventory playerInventory, Component text) {
        super(screenHandler, playerInventory, text);
    }

    @ModifyExpressionValue(
        method = "slotClicked",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen;hasClickedOutside:Z")
    )
    private boolean injectOnMouseClick(boolean original, @Local(ordinal = 1) int button) {
        // dropping by clicking outside creative tab
        CancellableEvent event = new CancellableEvent();
        if (original) {
            TriggerType.DROP_ITEM.triggerAll(Item.fromMC(menu.getCarried()), button == 0, event);
        }

        return original && !event.isCanceled();
    }

    @ModifyExpressionValue(
        method = "slotClicked",
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen;destroyItemSlot:Lnet/minecraft/world/inventory/Slot;",
                ordinal = 0
            )
        ),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 0)
    )
    private boolean injectOnMouseClick1(boolean original, @Local(ordinal = 1) int button) {
        // dropping by clicking outside creative inventory
        CancellableEvent event = new CancellableEvent();
        if (!original) {
            TriggerType.DROP_ITEM.triggerAll(Item.fromMC(menu.getCarried()), button == 0, event);
        }

        // !(original || eventCanceled) => !original && !canceled
        return original || event.isCanceled();
    }

    @Inject(method = "slotClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;remove(I)Lnet/minecraft/world/item/ItemStack;"), cancellable = true)
    private void injectOnMouseClick2(@NotNull Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo ci) {
        // dropping item from slot in creative inventory
        TriggerType.DROP_ITEM.triggerAll(Item.fromMC(slot.getItem()), button == 0, ci);
    }
}
