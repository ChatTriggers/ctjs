package com.chattriggers.ctjs.internal.launch.generation

import com.chattriggers.ctjs.api.Mappings
import com.chattriggers.ctjs.internal.launch.Descriptor
import com.chattriggers.ctjs.internal.launch.DynamicMixinManager
import com.chattriggers.ctjs.internal.launch.Mixin
import org.spongepowered.asm.mixin.transformer.ClassInfo

internal data class GenerationContext(val mixin: Mixin) {
    val mappedClass = Mappings.getMappedClass(mixin.target) ?: run {
        if (mixin.remap == false) {
            Mappings.getUnmappedClass(mixin.target)
        } else {
            error("Unknown class name ${mixin.target}")
        }
    }
    val generatedClassName = "CTMixin_\$${mixin.target.replace('.', '_')}\$_${mixinCounter++}"
    val generatedClassFullPath = "${DynamicMixinManager.GENERATED_PACKAGE}/$generatedClassName"

    fun findMethod(method: String): Pair<Mappings.MappedMethod, ClassInfo.Method> {
        val descriptor = Descriptor.Parser(method).parseMethod(full = false)
        return Utils.findMethod(mappedClass, descriptor)
    }

    companion object {
        private var mixinCounter = 0
    }
}
