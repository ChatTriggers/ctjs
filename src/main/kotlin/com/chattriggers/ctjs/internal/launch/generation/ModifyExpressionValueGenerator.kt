package com.chattriggers.ctjs.internal.launch.generation

import com.chattriggers.ctjs.internal.launch.ModifyExpressionValue
import com.chattriggers.ctjs.internal.utils.descriptorString
import org.objectweb.asm.tree.MethodNode
import com.llamalad7.mixinextras.injector.ModifyExpressionValue as SPModifyExpressionValue

internal class ModifyExpressionValueGenerator(
    ctx: GenerationContext,
    id: Int,
    private val modifyExpressionValue: ModifyExpressionValue,
) : InjectorGenerator(ctx, id) {
    override val type = "modifyExpressionValue"

    override fun getInjectionSignature(): InjectionSignature {
        val (mappedMethod, method) = ctx.findMethod(modifyExpressionValue.method)

        val exprDescriptor = when (val atTarget = Utils.getAtTargetDescriptor(modifyExpressionValue.at)) {
            is Utils.InvokeAtTarget -> atTarget.descriptor.returnType
            is Utils.FieldAtTarget -> atTarget.descriptor.type
            is Utils.NewAtTarget -> atTarget.descriptor.type
            is Utils.ConstantAtTarget -> atTarget.descriptor
        }

        check(exprDescriptor != null && exprDescriptor.isType)

        val parameters = listOf(Parameter(exprDescriptor)) + modifyExpressionValue.locals
            ?.map(Utils::getParameterFromLocal)
            .orEmpty()

        return InjectionSignature(
            mappedMethod,
            parameters,
            exprDescriptor,
            method.isStatic,
        )
    }

    override fun attachAnnotation(node: MethodNode, signature: InjectionSignature) {
        node.visitAnnotation(SPModifyExpressionValue::class.descriptorString(), true).apply {
            visit("method", listOf(signature.targetMethod.toFullDescriptor()))
            visit("at", Utils.createAtAnnotation(modifyExpressionValue.at))
            if (modifyExpressionValue.slice != null)
                visit("slice", modifyExpressionValue.slice.map(Utils::createSliceAnnotation))
            if (modifyExpressionValue.remap != null)
                visit("remap", modifyExpressionValue.remap)
            if (modifyExpressionValue.require != null)
                visit("require", modifyExpressionValue.require)
            if (modifyExpressionValue.expect != null)
                visit("expect", modifyExpressionValue.expect)
            if (modifyExpressionValue.allow != null)
                visit("allow", modifyExpressionValue.allow)
            visitEnd()
        }
    }
}
