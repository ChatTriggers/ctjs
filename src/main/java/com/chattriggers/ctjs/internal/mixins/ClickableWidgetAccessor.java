package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractWidget.class)
public interface ClickableWidgetAccessor {
    @Accessor
    void setHeight(int height);
}
