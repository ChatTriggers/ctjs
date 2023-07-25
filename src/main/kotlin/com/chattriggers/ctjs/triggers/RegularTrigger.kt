package com.chattriggers.ctjs.triggers

import com.chattriggers.ctjs.utils.InternalApi

@InternalApi
class RegularTrigger(method: Any, triggerType: ITriggerType) : Trigger(method, triggerType) {
    override fun trigger(args: Array<out Any?>) {
        callMethod(args)
    }
}
