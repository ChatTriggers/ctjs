package com.chattriggers.ctjs.engine

import com.chattriggers.ctjs.internal.launch.IInjector
import com.chattriggers.ctjs.internal.engine.JSLoader
import com.chattriggers.ctjs.internal.lifecycle.GenerationCallbackSlot
import java.lang.invoke.MethodHandle
import java.lang.invoke.SwitchPoint
import org.mozilla.javascript.Callable

data class MixinCallback(internal val id: Int, internal val injector: IInjector) {
    private val callback = GenerationCallbackSlot<Callable>()
    internal var method: Any? = null
    internal var handle: MethodHandle? = null
    internal var invalidator = SwitchPoint()

    fun attach(method: Any) {
        require(method is Callable) {
            "The value passed to MixinCallback.attach() must be a function"
        }

        this.method = method
        callback.publish(method, JSLoader.currentRuntimeOwner())
    }

    internal fun isActive() = callback.isActive()

    @JvmName("invokeStable")
    internal fun invoke(args: Array<Any?>): Any? = callback.invoke {
        JSLoader.invokeMixin(it, args)
    }.value
}
