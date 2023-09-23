package com.chattriggers.ctjs.internal.mixins.commands;

import com.chattriggers.ctjs.internal.engine.CTEvents;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Shadow
    private CommandDispatcher<CommandSource> commandDispatcher;

    @Inject(method = "onCommandTree", at = @At("TAIL"))
    private void injectOnCommandTree(CommandTreeS2CPacket packet, CallbackInfo ci) {
        //noinspection unchecked
        CTEvents.NETWORK_COMMAND_DISPATCHER_REGISTER.invoker().register(
            (CommandDispatcher<FabricClientCommandSource>) (Object) commandDispatcher
        );
    }
}
