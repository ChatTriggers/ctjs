package com.chattriggers.ctjs;

import com.chattriggers.ctjs.utils.InternalApi;
import net.minecraft.text.Text;

import java.util.List;

@InternalApi
public interface TooltipOverridable {
    void setTooltip(List<Text> tooltip);
    void setShouldOverrideTooltip(boolean shouldOverrideTooltip);
}
