package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BookViewScreen.class)
public interface BookScreenAccessor {
    @Accessor("currentPage")
    int getPageIndex();

    @Invoker("updateButtonVisibility")
    void invokeUpdatePageButtons();
}
