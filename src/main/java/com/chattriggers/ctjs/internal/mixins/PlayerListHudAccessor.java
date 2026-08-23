package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerTabOverlay.class)
public interface PlayerListHudAccessor {
    @Accessor("header")
    Component getHeader();

    @Accessor("footer")
    Component getFooter();
}
