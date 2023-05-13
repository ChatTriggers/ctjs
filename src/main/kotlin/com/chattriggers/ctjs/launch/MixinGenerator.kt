package com.chattriggers.ctjs.launch

import codes.som.koffee.assembleClass
import codes.som.koffee.insns.InstructionAssembly
import codes.som.koffee.insns.jvm.*
import codes.som.koffee.modifiers.public
import com.chattriggers.ctjs.engine.module.ModuleMixin
import com.chattriggers.ctjs.triggers.TriggerType
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.At.Shift
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import org.spongepowered.asm.service.MixinService
import java.lang.reflect.Modifier

class MixinGenerator(private val className: String, private val eventName: String, private val mixin: ModuleMixin) {
    fun generate(): ByteArray {
        val mappedClass = Mappings.getMappedClass(mixin.className.replace('.', '/'))
        val classNode = MixinService.getService().bytecodeProvider.getClassNode(mappedClass.name.value)
        val mappedMethod = mappedClass.findMethod(mixin.method, classNode)!!
        val methodNode = classNode.methods.first { it.name == mappedMethod.name.value }

        val methodType = Type.getType(methodNode.desc)
        val isStatic = Modifier.isStatic(methodNode.access)

        val mixinClassNode = assembleClass(public, className, version = Opcodes.V17) {
            val parameters = methodType.argumentTypes.toMutableList()

            if (methodType.returnType.sort == Type.VOID) {
                parameters.add(Type.getType(CallbackInfo::class.java))
            } else {
                parameters.add(Type.getType(CallbackInfoReturnable::class.java))
            }

            var modifiers = public
            if (isStatic)
                modifiers += static

            val staticParamOffset = if (isStatic) 0 else 1

            val injectMethodNode = method(modifiers, "inject", void, *parameters.toTypedArray()) {
                getstatic(TriggerType::class, "Mixin", TriggerType::class)

                ldc(parameters.size + 1 + staticParamOffset)
                anewarray(Any::class)

                dup
                ldc(0)
                ldc(eventName)
                aastore

                if (!isStatic) {
                    dup
                    ldc(1)
                    aload(0)
                    aastore
                }

                parameters.forEachIndexed { index, string ->
                    dup
                    ldc(index + 1 + staticParamOffset)
                    getOpcodeForType(string)(index + 1)

                    // Box primitives if necessary
                    when (parameters[index].sort) {
                        Type.VOID -> TODO("is this possible?")
                        Type.BOOLEAN -> invokestatic(java.lang.Boolean::class, "valueOf", java.lang.Boolean::class, boolean)
                        Type.CHAR -> invokestatic(java.lang.Character::class, "valueOf", java.lang.Character::class, char)
                        Type.BYTE -> invokestatic(java.lang.Byte::class, "valueOf", java.lang.Byte::class, byte)
                        Type.SHORT -> invokestatic(java.lang.Short::class, "valueOf", java.lang.Short::class, short)
                        Type.INT -> invokestatic(java.lang.Integer::class, "valueOf", java.lang.Integer::class, int)
                        Type.FLOAT -> invokestatic(java.lang.Float::class, "valueOf", java.lang.Float::class, float)
                        Type.LONG -> invokestatic(java.lang.Long::class, "valueOf", java.lang.Long::class, long)
                        Type.DOUBLE -> invokestatic(java.lang.Double::class, "valueOf", java.lang.Double::class, double)
                        else -> {}
                    }

                    aastore
                }

                invokevirtual(TriggerType::class, "triggerAll", void, Array<Any>::class)
                _return
            }

            val injectAnnotation = injectMethodNode.visitAnnotation(Inject::class.java.descriptorString(), true)
            injectAnnotation.visit("method", listOf(mappedMethod.name.value))
            if (mixin.cancellable != null)
                injectAnnotation.visit("cancellable", mixin.cancellable)

            val atAnnotation = AnnotationNode(At::class.java.descriptorString())
            atAnnotation.visit("value", mixin.at.value)
            if (mixin.at.shift != null)
                atAnnotation.visit("shift", arrayOf(Shift::class.java.descriptorString(), mixin.at.shift.name))
            if (mixin.at.by != null)
                atAnnotation.visit("by", mixin.at.by)
            if (mixin.at.target != null)
                atAnnotation.visit("target", mixin.at.target)
            if (mixin.at.ordinal != null)
                atAnnotation.visit("ordinal", mixin.at.ordinal)
            if (mixin.at.opcode != null)
                atAnnotation.visit("opcode", mixin.at.opcode.value)

            injectAnnotation.visit("at", listOf(atAnnotation))
            injectAnnotation.visitEnd()
        }

        val mixinAnnotation = mixinClassNode.visitAnnotation(Mixin::class.java.descriptorString(), false)
        mixinAnnotation.visit("targets", listOf(mappedClass.name.value))
        mixinAnnotation.visitEnd()

        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        mixinClassNode.accept(writer)
        writer.toByteArray()

        return writer.toByteArray()
    }

    private fun InstructionAssembly.getOpcodeForType(type: Type): (Int) -> Unit {
        return when (type.sort) {
            Type.BOOLEAN, Type.BYTE, Type.SHORT, Type.INT -> ::iload
            Type.LONG -> ::lload
            Type.FLOAT -> ::fload
            Type.DOUBLE -> ::dload
            else -> ::aload
        }
    }
}
