package com.chattriggers.ctjs.mixins;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    public boolean overrideTooltip = false;
    public List<Text> overriddenTooltip = new ArrayList<>();

    @Inject(method = "getTooltip", at = @At("HEAD"), cancellable = true)
    void injectGetTooltip(@Nullable PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
        if (overrideTooltip)
            cir.setReturnValue(Objects.requireNonNull(overriddenTooltip));
    }
}
