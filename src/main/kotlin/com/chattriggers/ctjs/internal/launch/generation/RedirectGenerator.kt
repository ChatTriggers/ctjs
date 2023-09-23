package com.chattriggers.ctjs.internal.launch.generation

import com.chattriggers.ctjs.api.Mappings
import com.chattriggers.ctjs.internal.launch.Descriptor
import com.chattriggers.ctjs.internal.launch.Redirect
import com.chattriggers.ctjs.internal.utils.descriptorString
import org.objectweb.asm.tree.MethodNode
import org.spongepowered.asm.mixin.injection.Redirect as SPRedirect

internal class RedirectGenerator(
    ctx: GenerationContext,
    id: Int,
    private val redirect: Redirect,
) : InjectorGenerator(ctx, id) {
    override val type = "redirect"

    override fun getInjectionSignature(): InjectionSignature {
        val (mappedMethod, method) = ctx.findMethod(redirect.method)

        val parameters = mutableListOf<Parameter>()
        val returnType: Descriptor

        when (val atTarget = Utils.getAtTargetDescriptor(redirect.at)) {
            is Utils.InvokeAtTarget -> {
                val descriptor = atTarget.descriptor

                val targetClass = Mappings.getMappedClass(descriptor.owner!!.originalDescriptor())
                    ?: error("Unknown class ${descriptor.owner}")
                val targetMethod = Utils.findMethod(targetClass, descriptor).second
                if (!targetMethod.isStatic)
                    parameters.add(Parameter(descriptor.owner))
                descriptor.parameters!!.forEach { parameters.add(Parameter(it)) }
                returnType = descriptor.returnType!!
            }
            is Utils.FieldAtTarget -> {
                require(atTarget.isStatic != null && atTarget.isGet != null) {
                    "Redirect targeting FIELD expects an opcode value"
                }
                returnType = if (atTarget.isGet) {
                    atTarget.descriptor.type!!
                } else Descriptor.Primitive.VOID

                if (!atTarget.isStatic)
                    parameters.add(Parameter(atTarget.descriptor.owner!!))

                if (!atTarget.isGet)
                    parameters.add(Parameter(atTarget.descriptor.type!!))
            }
            is Utils.NewAtTarget -> {
                atTarget.descriptor.parameters?.forEach {
                    parameters.add(Parameter(it))
                }
                returnType = atTarget.descriptor.type
            }
            else -> error("Invalid target type ${atTarget.targetName} for Redirect inject")
        }

        redirect.locals?.forEach {
            parameters.add(Utils.getParameterFromLocal(it))
        }

        return InjectionSignature(
            mappedMethod,
            parameters,
            returnType,
            method.isStatic,
        )
    }

    override fun attachAnnotation(node: MethodNode, signature: InjectionSignature) {
        node.visitAnnotation(SPRedirect::class.descriptorString(), true).apply {
            visit("method", signature.targetMethod.toFullDescriptor())
            if (redirect.slice != null)
                visit("slice", Utils.createSliceAnnotation(redirect.slice))
            visit("at", Utils.createAtAnnotation(redirect.at))
            if (redirect.remap != null)
                visit("remap", redirect.remap)
            if (redirect.require != null)
                visit("require", redirect.require)
            if (redirect.expect != null)
                visit("expect", redirect.expect)
            if (redirect.allow != null)
                visit("allow", redirect.allow)
            if (redirect.constraints != null)
                visit("constraints", redirect.constraints)
            visitEnd()
        }
    }
}
