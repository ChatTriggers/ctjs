package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.minecraft.listeners.CancellableEvent;
import com.chattriggers.ctjs.minecraft.wrappers.inventory.Item;
import com.chattriggers.ctjs.triggers.TriggerType;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends AbstractInventoryScreen<CreativeInventoryScreen.CreativeScreenHandler> {
    private CreativeInventoryScreenMixin(CreativeInventoryScreen.CreativeScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
        super(screenHandler, playerInventory, text);
    }

    @ModifyExpressionValue(
        method = "onMouseClick",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/ingame/CreativeInventoryScreen;lastClickOutsideBounds:Z")
    )
    private boolean injectOnMouseClick(boolean original, @Local(ordinal = 1) int button) {
        // dropping by clicking outside creative tab
        CancellableEvent event = new CancellableEvent();
        if (original) {
            TriggerType.DROP_ITEM.triggerAll(new Item(handler.getCursorStack()), button == 0, event);
        }

        return original && !event.isCanceled();
    }

    @ModifyExpressionValue(
        method = "onMouseClick",
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lnet/minecraft/client/gui/screen/ingame/CreativeInventoryScreen;deleteItemSlot:Lnet/minecraft/screen/slot/Slot;",
                ordinal = 0
            )
        ),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z", ordinal = 0)
    )
    private boolean injectOnMouseClick1(boolean original, @Local(ordinal = 1) int button) {
        // dropping by clicking outside creative inventory
        CancellableEvent event = new CancellableEvent();
        if (!original) {
            TriggerType.DROP_ITEM.triggerAll(new Item(handler.getCursorStack()), button == 0, event);
        }

        // !(original || eventCanceled) => !original && !canceled
        return original || event.isCanceled();
    }

    @Inject(method = "onMouseClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;takeStack(I)Lnet/minecraft/item/ItemStack;"), cancellable = true)
    private void injectOnMouseClick2(@NotNull Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        // dropping item from slot in creative inventory
        TriggerType.DROP_ITEM.triggerAll(new Item(slot.getStack()), button == 0, ci);
    }
}
