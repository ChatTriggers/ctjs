package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.client.gui.screen.ingame.BookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BookScreen.class)
public interface BookScreenAccessor {
    @Accessor
    int getPageIndex();

    @Invoker
    void invokeUpdatePageButtons();
}
