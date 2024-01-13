package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.inventory.Item;
import com.chattriggers.ctjs.api.message.TextComponent;
import com.chattriggers.ctjs.api.triggers.TriggerType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
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

import java.util.Objects;

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
            target = "Lnet/minecraft/client/gui/DrawContext;drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V"
        ),
        cancellable = true
    )
    private void injectDrawMouseoverTooltip(DrawContext context, int x, int y, CallbackInfo ci) {
        ItemStack stack = focusedSlot.getStack();
        TriggerType.ITEM_TOOLTIP.triggerAll(
            getTooltipFromItem(Objects.requireNonNull(client), stack)
                .stream()
                .map(TextComponent::new)
                .toList(),
            Item.fromMC(stack),
            ci
        );
    }

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
    private void injectOnMouseClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (
            (slotId != -999 && actionType == SlotActionType.THROW) || // dropping item from slot
                (slotId == -999 && actionType == SlotActionType.PICKUP) // dropping by clicking outside inventory
        ) {
            TriggerType.DROP_ITEM.triggerAll(Item.fromMC(handler.getCursorStack()), button == 0, ci);
        }
    }
}
