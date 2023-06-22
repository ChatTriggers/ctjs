package com.chattriggers.ctjs.launch.generation

import com.chattriggers.ctjs.launch.Descriptor
import com.chattriggers.ctjs.launch.ModifyReturnValue
import com.chattriggers.ctjs.utils.descriptorString
import org.objectweb.asm.tree.MethodNode
import com.llamalad7.mixinextras.injector.ModifyReturnValue as SPModifyReturnValue

internal class ModifyReturnValueInjector(
    ctx: GenerationContext,
    id: Int,
    private val modifyReturnValue: ModifyReturnValue
) : InjectorGenerator(ctx, id) {
    override val type = "modifyReturnValue"

    override fun getInjectionSignature(): InjectionSignature {
        val (mappedMethod, method) = ctx.findMethod(modifyReturnValue.method)
        val returnType = Descriptor.Parser(mappedMethod.returnType.value).parseType(full = true)

        val parameters = listOf(Parameter(returnType)) + modifyReturnValue.locals?.map {
            Utils.getParameterFromLocal(it, mappedMethod)
        }.orEmpty()

        return InjectionSignature(
            mappedMethod,
            parameters,
            returnType,
            method.isStatic,
        )
    }

    override fun attachAnnotation(node: MethodNode, signature: InjectionSignature) {
        node.visitAnnotation(SPModifyReturnValue::class.descriptorString(), true).apply {
            visit("method", listOf(signature.targetMethod.toFullDescriptor()))
            visit("at", Utils.createAtAnnotation(modifyReturnValue.at))
            if (modifyReturnValue.slice != null)
                visit("slice", listOf(modifyReturnValue.slice.map(Utils::createSliceAnnotation)))
            if (modifyReturnValue.remap != null)
                visit("remap", modifyReturnValue.remap)
            if (modifyReturnValue.require != null)
                visit("require", modifyReturnValue.require)
            if (modifyReturnValue.expect != null)
                visit("expect", modifyReturnValue.expect)
            if (modifyReturnValue.allow != null)
                visit("allow", modifyReturnValue.allow)
            visitEnd()
        }
    }
}
