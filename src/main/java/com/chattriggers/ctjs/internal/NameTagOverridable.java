package com.chattriggers.ctjs.internal;

import com.chattriggers.ctjs.api.message.TextComponent;
import org.jetbrains.annotations.Nullable;

public interface NameTagOverridable {
    void ctjs_setOverriddenNametagName(@Nullable TextComponent component);
}
