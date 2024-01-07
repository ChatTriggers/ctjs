package com.chattriggers.ctjs.internal.launch.generation

import codes.som.koffee.MethodAssembly
import codes.som.koffee.insns.jvm.aconst_null
import com.chattriggers.ctjs.internal.launch.Descriptor
import com.chattriggers.ctjs.internal.launch.ModifyArgs
import com.chattriggers.ctjs.internal.utils.descriptor
import com.chattriggers.ctjs.internal.utils.descriptorString
import org.objectweb.asm.tree.MethodNode
import org.spongepowered.asm.mixin.injection.invoke.arg.Args
import org.spongepowered.asm.mixin.injection.ModifyArgs as SPModifyArgs

internal class ModifyArgsGenerator(
    ctx: GenerationContext,
    id: Int,
    private val modifyArgs: ModifyArgs,
) : InjectorGenerator(ctx, id) {
    override val type = "modifyArgs"

    override fun getInjectionSignature(): InjectionSignature {
        val (mappedMethod, method) = ctx.findMethod(modifyArgs.method)

        val parameters = mutableListOf<Parameter>()
        parameters.add(Parameter(Args::class.descriptor()))
        modifyArgs.locals?.forEach {
            parameters.add(Utils.getParameterFromLocal(it))
        }

        return InjectionSignature(
            mappedMethod,
            parameters,
            Descriptor.Primitive.VOID,
            method.isStatic,
        )
    }

    override fun attachAnnotation(node: MethodNode, signature: InjectionSignature) {
        node.visitAnnotation(SPModifyArgs::class.descriptorString(), true).apply {
            visit("method", listOf(signature.targetMethod.toFullDescriptor()))
            if (modifyArgs.slice != null)
                visit("slice", modifyArgs.slice)
            visit("at", Utils.createAtAnnotation(modifyArgs.at))
            if (modifyArgs.remap != null)
                visit("remap", modifyArgs.remap)
            if (modifyArgs.expect != null)
                visit("expect", modifyArgs.expect)
            if (modifyArgs.allow != null)
                visit("allow", modifyArgs.allow)
            if (modifyArgs.constraints != null)
                visit("constraints", modifyArgs.constraints)
        }
    }

    context(MethodAssembly)
    override fun generateNotAttachedBehavior() {
        // This method is expected to leave something on the stack
        aconst_null
    }
}
