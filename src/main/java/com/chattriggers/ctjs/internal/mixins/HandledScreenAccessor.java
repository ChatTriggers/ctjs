package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerScreen.class)
public interface HandledScreenAccessor {
    @Invoker("getHoveredSlot")
    Slot invokeGetHoveredSlot(double x, double y);
}
