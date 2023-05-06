
package com.chattriggers.ctjs.mixins;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {
    @Inject(
        method = "onPlayerCollision",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;getCount()I",
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILHARD,
        cancellable = true
    )
    void injectPickup(PlayerEntity player, CallbackInfo ci, ItemStack itemStack, Item item) {
        // TODO: PickupItem trigger
    }
}
