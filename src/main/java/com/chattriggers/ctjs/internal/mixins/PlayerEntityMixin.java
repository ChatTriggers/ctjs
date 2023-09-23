package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.NameTagOverridable;
import com.chattriggers.ctjs.api.message.TextComponent;
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

    @Override
    public void ctjs_setOverriddenNametagName(@Nullable TextComponent component) {
        overriddenNametagName = component;
    }
}
