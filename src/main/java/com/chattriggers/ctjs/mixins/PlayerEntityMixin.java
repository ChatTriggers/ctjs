package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.NameTagOverridable;
import gg.essential.universal.wrappers.message.UTextComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements NameTagOverridable {
    @Unique
    private UTextComponent overriddenNametagName;

    @ModifyVariable(method = "getDisplayName", at = @At(value = "STORE", ordinal = 0))
    private MutableText injectGetName(MutableText original) {
        if (overriddenNametagName != null)
            return overriddenNametagName.getComponent();
        return original;
    }

    @Unique
    @Override
    public void setOverriddenNametagName(@Nullable UTextComponent component) {
        overriddenNametagName = component;
    }
}
