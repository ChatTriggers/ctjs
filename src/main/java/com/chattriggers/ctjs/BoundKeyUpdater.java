package com.chattriggers.ctjs;

import com.chattriggers.ctjs.utils.InternalApi;
import net.minecraft.client.option.KeyBinding;

@InternalApi
public interface BoundKeyUpdater {
    void ctjs_updateBoundKey(KeyBinding keyBinding);
}
