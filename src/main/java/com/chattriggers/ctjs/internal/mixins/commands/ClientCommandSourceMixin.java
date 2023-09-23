package com.chattriggers.ctjs.internal.mixins.commands;

import com.chattriggers.ctjs.internal.CTClientCommandSource;
import net.minecraft.client.network.ClientCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(ClientCommandSource.class)
public abstract class ClientCommandSourceMixin implements CTClientCommandSource {
    @Unique
    private final HashMap<String, Object> contextValues = new HashMap<>();

    @Override
    public void setContextValue(String key, Object value) {
        contextValues.put(key, value);
    }

    @Override
    public HashMap<String, Object> getContextValues() {
        return contextValues;
    }
}
