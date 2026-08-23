package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mixin(ClientPacketListener.class)
public interface ClientPlayNetworkHandlerAccessor {
    @Accessor("playerInfoMap")
    Map<UUID, PlayerInfo> getPlayerListEntries();

    @Accessor("listedPlayers")
    Set<PlayerInfo> getListedPlayers();
}
