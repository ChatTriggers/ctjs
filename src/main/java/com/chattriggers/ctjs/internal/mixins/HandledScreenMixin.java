package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.inventory.Item;
import com.chattriggers.ctjs.api.message.TextComponent;
import com.chattriggers.ctjs.api.triggers.TriggerType;
import com.chattriggers.ctjs.internal.triggers.TriggerContractAdapters;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(AbstractContainerScreen.class)
public class HandledScreenMixin extends Screen {
    @Shadow
    protected Slot hoveredSlot;

    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    private HandledScreenMixin(Component title) {
        super(title);
    }

    @Inject(
        method = "extractTooltip",
        at = @At("HEAD"),
        cancellable = true
    )
    private void injectDrawMouseoverTooltip(GuiGraphicsExtractor context, int x, int y, CallbackInfo ci) {
        if (hoveredSlot == null || !hoveredSlot.hasItem()) return;
        ItemStack stack = hoveredSlot.getItem();
        TriggerType.ITEM_TOOLTIP.triggerAll(
            TriggerContractAdapters.mutableList(
                getTooltipFromItem(Objects.requireNonNull(minecraft), stack)
                    .stream()
                    .map(TextComponent::new)
                    .toList()
            ),
            Item.fromMC(stack),
            ci
        );
    }

    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V", at = @At("HEAD"), cancellable = true)
    private void injectOnMouseClick(Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo ci) {
        if (
            (slotId != -999 && actionType == ContainerInput.THROW) || // dropping item from slot
                (slotId == -999 && actionType == ContainerInput.PICKUP) // dropping by clicking outside inventory
        ) {
            TriggerType.DROP_ITEM.triggerAll(Item.fromMC(menu.getCarried()), button == 0, ci);
        }
    }
}
