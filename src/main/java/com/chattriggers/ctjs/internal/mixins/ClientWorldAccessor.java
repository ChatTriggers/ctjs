package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientWorld.class)
public interface ClientWorldAccessor {
    @Accessor
    ClientChunkManager getChunkManager();
}
