package com.chattriggers.ctjs;

import com.chattriggers.ctjs.utils.InternalApi;

@InternalApi
public interface Skippable {
    void ctjs_setShouldSkip(boolean shouldSkip);
}
