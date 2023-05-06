package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.minecraft.CTEvents;
import com.chattriggers.ctjs.triggers.TriggerType;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Inject(method = "channelActive", at = @At("TAIL"))
    void injectChannelActive(ChannelHandlerContext context, CallbackInfo ci) {
        CTEvents.CONNECTION_CREATED.invoker().connect(context);
    }

    @Inject(
        method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    void injectSendPacket(Packet<?> packet, @Nullable PacketCallbacks callbacks, CallbackInfo ci) {
        TriggerType.PacketSent.triggerAll(packet, ci);
    }
}
