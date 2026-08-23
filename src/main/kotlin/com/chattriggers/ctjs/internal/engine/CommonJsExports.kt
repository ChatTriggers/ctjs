package com.chattriggers.ctjs.internal.engine

import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

internal object CommonJsExports {
    fun ensureDefault(exports: Scriptable): Scriptable = exports.also {
        if (!ScriptableObject.hasProperty(it, "default"))
            ScriptableObject.defineProperty(it, "default", it, ScriptableObject.NOT_ENUMERABLE)
    }
}
