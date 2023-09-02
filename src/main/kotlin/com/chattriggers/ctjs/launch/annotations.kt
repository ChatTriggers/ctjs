
package com.chattriggers.ctjs.launch

import com.chattriggers.ctjs.utils.descriptor
import org.spongepowered.asm.mixin.injection.Constant as SPConstant

/*
 * Annotation classes present some difficulties, especially when trying to
 * create them from an embedded engine context. They've been more-or-less
 * recreated here as normal classes for the loaders to use
 */

data class Mixin(
    val target: String,
    val priority: Int?,
    val remap: Boolean?,
)

data class At(
    val value: String,
    val id: String?,
    val slice: String?,
    val shift: Shift?,
    val by: Int?,
    val args: List<String>?,
    val target: String?,
    val ordinal: Int?,
    val opcode: Int?,
    val remap: Boolean?,
) {
    enum class Shift {
        NONE,
        BEFORE,
        AFTER,
        BY,
    }
}

data class Slice(
    val id: String?,
    val from: At?,
    val to: At?,
)

// There are two ways to capture a local:
//   - By absolute local index: Local(type = "F", index = 4)
//   - By ordinal and type: Local(type = "F", ordinal = 1)
//
// If print is required, then type is not required since the local
// is not actually captured
data class Local(
    val print: Boolean?,
    val index: Int?,
    val ordinal: Int?,
    val type: String?,
    val mutable: Boolean?,
)

data class Constant(
    val nullValue: Boolean?,
    val intValue: Int?,
    val floatValue: Float?,
    val longValue: Long?,
    val doubleValue: Double?,
    val stringValue: String?,
    val classValue: String?,
    val ordinal: Int?,
    val slice: String?,
    val expandZeroConditions: List<SPConstant.Condition>?,
    val log: Boolean?,
) {
    fun getTypeDescriptor() = when {
        nullValue != null -> Any::class.descriptor() // Is this right?
        intValue != null -> Descriptor.Primitive.INT
        floatValue != null -> Descriptor.Primitive.FLOAT
        longValue != null -> Descriptor.Primitive.LONG
        doubleValue != null -> Descriptor.Primitive.DOUBLE
        stringValue != null -> String::class.descriptor()
        classValue != null -> Class::class.descriptor()
        else -> error("Constant() expects one non-null type field")
    }
}

sealed interface IInjector

data class Inject(
    val method: String,
    val id: String?,
    val slice: List<Slice>?,
    val at: List<At>?,
    val cancellable: Boolean?,
    val locals: List<Local>?,
    val remap: Boolean?,
    val require: Int?,
    val expect: Int?,
    val allow: Int?,
    val constraints: String?,
) : IInjector

data class Redirect(
    val method: String,
    val slice: Slice?,
    val at: At,
    val locals: List<Local>?,
    val remap: Boolean?,
    val require: Int?,
    val expect: Int?,
    val allow: Int?,
    val constraints: String?,
) : IInjector

data class ModifyArg(
    val method: String,
    val slice: Slice?,
    val at: At,
    val index: Int,
    val captureAllParams: Boolean?,
    val locals: List<Local>?,
    val remap: Boolean?,
    val require: Int?,
    val expect: Int?,
    val allow: Int?,
    val constraints: String?,
) : IInjector

data class ModifyArgs(
    val method: String,
    val slice: Slice?,
    val at: At,
    val locals: List<Local>?,
    val remap: Boolean?,
    val require: Int?,
    val expect: Int?,
    val allow: Int?,
    val constraints: String?,
) : IInjector

data class ModifyConstant(
    val method: String,
    val slice: List<Slice>?,
    val constant: Constant,
    val locals: List<Local>?,
    val remap: Boolean?,
    val require: Int?,
    val expect: Int?,
    val allow: Int?,
    val constraints: String?,
) : IInjector

data class ModifyExpressionValue(
    val method: String,
    val at: At,
    val slice: List<Slice>?,
    val locals: List<Local>?,
    val remap: Boolean?,
    val require: Int?,
    val expect: Int?,
    val allow: Int?,
) : IInjector

data class ModifyReceiver(
    val method: String,
    val at: At,
    val slice: List<Slice>?,
    val locals: List<Local>?,
    val remap: Boolean?,
    val require: Int?,
    val expect: Int?,
    val allow: Int?,
) : IInjector

data class ModifyReturnValue(
    val method: String,
    val at: At,
    val slice: List<Slice>?,
    val locals: List<Local>?,
    val remap: Boolean?,
    val require: Int?,
    val expect: Int?,
    val allow: Int?,
) : IInjector

data class ModifyVariable(
    val method: String,
    val at: At,
    val slice: Slice?,
    val print: Boolean?,
    val ordinal: Int?,
    val index: Int?,
    val type: String?,
    val locals: List<Local>?,
    val remap: Boolean?,
    val require: Int?,
    val expect: Int?,
    val allow: Int?,
    val constraints: String?,
) : IInjector

data class WrapOperation(
    val method: String,
    val at: At?,
    val constant: Constant?,
    val slice: List<Slice>?,
    val locals: List<Local>?,
    val remap: Boolean?,
    val require: Int?,
    val expect: Int?,
    val allow: Int?,
) : IInjector

data class WrapWithCondition(
    val method: String,
    val at: At,
    val slice: List<Slice>?,
    val locals: List<Local>?,
    val remap: Boolean?,
    val require: Int?,
    val expect: Int?,
    val allow: Int?,
) : IInjector
