package com.chattriggers.ctjs.launch.generation

import codes.som.koffee.assembleClass
import codes.som.koffee.modifiers.public
import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.engine.MixinDetails
import com.chattriggers.ctjs.launch.*
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.File
import org.spongepowered.asm.mixin.Mixin as SPMixin

internal class DynamicMixinGenerator(private val ctx: GenerationContext, private val details: MixinDetails) {
    val generatedClassName = "CTMixin_\$${ctx.mixin.target.replace('.', '_')}\$_${mixinCounter++}"
    val generatedClassFullPath = "${DynamicMixinManager.GENERATED_PACKAGE}/$generatedClassName"

    fun generate(): ByteArray {
        val mixinClassNode = assembleClass(public, generatedClassFullPath, version = Opcodes.V17) {
            for ((id, injector) in details.injectors) {
                when (injector) {
                    is Inject -> InjectGenerator(ctx, id, injector).generate()
                    is Redirect -> RedirectGenerator(ctx, id, injector).generate()
                    is ModifyArg -> ModifyArgGenerator(ctx, id, injector).generate()
                    is ModifyArgs -> ModifyArgsGenerator(ctx, id, injector).generate()
                    is ModifyConstant -> ModifyConstantGenerator(ctx, id, injector).generate()
                    is ModifyExpressionValue -> ModifyExpressionValueGenerator(ctx, id, injector).generate()
                    is ModifyReceiver -> ModifyReceiverGenerator(ctx, id, injector).generate()
                    is ModifyReturnValue -> ModifyReturnValueInjector(ctx, id, injector).generate()
                    is ModifyVariable -> ModifyVariableGenerator(ctx, id, injector).generate()
                    is WrapOperation -> WrapOperationGenerator(ctx, id, injector).generate()
                    is WrapWithCondition -> WrapWithConditionGenerator(ctx, id, injector).generate()
                }
            }
        }

        val mixinAnnotation = mixinClassNode.visitAnnotation(SPMixin::class.java.descriptorString(), false)
        val mixin = ctx.mixin
        mixinAnnotation.visit("targets", listOf(ctx.mappedClass.name.value))
        if (mixin.priority != null)
            mixinAnnotation.visit("priority", mixin.priority)
        if (mixin.remap != null)
            mixinAnnotation.visit("remap", mixin.remap)
        mixinAnnotation.visitEnd()

        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        mixinClassNode.accept(writer)
        val bytes = writer.toByteArray()

        if (CTJS.isDevelopment) {
            val dir = File(CTJS.configLocation, "ChatTriggers/mixin-classes")
            dir.mkdirs()
            File(dir, "$generatedClassName.class").writeBytes(bytes)
        }

        return bytes
    }

    companion object {
        private var mixinCounter = 0
    }
}
