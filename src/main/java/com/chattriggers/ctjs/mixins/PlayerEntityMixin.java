package com.chattriggers.ctjs.mixins;

import gg.essential.universal.wrappers.message.UTextComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    private String overriddenNametagName;

    public void setOverriddenNametagName(@Nullable String component) {
        overriddenNametagName = component;
    }

    @Inject(method = "getEntityName", at = @At("HEAD"), cancellable = true)
    void injectGetName(CallbackInfoReturnable<String> cir) {
        if (overriddenNametagName != null)
            cir.setReturnValue(overriddenNametagName);
    }

    @Inject(
        method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;",
        at = @At("HEAD")
    )
    void injectDropItem(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        // TODO
    }
}
