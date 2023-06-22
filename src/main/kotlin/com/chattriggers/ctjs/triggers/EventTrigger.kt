package com.chattriggers.ctjs.triggers

import com.chattriggers.ctjs.engine.ILoader
import com.chattriggers.ctjs.minecraft.listeners.CancellableEvent
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

class EventTrigger(method: Any, triggerType: ITriggerType, loader: ILoader) : Trigger(method, triggerType, loader) {
    private var triggerIfCanceled = true

    /**
     * Sets if this trigger should run if the event has already been canceled.
     * True by default.
     *
     * @param bool Boolean to set
     * @return the trigger object for method chaining
     */
    fun triggerIfCanceled(bool: Boolean) = apply { triggerIfCanceled = bool }

    override fun trigger(args: Array<out Any?>) {
        val isCanceled = when (val event = args.lastOrNull()) {
            is CancellableEvent -> event.isCanceled()
            is CallbackInfo -> event.isCancelled
            else -> throw IllegalArgumentException(
                "Expected last argument of ${type.name} trigger to be an Event, got ${event?.javaClass?.name ?: "null"}"
            )
        }

        if (triggerIfCanceled || !isCanceled)
            callMethod(args)
    }
}
