package com.chattriggers.ctjs.internal.launch.generation

import codes.som.koffee.MethodAssembly
import codes.som.koffee.insns.jvm.*
import codes.som.koffee.insns.sugar.construct
import com.chattriggers.ctjs.api.Mappings
import com.chattriggers.ctjs.internal.launch.At
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

        when (val atTarget = redirect.at.atTarget) {
            is At.InvokeTarget -> {
                val descriptor = atTarget.descriptor

                val targetClass = Mappings.getMappedClass(descriptor.owner!!.originalDescriptor())
                    ?: error("Unknown class ${descriptor.owner}")
                val targetMethod = Utils.findMethod(targetClass, descriptor).second
                if (!targetMethod.isStatic)
                    parameters.add(Parameter(descriptor.owner))
                descriptor.parameters!!.forEach { parameters.add(Parameter(it)) }
                returnType = descriptor.returnType!!
            }
            is At.FieldTarget -> {
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
            is At.NewTarget -> {
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

    context(MethodAssembly)
    override fun generateNotAttachedBehavior() {
        val parameters = signature.parameters.filter { it.local == null }

        when (val target = redirect.at.atTarget) {
            is At.FieldTarget -> {
                parameters.indices.forEach { generateParameterLoad(it) }

                val owner = target.descriptor.owner!!.toType()
                val name = target.descriptor.name
                val type = target.descriptor.type!!.toType()

                if (target.isGet!!) {
                    if (target.isStatic!!) {
                        getstatic(owner, name, type)
                    } else {
                        getfield(owner, name, type)
                    }
                } else {
                    if (target.isStatic!!) {
                        putstatic(owner, name, type)
                    } else {
                        putfield(owner, name, type)
                    }

                    // Must leave something on the stack to pop
                    aconst_null
                }
            }
            is At.InvokeTarget -> {
                parameters.indices.forEach { generateParameterLoad(it) }

                val owner = target.descriptor.owner!!.toType()
                val name = target.descriptor.name
                val returnType = target.descriptor.returnType!!.toType()
                val parameterTypes = parameters.drop(1).map {
                    it.descriptor.toType()
                }.toTypedArray()

                if (signature.isStatic) {
                    invokestatic(owner, name, returnType, *parameterTypes)
                } else {
                    invokevirtual(owner, name, returnType, *parameterTypes)
                }
            }
            is At.NewTarget -> {
                construct(
                    target.descriptor.type.toType(),
                    *parameters.map { it.descriptor.toType() }.toTypedArray(),
                ) {
                    parameters.indices.forEach { generateParameterLoad(it) }
                }
            }
            else -> error("unreachable")
        }
    }
}
