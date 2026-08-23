package com.chattriggers.ctjs.internal;

import net.minecraft.network.chat.Component;

import java.util.List;

public interface TooltipOverridable {
    void ctjs_setTooltip(List<Component> tooltip);
    void ctjs_setShouldOverrideTooltip(boolean shouldOverrideTooltip);
}
