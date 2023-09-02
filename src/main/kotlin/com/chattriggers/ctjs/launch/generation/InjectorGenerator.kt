package com.chattriggers.ctjs.launch.generation

import codes.som.koffee.ClassAssembly
import codes.som.koffee.insns.InstructionAssembly
import codes.som.koffee.insns.jvm.*
import com.chattriggers.ctjs.engine.js.JSLoader
import com.chattriggers.ctjs.launch.Descriptor
import com.chattriggers.ctjs.launch.InvokeDynamicSupport
import com.chattriggers.ctjs.launch.Local
import com.chattriggers.ctjs.utils.Mappings
import com.chattriggers.ctjs.utils.descriptorString
import com.chattriggers.ctjs.utils.descriptor
import com.llamalad7.mixinextras.sugar.ref.*
import org.objectweb.asm.tree.MethodNode

internal abstract class InjectorGenerator(protected val ctx: GenerationContext, val id: Int) {
    abstract val type: String

    abstract fun getInjectionSignature(): InjectionSignature

    abstract fun attachAnnotation(node: MethodNode, signature: InjectionSignature)

    context(ClassAssembly)
    fun generate() {
        val signature = getInjectionSignature()
        val (targetMethod, parameters, returnType, isStatic) = signature

        var modifiers = private
        if (isStatic)
            modifiers += static

        val parameterTypes = parameters.map {
            // Check if the type needs to be wrapped in a ref. Also handle the case where
            // the user provides an explicitly wrapped type
            if (it.local?.mutable == true && !it.descriptor.originalDescriptor().startsWith("Lcom/llamalad7/mixinextras/sugar/ref/")) {
                when (it.descriptor) {
                    Descriptor.Primitive.BOOLEAN -> LocalBooleanRef::class.descriptor()
                    Descriptor.Primitive.BYTE -> LocalByteRef::class.descriptor()
                    Descriptor.Primitive.CHAR -> LocalCharRef::class.descriptor()
                    Descriptor.Primitive.DOUBLE -> LocalDoubleRef::class.descriptor()
                    Descriptor.Primitive.FLOAT -> LocalFloatRef::class.descriptor()
                    Descriptor.Primitive.INT -> LocalIntRef::class.descriptor()
                    Descriptor.Primitive.LONG -> LocalLongRef::class.descriptor()
                    Descriptor.Primitive.SHORT -> LocalShortRef::class.descriptor()
                    else -> LocalRef::class.descriptor()
                }
            } else it.descriptor
        }

        val methodNode = method(
            modifiers,
            "ctjs_${type}_${targetMethod.name.original}_${counter++}",
            returnType.toMappedType(),
            *parameterTypes.map { it.toMappedType() }.toTypedArray(),
        ) {
            // Apply parameter annotations
            for (i in parameters.indices) {
                val local = parameters[i].local ?: continue
                node.visitParameterAnnotation(i, com.llamalad7.mixinextras.sugar.Local::class.descriptorString(), false).apply {
                    local.print?.let { visit("print", it) }
                    local.index?.let { visit("index", it) }
                    local.ordinal?.let { visit("ordinal", it) }

                    if (local.parameterName != null)
                        visit("argsOnly", true)
                }
            }

            ldc(parameters.size + if (isStatic) 0 else 1)
            anewarray(Any::class)

            if (!isStatic) {
                dup
                ldc(0)
                aload_0
                aastore
            }

            parameterTypes.forEachIndexed { index, descriptor ->
                dup
                ldc(index + if (isStatic) 0 else 1)
                getLoadInsn(descriptor)(index + if (isStatic) 0 else 1)

                // Box primitives if necessary
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

                aastore
            }

            invokedynamic(
                assembleIndyName(targetMethod.name.original, type),
                "([Ljava/lang/Object;)Ljava/lang/Object;",
                InvokeDynamicSupport.BOOTSTRAP_HANDLE,
                arrayOf(id),
            )

            when (returnType) {
                Descriptor.Primitive.VOID -> {
                    pop
                    _return
                }
                Descriptor.Primitive.BOOLEAN -> {
                    checkcast(java.lang.Boolean::class)
                    invokevirtual(java.lang.Boolean::class, "booleanValue", boolean)
                    ireturn
                }
                is Descriptor.Primitive -> {
                    checkcast(java.lang.Number::class)

                    when (returnType) {
                        Descriptor.Primitive.CHAR -> {
                            invokevirtual(java.lang.Number::class, "charValue", char)
                            ireturn
                        }
                        Descriptor.Primitive.BYTE -> {
                            invokevirtual(java.lang.Number::class, "byteValue", byte)
                            ireturn
                        }
                        Descriptor.Primitive.SHORT -> {
                            invokevirtual(java.lang.Number::class, "shortValue", short)
                            ireturn
                        }
                        Descriptor.Primitive.INT -> {
                            invokevirtual(java.lang.Number::class, "intValue", int)
                            ireturn
                        }
                        Descriptor.Primitive.LONG -> {
                            invokevirtual(java.lang.Number::class, "longValue", long)
                            lreturn
                        }
                        Descriptor.Primitive.FLOAT -> {
                            invokevirtual(java.lang.Number::class, "floatValue", float)
                            freturn
                        }
                        Descriptor.Primitive.DOUBLE -> {
                            invokevirtual(java.lang.Number::class, "doubleValue", double)
                            dreturn
                        }
                        else -> throw IllegalStateException()
                    }
                }
                else -> {
                    checkcast(returnType.toMappedType())
                    areturn
                }
            }
        }

        attachAnnotation(methodNode, signature)
    }

    private fun InstructionAssembly.getLoadInsn(descriptor: Descriptor): (Int) -> Unit {
        return when (descriptor) {
            Descriptor.Primitive.BOOLEAN,
            Descriptor.Primitive.BYTE,
            Descriptor.Primitive.SHORT,
            Descriptor.Primitive.INT -> ::iload
            Descriptor.Primitive.LONG -> ::lload
            Descriptor.Primitive.FLOAT -> ::fload
            Descriptor.Primitive.DOUBLE -> ::dload
            else -> ::aload
        }
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

        fun disassembleIndyName(name: String): Pair<String, String> = name.drop("invokeDynamic_".length).let {
            val (methodName, injectionType) = it.split('_')
            methodName to injectionType
        }
    }
}
