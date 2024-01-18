package com.chattriggers.ctjs.api.triggers

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.internal.engine.JSLoader

abstract class Trigger protected constructor(
    var method: Any,
    var type: ITriggerType,
) : Comparable<Trigger> {
    private var priority: Priority = Priority.NORMAL

    var isRegistered = false
        private set

    init {
        // See note for register method
        @Suppress("LeakingThis")
        register()
    }

    /**
     * Sets a trigger's priority using [Priority].
     * Highest runs first.
     * @param priority the priority of the trigger
     * @return the trigger for method chaining
     */
    fun setPriority(priority: Priority) = apply {
        this.priority = priority

        // Re-register so the position in the ConcurrentSkipListSet gets updated
        unregister()
        register()
    }

    /**
     * Registers a trigger based on its type.
     * This is done automatically with TriggerRegister.
     * @return the trigger for method chaining
     */
    // NOTE: Class initialization cannot be done in this method. It is called in
    //       the init block above, and thus the child class version will not be
    //       run initially
    open fun register() = apply {
        if (!isRegistered) {
            isRegistered = true
            JSLoader.addTrigger(this)
        }
    }

    /**
     * Unregisters a trigger.
     * @return the trigger for method chaining
     */
    open fun unregister() = apply {
        if (isRegistered) {
            isRegistered = false
            JSLoader.removeTrigger(this)
        }
    }

    protected fun callMethod(args: Array<out Any?>) {
        if (CTJS.isLoaded)
            JSLoader.trigger(this, method, args)
    }

    internal abstract fun trigger(args: Array<out Any?>)

    override fun compareTo(other: Trigger): Int {
        val ordCmp = priority.ordinal - other.priority.ordinal
        return if (ordCmp == 0)
            hashCode() - other.hashCode()
        else ordCmp
    }

    enum class Priority {
        //LOWEST IS RAN LAST
        HIGHEST,
        HIGH,
        NORMAL,
        LOW,
        LOWEST
    }
}
