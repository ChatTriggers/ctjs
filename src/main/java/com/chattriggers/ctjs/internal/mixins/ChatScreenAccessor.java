package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatScreen.class)
public interface ChatScreenAccessor {
    @Accessor
    TextFieldWidget getChatField();
}
