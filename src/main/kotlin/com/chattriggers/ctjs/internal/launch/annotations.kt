package com.chattriggers.ctjs.internal.launch

import com.chattriggers.ctjs.internal.utils.descriptor
import org.objectweb.asm.Opcodes
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
    internal val atTarget: AtTarget<*> by lazy(::getAtTarget)

    internal sealed class AtTarget<T : Descriptor>(val descriptor: T, val targetName: String)

    internal class InvokeTarget(descriptor: Descriptor.Method) : AtTarget<Descriptor.Method>(descriptor, "INVOKE") {
        override fun toString() = descriptor.originalDescriptor()
    }

    internal class NewTarget(descriptor: Descriptor.New) : AtTarget<Descriptor.New>(descriptor, "NEW") {
        override fun toString() = descriptor.originalDescriptor()
    }

    internal class FieldTarget(
        descriptor: Descriptor.Field, val isGet: Boolean?, val isStatic: Boolean?,
    ) : AtTarget<Descriptor.Field>(descriptor, "FIELD") {
        override fun toString() = descriptor.originalDescriptor()
    }

    internal class ConstantTarget(val key: String, descriptor: Descriptor) : AtTarget<Descriptor>(descriptor, "CONSTANT") {
        init {
            require(descriptor.isType)
        }

        override fun toString() = "$key=$descriptor"
    }

    private fun getAtTarget(): AtTarget<*> {
        return when (value) {
            "INVOKE" -> {
                requireNotNull(target) { "At targeting INVOKE expects its target to be a method descriptor" }
                InvokeTarget(Descriptor.Parser(target).parseMethod(full = true))
            }
            "NEW" -> {
                requireNotNull(target) { "At targeting NEW expects its target to be a new invocation descriptor" }
                NewTarget(Descriptor.Parser(target).parseNew(full = true))
            }
            "FIELD" -> {
                requireNotNull(target) { "At targeting FIELD expects its target to be a field descriptor" }
                if (opcode != null) {
                    require(
                        opcode in setOf(
                            Opcodes.GETFIELD,
                            Opcodes.GETSTATIC,
                            Opcodes.PUTFIELD,
                            Opcodes.PUTSTATIC
                        )
                    ) {
                        "At targeting FIELD expects its opcode to be one of: GETFIELD, GETSTATIC, PUTFIELD, PUTSTATIC"
                    }
                    val isGet = opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC
                    val isStatic = opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC
                    FieldTarget(Descriptor.Parser(target).parseField(full = true), isGet, isStatic)
                } else {
                    FieldTarget(Descriptor.Parser(target).parseField(full = true), null, null)
                }
            }
            "CONSTANT" -> {
                require(args != null) {
                    "At targeting CONSTANT requires args"
                }
                args.firstNotNullOfOrNull {
                    val key = it.substringBefore('=')
                    val type = when (key) {
                        "null" -> Any::class.descriptor() // Is this right?
                        "intValue" -> Descriptor.Primitive.INT
                        "floatValue" -> Descriptor.Primitive.FLOAT
                        "longValue" -> Descriptor.Primitive.LONG
                        "doubleValue" -> Descriptor.Primitive.DOUBLE
                        "stringValue" -> String::class.descriptor()
                        "classValue" -> Descriptor.Object("L${it.substringAfter("=")};")
                        else -> return@firstNotNullOfOrNull null
                    }

                    ConstantTarget(key, type)
                } ?: error("At targeting CONSTANT expects a typeValue arg")
            }
            else -> error("Invalid At.value for Utils.getAtTarget: ${value}")
        }
    }

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
