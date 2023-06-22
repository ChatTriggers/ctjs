package com.chattriggers.ctjs.launch.generation

import com.chattriggers.ctjs.launch.ModifyArg
import com.chattriggers.ctjs.utils.descriptorString
import org.objectweb.asm.tree.MethodNode
import org.spongepowered.asm.mixin.injection.ModifyArg as SPModifyArg

internal class ModifyArgGenerator(
    ctx: GenerationContext,
    id: Int,
    private val modifyArg: ModifyArg,
) : InjectorGenerator(ctx, id) {
    override val type = "modifyArg"

    override fun getInjectionSignature(): InjectionSignature {
        val (mappedMethod, method) = ctx.findMethod(modifyArg.method)

        // Resolve the target method
        val atTarget = Utils.getAtTargetDescriptor(modifyArg.at)
        check(atTarget is Utils.InvokeAtTarget) { "ModifyArg expects At.target to be INVOKE" }
        val targetDescriptor = atTarget.descriptor
        requireNotNull(targetDescriptor.parameters)

        if (modifyArg.index !in targetDescriptor.parameters.indices)
            error("ModifyArg received an out-of-bounds index ${modifyArg.index}")

        val parameters = if (modifyArg.captureAllParams == true) {
            targetDescriptor.parameters.mapTo(mutableListOf(), ::Parameter)
        } else mutableListOf(Parameter(targetDescriptor.parameters[modifyArg.index]))

        val returnType = targetDescriptor.parameters[modifyArg.index]

        modifyArg.locals?.forEach {
            parameters.add(Utils.getParameterFromLocal(it, mappedMethod))
        }

        return InjectionSignature(
            mappedMethod,
            parameters,
            returnType,
            method.isStatic,
        )
    }

    override fun attachAnnotation(node: MethodNode, signature: InjectionSignature) {
        node.visitAnnotation(SPModifyArg::class.descriptorString(), true).apply {
            visit("method", listOf(signature.targetMethod.toFullDescriptor()))
            if (modifyArg.slice != null)
                visit("slice", modifyArg.slice)
            visit("at", Utils.createAtAnnotation(modifyArg.at))
            visit("index", modifyArg.index)
            if (modifyArg.remap != null)
                visit("remap", modifyArg.remap)
            if (modifyArg.require != null)
                visit("require", modifyArg.require)
            if (modifyArg.expect != null)
                visit("expect", modifyArg.expect)
            if (modifyArg.allow != null)
                visit("allow", modifyArg.allow)
            if (modifyArg.constraints != null)
                visit("constraints", modifyArg.constraints)
        }
    }
}
