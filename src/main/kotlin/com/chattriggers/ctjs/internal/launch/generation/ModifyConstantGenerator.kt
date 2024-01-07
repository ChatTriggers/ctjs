package com.chattriggers.ctjs.internal.launch.generation

import codes.som.koffee.MethodAssembly
import com.chattriggers.ctjs.internal.launch.ModifyConstant
import com.chattriggers.ctjs.internal.utils.descriptorString
import org.objectweb.asm.tree.MethodNode
import org.spongepowered.asm.mixin.injection.ModifyConstant as SPModifyConstant

internal class ModifyConstantGenerator(
    ctx: GenerationContext,
    id: Int,
    private val modifyConstant: ModifyConstant,
) : InjectorGenerator(ctx, id) {
    override val type = "modifyConstant"

    override fun getInjectionSignature(): InjectionSignature {
        val (mappedMethod, method) = ctx.findMethod(modifyConstant.method)

        val type = modifyConstant.constant.getTypeDescriptor()
        val parameters = listOf(Parameter(type)) + modifyConstant.locals?.map(Utils::getParameterFromLocal).orEmpty()

        return InjectionSignature(
            mappedMethod,
            parameters,
            type,
            method.isStatic,
        )
    }

    override fun attachAnnotation(node: MethodNode, signature: InjectionSignature) {
        node.visitAnnotation(SPModifyConstant::class.descriptorString(), true).apply {
            visit("method", signature.targetMethod.toFullDescriptor())
            if (modifyConstant.slice != null)
                visit("slice", modifyConstant.slice.map(Utils::createSliceAnnotation))
            visit("constant", listOf(Utils.createConstantAnnotation(modifyConstant.constant)))
            if (modifyConstant.remap != null)
                visit("remap", modifyConstant.remap)
            if (modifyConstant.require != null)
                visit("require", modifyConstant.require)
            if (modifyConstant.expect != null)
                visit("expect", modifyConstant.expect)
            if (modifyConstant.allow != null)
                visit("allow", modifyConstant.allow)
            if (modifyConstant.constraints != null)
                visit("constraints", modifyConstant.constraints)
            visitEnd()
        }
    }

    context(MethodAssembly)
    override fun generateNotAttachedBehavior() {
        generateParameterLoad(0)
    }
}
