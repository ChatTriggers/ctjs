package com.chattriggers.ctjs.triggers

import com.chattriggers.ctjs.engine.ILoader

class MixinTrigger(method: Any, loader: ILoader) : Trigger(method, TriggerType.MIXIN, loader) {
    private var name: String? = null

    fun setEventName(name: String) {
        this.name = name
    }

    override fun trigger(args: Array<out Any?>) {
        if (name == null)
            return

        require(args[0] is String) {
            "First argument of MixinTrigger must be a string"
        }

        if (args[0] == name)
            callMethod(args.copyOfRange(1, args.size))
    }
}
