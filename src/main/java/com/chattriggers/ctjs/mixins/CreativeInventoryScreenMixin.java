package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.minecraft.wrappers.inventory.Item;
import com.chattriggers.ctjs.triggers.TriggerType;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends AbstractInventoryScreen<CreativeInventoryScreen.CreativeScreenHandler> {
    private CreativeInventoryScreenMixin(CreativeInventoryScreen.CreativeScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
        super(screenHandler, playerInventory, text);
    }

    @Inject(
            method = "onMouseClick",
            at = {
                // dropping by clicking outside creative tab
                @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/ingame/CreativeInventoryScreen;lastClickOutsideBounds:Z", shift = At.Shift.BY, by = 2),
                // dropping by clicking outside creative inventory
                @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z", ordinal = 0, shift = At.Shift.BY, by = 2)
            },
            cancellable = true
    )
    private void injectOnMouseClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        TriggerType.DROP_ITEM.triggerAll(new Item(handler.getCursorStack()), button == 0, ci);
    }

    @Inject(method = "onMouseClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;takeStack(I)Lnet/minecraft/item/ItemStack;"), cancellable = true)
    private void injectOnMouseClick1(@NotNull Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        // dropping item from slot in creative inventory
        TriggerType.DROP_ITEM.triggerAll(new Item(slot.getStack()), button == 0, ci);
    }
}
