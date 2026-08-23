package com.chattriggers.ctjs.internal.engine

import com.chattriggers.ctjs.engine.MixinCallback
import java.lang.invoke.MethodHandle
import kotlin.test.Test
import kotlin.test.assertEquals

class MixinCallbackHandleContractTest {
    @Test
    fun `stable mixin trampoline is an instance handle before binding`() {
        val field = JSLoader::class.java.getDeclaredField("INVOKE_MIXIN_CALL").apply {
            isAccessible = true
        }
        val handle = field.get(JSLoader) as MethodHandle

        assertEquals(MixinCallback::class.java, handle.type().parameterType(0))
        assertEquals(Array<Any?>::class.java, handle.type().parameterType(1))
    }
}
