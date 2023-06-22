package com.chattriggers.ctjs.launch

import java.lang.invoke.MethodHandle
import java.lang.invoke.SwitchPoint

data class MixinCallback(internal val id: Int, internal val injector: IInjector) {
    internal var method: Any? = null
    internal var handle: MethodHandle? = null
    internal var invalidator = SwitchPoint()

    fun attach(method: Any) {
        this.method = method
        invalidate()
    }

    fun release() {
        this.method = null
        invalidate()
    }

    private fun invalidate() {
        // The target method of this mixin has changed, so we need to re-initialize the invokedynamic instruction tied
        // to this callback
        SwitchPoint.invalidateAll(arrayOf(invalidator))
        invalidator = SwitchPoint()
    }
}
