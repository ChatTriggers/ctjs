package com.chattriggers.ctjs;

import com.chattriggers.ctjs.minecraft.objects.TextComponent;
import org.jetbrains.annotations.Nullable;

public interface NameTagOverridable {
    void setOverriddenNametagName(@Nullable TextComponent component);
}
