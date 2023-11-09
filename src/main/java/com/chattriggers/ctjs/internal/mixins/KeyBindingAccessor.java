package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.Set;

@Mixin(KeyBinding.class)
public interface KeyBindingAccessor {
    @Accessor("CATEGORY_ORDER_MAP")
    static Map<String, Integer> getCategoryMap() { throw new IllegalStateException(); }

    @Accessor("KEY_CATEGORIES")
    static Set<String> getKeyCategories() {
        throw new IllegalStateException();
    }

    @Accessor
    InputUtil.Key getBoundKey();

    @Accessor
    int getTimesPressed();
}
