package com.chattriggers.ctjs.internal;

import net.minecraft.command.CommandSource;

import java.util.HashMap;

public interface CTClientCommandSource extends CommandSource {
    void setContextValue(String key, Object value);

    HashMap<String, Object> getContextValues();
}
