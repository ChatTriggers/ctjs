package com.chattriggers.ctjs.api.triggers


class RegularTrigger(method: Any, triggerType: ITriggerType) : Trigger(method, triggerType) {
    override fun triggerImpl(args: Array<out Any?>) {
        callMethod(args)
    }
}
