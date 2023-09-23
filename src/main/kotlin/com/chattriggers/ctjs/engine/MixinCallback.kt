package com.chattriggers.ctjs.engine

import com.chattriggers.ctjs.internal.launch.IInjector
import java.lang.invoke.MethodHandle
import java.lang.invoke.SwitchPoint

data class MixinCallback(internal val id: Int, internal val injector: IInjector) {
    internal var method: Any? = null
    internal var handle: MethodHandle? = null
    internal var invalidator = SwitchPoint()

    fun attach(method: Any) {
        this.method = method

        // The target method of this mixin has changed, so we need to re-initialize the invokedynamic instruction tied
        // to this callback
        SwitchPoint.invalidateAll(arrayOf(invalidator))
        invalidator = SwitchPoint()
    }
}
