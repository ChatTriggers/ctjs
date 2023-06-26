package com.chattriggers.ctjs;

import net.minecraft.text.Text;

import java.util.List;

public interface TooltipOverridable {
    void setTooltip(List<Text> tooltip);
    void setShouldOverrideTooltip(boolean shouldOverrideTooltip);
}
