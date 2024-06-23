package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.message.ChatLib;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @Final
    @Shadow
    private List<ChatHudLine> messages;

    @Inject(method = "clear", at = @At("TAIL"))
    private void injectClear(boolean clearHistory, CallbackInfo ci) {
        ChatLib.INSTANCE.onChatHudClearChat$ctjs();
    }

    // TODO: is it this or addVisibleMessage
    @Inject(
        method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;remove(I)Ljava/lang/Object;",
            shift = At.Shift.BEFORE
        )
    )
    private void injectMessageRemovedForChatLimit(ChatHudLine message, CallbackInfo ci) {
        ChatLib.INSTANCE.onChatHudLineRemoved$ctjs(messages.getLast());
    }

    // Note: ChatHudLine objects are also removed in queueForRemoval, however those are signature based.
    //       The Message objects that CT sends will always create ChatHudLine objects with null signatures,
    //       so objects removed in that method will never be in the ChatLib.chatLineIds map
}
