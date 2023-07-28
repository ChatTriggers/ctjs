package com.chattriggers.ctjs;

import com.chattriggers.ctjs.minecraft.objects.TextComponent;
import com.chattriggers.ctjs.utils.InternalApi;
import org.jetbrains.annotations.Nullable;

@InternalApi
public interface NameTagOverridable {
    void ctjs_setOverriddenNametagName(@Nullable TextComponent component);
}
