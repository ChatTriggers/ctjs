package com.chattriggers.ctjs.launch.generation

import com.chattriggers.ctjs.launch.Descriptor
import com.chattriggers.ctjs.launch.Mappings
import com.chattriggers.ctjs.launch.WrapOperation
import com.chattriggers.ctjs.utils.descriptor
import com.chattriggers.ctjs.utils.descriptorString
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation as SPWrapOperation

internal class WrapOperationGenerator(
    ctx: GenerationContext,
    id: Int,
    private val wrapOperation: WrapOperation,
) : InjectorGenerator(ctx, id) {
    override val type = "wrapOperation"

    override fun getInjectionSignature(): InjectionSignature {
        val (mappedMethod, method) = ctx.findMethod(wrapOperation.method)

        val parameters = mutableListOf<Parameter>()
        val returnType: Descriptor

        if (wrapOperation.at == null) {
            require(wrapOperation.constant != null) {
                "WrapOperation must specify either 'at' or 'constant'"
            }
            require(wrapOperation.constant.classValue != null) {
                "WrapOperation targeting INSTANCEOF must specify constant.classValue"
            }

            parameters.add(Parameter(Any::class.descriptor()))
            parameters.add(Parameter(Operation::class.descriptor()))
            returnType = Descriptor.Primitive.BOOLEAN
        } else {
            when (val atTarget = Utils.getAtTargetDescriptor(wrapOperation.at)) {
                is Utils.InvokeAtTarget -> {
                    val descriptor = atTarget.descriptor

                    val targetClass = Mappings.getMappedClass(descriptor.owner!!.originalDescriptor())
                        ?: error("Unknown class ${descriptor.owner}")
                    val targetMethodIsStatic = Utils.findMethod(targetClass, descriptor).second.isStatic

                    if (!targetMethodIsStatic)
                        parameters.add(Parameter(descriptor.owner))

                    descriptor.parameters!!.forEach {
                        parameters.add(Parameter(it))
                    }
                    parameters.add(Parameter(Operation::class.descriptor()))
                    returnType = descriptor.returnType!!
                }
                is Utils.FieldAtTarget -> {
                    require(atTarget.isStatic != null && atTarget.isGet != null) {
                        "WrapOperation targeting FIELD expects an opcode value"
                    }

                    val descriptor = atTarget.descriptor
                    if (!atTarget.isStatic)
                        parameters.add(Parameter(descriptor.owner!!))

                    if (!atTarget.isGet)
                        parameters.add(Parameter(descriptor.type!!))

                    parameters.add(Parameter(Operation::class.descriptor()))

                    returnType = if (atTarget.isGet) {
                        descriptor.type!!
                    } else Descriptor.Primitive.VOID
                }
                else -> error("Unexpected At.target for WrapOperation: ${atTarget.targetName}")
            }
        }

        return InjectionSignature(
            mappedMethod,
            parameters,
            returnType,
            method.isStatic,
        )
    }

    override fun attachAnnotation(node: MethodNode, signature: InjectionSignature) {
        node.visitAnnotation(SPWrapOperation::class.descriptorString(), true).apply {
            visit("method", signature.targetMethod.toFullDescriptor())
            if (wrapOperation.at != null)
                visit("at", Utils.createAtAnnotation(wrapOperation.at))
            if (wrapOperation.constant != null)
                visit("constant", Utils.createConstantAnnotation(wrapOperation.constant))
            if (wrapOperation.slice != null)
                visit("slice", wrapOperation.slice.map(Utils::createSliceAnnotation))
            if (wrapOperation.remap != null)
                visit("remap", wrapOperation.remap)
            if (wrapOperation.require != null)
                visit("require", wrapOperation.require)
            if (wrapOperation.expect != null)
                visit("expect", wrapOperation.expect)
            if (wrapOperation.allow != null)
                visit("allow", wrapOperation.allow)
            visitEnd()
        }
    }
}
