package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.Skippable;
import com.chattriggers.ctjs.internal.TooltipOverridable;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
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
    private List<Text> overriddenTooltip = new ArrayList<>();
    @Unique
    private boolean shouldSkipFabricEvent = false;

    @Inject(method = "getTooltip", at = @At("HEAD"), cancellable = true)
    private void injectGetTooltip(Item.TooltipContext context, @Nullable PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir) {
        if (shouldOverrideTooltip)
            cir.setReturnValue(Objects.requireNonNull(overriddenTooltip));
    }

    @Inject(method = "getTooltip", at = @At(value = "RETURN", ordinal = 1, shift = At.Shift.BEFORE), cancellable = true)
    private void cancelFabricEvent(Item.TooltipContext context, @Nullable PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir, @Local List<Text> list) {
        if (shouldSkipFabricEvent)
            cir.setReturnValue(list);
    }

    @Override
    public void ctjs_setTooltip(List<Text> tooltip) {
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
