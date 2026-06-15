package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.CustomKeyMapping;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.lang.reflect.Array;

@Mixin(KeyBindsList.class)
public class KeyBindsListMixin {
    @ModifyExpressionValue(method = "<init>", at = @At(
        value = "INVOKE",
        target = "Lorg/apache/commons/lang3/ArrayUtils;clone([Ljava/lang/Object;)[Ljava/lang/Object;")
    )
    public Object[] addCustomKeyMappings(Object[] original) {
        var customKeyMappings = CustomKeyMapping.getKeyMappings();
        if (customKeyMappings.isEmpty()) return original;

        Class<?> type = original.getClass().getComponentType();
        Object[] expanded = (Object[]) Array.newInstance(type, original.length + customKeyMappings.size());

        // keep the original key mappings
        System.arraycopy(original, 0, expanded, 0, original.length);

        // add the custom ones
        for (int i = 0; i < customKeyMappings.size(); i++) {
            expanded[i + original.length] = customKeyMappings.get(i);
        }

        return expanded;
    }
}
