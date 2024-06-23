package com.chattriggers.ctjs.internal.launch.generation

import codes.som.koffee.ClassAssembly
import codes.som.koffee.MethodAssembly
import codes.som.koffee.insns.InstructionAssembly
import codes.som.koffee.insns.jvm.*
import codes.som.koffee.insns.sugar.JumpCondition
import codes.som.koffee.insns.sugar.ifStatement
import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.api.Mappings
import com.chattriggers.ctjs.internal.engine.JSLoader
import com.chattriggers.ctjs.internal.launch.Descriptor
import com.chattriggers.ctjs.internal.launch.InvokeDynamicSupport
import com.chattriggers.ctjs.internal.launch.Local
import com.chattriggers.ctjs.internal.utils.descriptor
import com.chattriggers.ctjs.internal.utils.descriptorString
import org.objectweb.asm.tree.MethodNode

internal abstract class InjectorGenerator(protected val ctx: GenerationContext, val id: Int) {
    abstract val type: String
    protected val signature by lazy { getInjectionSignature() }
    protected val parameterDescriptors by lazy {
        signature.parameters.map {
            // Check if the type needs to be wrapped in a ref. Also handle the case where
            // the user provides an explicitly wrapped type
            if (it.local?.mutable == true && !it.descriptor.originalDescriptor()
                    .startsWith("Lcom/llamalad7/mixinextras/sugar/ref/")
            ) {
                when (it.descriptor) {
                    com.chattriggers.ctjs.internal.launch.Descriptor.Primitive.BOOLEAN -> com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef::class.descriptor()
                    com.chattriggers.ctjs.internal.launch.Descriptor.Primitive.BYTE -> com.llamalad7.mixinextras.sugar.ref.LocalByteRef::class.descriptor()
                    com.chattriggers.ctjs.internal.launch.Descriptor.Primitive.CHAR -> com.llamalad7.mixinextras.sugar.ref.LocalCharRef::class.descriptor()
                    com.chattriggers.ctjs.internal.launch.Descriptor.Primitive.DOUBLE -> com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef::class.descriptor()
                    com.chattriggers.ctjs.internal.launch.Descriptor.Primitive.FLOAT -> com.llamalad7.mixinextras.sugar.ref.LocalFloatRef::class.descriptor()
                    com.chattriggers.ctjs.internal.launch.Descriptor.Primitive.INT -> com.llamalad7.mixinextras.sugar.ref.LocalIntRef::class.descriptor()
                    com.chattriggers.ctjs.internal.launch.Descriptor.Primitive.LONG -> com.llamalad7.mixinextras.sugar.ref.LocalLongRef::class.descriptor()
                    com.chattriggers.ctjs.internal.launch.Descriptor.Primitive.SHORT -> com.llamalad7.mixinextras.sugar.ref.LocalShortRef::class.descriptor()
                    else -> com.llamalad7.mixinextras.sugar.ref.LocalRef::class.descriptor()
                }
            } else it.descriptor
        }
    }

    abstract fun getInjectionSignature(): InjectionSignature

    abstract fun attachAnnotation(node: MethodNode, signature: InjectionSignature)

    context(MethodAssembly)
    abstract fun generateNotAttachedBehavior()

    context(ClassAssembly)
    fun generate() {
        val (targetMethod, parameters, returnType, isStatic) = signature

        var modifiers = private
        if (isStatic)
            modifiers += static

        val nameForInjection = targetMethod.name.original.replace('<', '$').replace('>', '$')
        val methodNode = method(
            modifiers,
            "${CTJS.MOD_ID}_${type}_${nameForInjection}_${counter++}",
            returnType.toMappedType(),
            *parameterDescriptors.map { it.toMappedType() }.toTypedArray(),
        ) {
            // Apply parameter annotations
            for (i in parameters.indices) {
                val local = parameters[i].local ?: continue
                node.visitParameterAnnotation(i, com.llamalad7.mixinextras.sugar.Local::class.descriptorString(), false)
                    .apply {
                        local.print?.let { visit("print", it) }
                        local.index?.let { visit("index", it) }
                        local.ordinal?.let { visit("ordinal", it) }
                    }
            }

            // Check if we're attached
            ldc(id)
            invokestatic(JSLoader::class, "mixinIsAttached", Boolean::class, Int::class)
            ifStatement(JumpCondition.False) {
                generateNotAttachedBehavior()
                generateReturn(returnType)
            }

            ldc(parameters.size + if (isStatic) 0 else 1)
            anewarray(Any::class)

            if (!isStatic) {
                dup
                ldc(0)
                aload_0
                aastore
            }

            parameterDescriptors.forEachIndexed { index, descriptor ->
                dup
                ldc(index + if (isStatic) 0 else 1)
                generateParameterLoad(index)
                generateBoxIfNecessary(descriptor)
                aastore
            }

            invokedynamic(
                assembleIndyName(nameForInjection, type),
                "([Ljava/lang/Object;)Ljava/lang/Object;",
                InvokeDynamicSupport.BOOTSTRAP_HANDLE,
                arrayOf(id),
            )

            generateUnboxIfNecessary(returnType)
            generateReturn(returnType)
        }

        attachAnnotation(methodNode, signature)
    }

