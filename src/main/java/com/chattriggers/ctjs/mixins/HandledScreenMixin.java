package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.minecraft.wrappers.inventory.Item;
import com.chattriggers.ctjs.triggers.TriggerType;
import gg.essential.universal.wrappers.message.UTextComponent;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class HandledScreenMixin extends Screen {
    @Shadow
    protected Slot focusedSlot;

    @Shadow
    @Final
    protected ScreenHandler handler;

    private HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(
            method = "drawMouseoverTooltip",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;renderTooltip(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/item/ItemStack;II)V"
            ),
            cancellable = true
    )
    private void injectDrawMouseoverTooltip(MatrixStack matrices, int x, int y, CallbackInfo ci) {
        ItemStack stack = focusedSlot.getStack();
        TriggerType.ITEM_TOOLTIP.triggerAll(getTooltipFromItem(stack).stream().map(text -> new UTextComponent(text).getFormattedText()).toList(), new Item(stack), ci);
    }

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
    private void injectOnMouseClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (
                (slotId != -999 && actionType == SlotActionType.THROW) || // dropping item from slot
                        (slotId == -999 && actionType == SlotActionType.PICKUP) // dropping by clicking outside inventory
        ) {
            TriggerType.DROP_ITEM.triggerAll(new Item(handler.getCursorStack()), button == 0, ci);
        }
    }
}
