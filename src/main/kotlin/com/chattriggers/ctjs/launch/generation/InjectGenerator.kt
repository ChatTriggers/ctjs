package com.chattriggers.ctjs.launch.generation

import com.chattriggers.ctjs.launch.Descriptor
import com.chattriggers.ctjs.launch.Inject
import com.chattriggers.ctjs.utils.descriptor
import org.objectweb.asm.tree.MethodNode
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import org.spongepowered.asm.mixin.injection.Inject as SPInject

internal class InjectGenerator(
    ctx: GenerationContext,
    id: Int,
    private val inject: Inject,
) : InjectorGenerator(ctx, id) {
    override val type = "inject"

    override fun getInjectionSignature(): InjectionSignature {
        val (mappedMethod, method) = ctx.findMethod(inject.method)
        val parameters = mutableListOf<Parameter>()

        if (mappedMethod.returnType.value == "V") {
            parameters.add(Parameter(CallbackInfo::class.descriptor()))
        } else {
            parameters.add(Parameter(CallbackInfoReturnable::class.descriptor()))
        }

        inject.locals?.forEach {
            parameters.add(Utils.getParameterFromLocal(it, mappedMethod))
        }

        return InjectionSignature(
            mappedMethod,
            parameters,
            Descriptor.Primitive.VOID,
            method.isStatic,
        )
    }

    override fun attachAnnotation(node: MethodNode, signature: InjectionSignature) {
        node.visitAnnotation(SPInject::class.java.descriptorString(), true).apply {
            if (inject.id != null)
                visit("id", inject.id)
            visit("method", signature.targetMethod.toFullDescriptor())
            if (inject.slice != null)
                visit("slice", inject.slice.map(Utils::createSliceAnnotation))
            if (inject.at != null)
                visit("at", inject.at.map(Utils::createAtAnnotation))
            if (inject.cancellable != null)
                visit("cancellable", inject.cancellable)
            if (inject.remap != null)
                visit("remap", inject.remap)
            if (inject.require != null)
                visit("require", inject.require)
            if (inject.expect != null)
                visit("expect", inject.expect)
            if (inject.allow != null)
                visit("allow", inject.allow)
            if (inject.constraints != null)
                visit("constraints", inject.constraints)
            visitEnd()
        }
    }
}
