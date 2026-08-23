package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.Skippable;
import com.chattriggers.ctjs.internal.TooltipOverridable;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Mixin(ItemStack.class)
public class ItemStackMixin implements TooltipOverridable, Skippable {
    @Unique
    private boolean shouldOverrideTooltip = false;
    @Unique
    private List<Component> overriddenTooltip = new ArrayList<>();
    @Unique
    private boolean shouldSkipFabricEvent = false;

    @Inject(method = "getTooltipLines", at = @At("HEAD"), cancellable = true)
    private void injectGetTooltip(Item.TooltipContext context, @Nullable Player player, TooltipFlag type, CallbackInfoReturnable<List<Component>> cir) {
        if (shouldOverrideTooltip)
            cir.setReturnValue(Objects.requireNonNull(overriddenTooltip));
    }

    @Inject(method = "getTooltipLines", at = @At(value = "RETURN", ordinal = 1, shift = At.Shift.BEFORE), cancellable = true)
    private void cancelFabricEvent(Item.TooltipContext context, @Nullable Player player, TooltipFlag type, CallbackInfoReturnable<List<Component>> cir, @Local List<Component> list) {
        if (shouldSkipFabricEvent)
            cir.setReturnValue(list);
    }

    @Override
    public void ctjs_setTooltip(List<Component> tooltip) {
        overriddenTooltip = tooltip;
    }

    @Override
    public void ctjs_setShouldOverrideTooltip(boolean shouldOverrideTooltip) {
        this.shouldOverrideTooltip = shouldOverrideTooltip;
    }

    @Override
    public void ctjs_setShouldSkip(boolean shouldSkip) {
        shouldSkipFabricEvent = shouldSkip;
    }
}
