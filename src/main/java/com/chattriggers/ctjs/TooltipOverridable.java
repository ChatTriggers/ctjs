package com.chattriggers.ctjs;

import com.chattriggers.ctjs.utils.InternalApi;
import net.minecraft.text.Text;

import java.util.List;

@InternalApi
public interface TooltipOverridable {
    void ctjs_setTooltip(List<Text> tooltip);
    void ctjs_setShouldOverrideTooltip(boolean shouldOverrideTooltip);
}
