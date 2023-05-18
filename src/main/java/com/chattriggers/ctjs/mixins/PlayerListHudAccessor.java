package com.chattriggers.ctjs.mixins;

import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerListHudAccessor.class)
public interface PlayerListHudAccessor {
    @Accessor
    Text getHeader();

    @Accessor
    Text getFooter();
}