    context(MethodAssembly)
    private fun generateBoxIfNecessary(descriptor: Descriptor) {
        when (descriptor) {
            Descriptor.Primitive.VOID -> throw IllegalStateException("Cannot use Void as a parameter type")
            Descriptor.Primitive.BOOLEAN ->
                invokestatic(java.lang.Boolean::class, "valueOf", java.lang.Boolean::class, boolean)
            Descriptor.Primitive.CHAR ->
                invokestatic(java.lang.Character::class, "valueOf", java.lang.Character::class, char)
            Descriptor.Primitive.BYTE ->
                invokestatic(java.lang.Byte::class, "valueOf", java.lang.Byte::class, byte)
            Descriptor.Primitive.SHORT ->
                invokestatic(java.lang.Short::class, "valueOf", java.lang.Short::class, short)
            Descriptor.Primitive.INT ->
                invokestatic(java.lang.Integer::class, "valueOf", java.lang.Integer::class, int)
            Descriptor.Primitive.FLOAT ->
                invokestatic(java.lang.Float::class, "valueOf", java.lang.Float::class, float)
            Descriptor.Primitive.LONG ->
                invokestatic(java.lang.Long::class, "valueOf", java.lang.Long::class, long)
            Descriptor.Primitive.DOUBLE ->
                invokestatic(java.lang.Double::class, "valueOf", java.lang.Double::class, double)
            else -> {}
        }
    }

    context(MethodAssembly)
    private fun generateUnboxIfNecessary(descriptor: Descriptor) {
        when (descriptor) {
            Descriptor.Primitive.VOID -> {}
            Descriptor.Primitive.BOOLEAN -> {
                checkcast(java.lang.Boolean::class)
                invokevirtual(java.lang.Boolean::class, "booleanValue", boolean)
            }
            is Descriptor.Primitive -> {
                checkcast(java.lang.Number::class)

                when (descriptor) {
                    Descriptor.Primitive.CHAR -> invokevirtual(java.lang.Number::class, "charValue", char)
                    Descriptor.Primitive.BYTE -> invokevirtual(java.lang.Number::class, "byteValue", byte)
                    Descriptor.Primitive.SHORT -> invokevirtual(java.lang.Number::class, "shortValue", short)
                    Descriptor.Primitive.INT -> invokevirtual(java.lang.Number::class, "intValue", int)
                    Descriptor.Primitive.LONG -> invokevirtual(java.lang.Number::class, "longValue", long)
                    Descriptor.Primitive.FLOAT -> invokevirtual(java.lang.Number::class, "floatValue", float)
                    Descriptor.Primitive.DOUBLE -> invokevirtual(java.lang.Number::class, "doubleValue", double)
                    else -> throw IllegalStateException()
                }
            }
            else -> checkcast(descriptor.toMappedType())
        }
    }

    context(MethodAssembly)
    private fun generateReturn(returnType: Descriptor) {
        when (returnType) {
            Descriptor.Primitive.VOID -> {
                pop
                _return
            }
            Descriptor.Primitive.BOOLEAN -> ireturn
            Descriptor.Primitive.LONG -> lreturn
            Descriptor.Primitive.FLOAT -> freturn
            Descriptor.Primitive.DOUBLE -> dreturn
            is Descriptor.Primitive -> ireturn
            else -> areturn
        }
    }

    protected fun InstructionAssembly.generateParameterLoad(parameterIndex: Int) {
        val localIndex = (0 until parameterIndex).sumOf {
            val descriptor = parameterDescriptors[it]
            if (descriptor == Descriptor.Primitive.LONG || descriptor == Descriptor.Primitive.DOUBLE) {
                // Compiler bug
                @Suppress("USELESS_CAST")
                2 as Int
            } else 1
        }
        generateLoad(parameterDescriptors[parameterIndex], localIndex)
    }

    protected fun InstructionAssembly.generateLoad(descriptor: Descriptor, index: Int) {
        val modifiedIndex = if (signature.isStatic) index else index + 1
        when (descriptor) {
            Descriptor.Primitive.BOOLEAN,
            Descriptor.Primitive.BYTE,
            Descriptor.Primitive.SHORT,
            Descriptor.Primitive.INT -> ::iload
            Descriptor.Primitive.LONG -> ::lload
            Descriptor.Primitive.FLOAT -> ::fload
            Descriptor.Primitive.DOUBLE -> ::dload
            else -> ::aload
        }(modifiedIndex)
    }

    data class Parameter(
        val descriptor: Descriptor,
        val local: Local? = null,
    )

    data class InjectionSignature(
        val targetMethod: Mappings.MappedMethod,
        val parameters: List<Parameter>,
        val returnType: Descriptor,
        val isStatic: Boolean,
    )

    companion object {
        private var counter = 0

        fun assembleIndyName(methodName: String, injectionType: String) =
            "invokeDynamic_${methodName}_${injectionType}_${counter++}"
    }
}
