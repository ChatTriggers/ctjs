package com.chattriggers.ctjs.api.triggers

import com.chattriggers.ctjs.api.entity.BlockEntity
import com.chattriggers.ctjs.api.entity.Entity
import com.chattriggers.ctjs.MCBlockEntity
import com.chattriggers.ctjs.MCEntity
import net.minecraft.network.packet.Packet

sealed class ClassFilterTrigger<Wrapped, Unwrapped>(
    method: Any,
    private val triggerType: ITriggerType,
    private val wrappedClass: Class<Wrapped>,
) : Trigger(method, triggerType) {
    private var triggerClasses: List<Class<Unwrapped>> = emptyList()

    /**
     * Alias for `setFilteredClasses([A.class])`
     *
     * @param clazz The class for which this trigger should run for
     */
    fun setFilteredClass(clazz: Class<Unwrapped>) = setFilteredClasses(listOf(clazz))

    /**
     * Sets which classes this trigger should run for. If the list is empty, it runs
     * for every class.
     *
     * @param classes The classes for which this trigger should run for
     * @return This trigger object for chaining
     */
    fun setFilteredClasses(classes: List<Class<Unwrapped>>) = apply { triggerClasses = classes }

    override fun trigger(args: Array<out Any?>) {
        val placeholder = evalTriggerType(args)
        if (triggerClasses.isEmpty() || triggerClasses.any { it.isInstance(placeholder) })
            callMethod(args)
    }

    private fun evalTriggerType(args: Array<out Any?>): Unwrapped {
        val arg = args.getOrNull(0) ?: error("First argument of $triggerType trigger can not be null")

        check(wrappedClass.isInstance(arg)) {
            "Expected first argument of $triggerType trigger to be instance of $wrappedClass"
        }

        @Suppress("UNCHECKED_CAST")
        return unwrap(arg as Wrapped)
    }

    protected abstract fun unwrap(wrapped: Wrapped): Unwrapped
}

class RenderEntityTrigger(method: Any) : ClassFilterTrigger<Entity, MCEntity>(
    method,
    TriggerType.RENDER_ENTITY,
    Entity::class.java,
) {
    override fun unwrap(wrapped: Entity): MCEntity = wrapped.toMC()
}

class RenderBlockEntityTrigger(method: Any) : ClassFilterTrigger<BlockEntity, MCBlockEntity>(
    method,
    TriggerType.RENDER_BLOCK_ENTITY,
    BlockEntity::class.java
) {
    override fun unwrap(wrapped: BlockEntity): MCBlockEntity = wrapped.toMC()
}

class PacketTrigger(method: Any, triggerType: ITriggerType) : ClassFilterTrigger<Packet<*>, Packet<*>>(
    method,
    triggerType,
    Packet::class.java,
) {
    override fun unwrap(wrapped: Packet<*>): Packet<*> = wrapped
}
