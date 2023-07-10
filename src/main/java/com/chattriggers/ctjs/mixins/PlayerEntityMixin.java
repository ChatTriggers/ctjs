package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.NameTagOverridable;
import com.chattriggers.ctjs.minecraft.objects.TextComponent;
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
    private TextComponent overriddenNametagName;

    @ModifyVariable(method = "getDisplayName", at = @At(value = "STORE", ordinal = 0))
    private MutableText injectGetName(MutableText original) {
        if (overriddenNametagName != null)
            return overriddenNametagName.getComponent();
        return original;
    }

    @Unique
    @Override
    public void setOverriddenNametagName(@Nullable TextComponent component) {
        overriddenNametagName = component;
    }
}
