package com.chattriggers.ctjs.internal.launch.generation

import com.chattriggers.ctjs.api.Mappings
import com.chattriggers.ctjs.internal.launch.*
import com.chattriggers.ctjs.internal.utils.descriptor
import com.chattriggers.ctjs.internal.utils.descriptorString
import net.fabricmc.accesswidener.AccessWidenerReader
import net.fabricmc.loader.impl.FabricLoaderImpl
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.spongepowered.asm.mixin.transformer.ClassInfo
import org.spongepowered.asm.mixin.injection.At as SPAt
import org.spongepowered.asm.mixin.injection.Constant as SPConstant
import org.spongepowered.asm.mixin.injection.Slice as SPSlice

internal object Utils {
    fun createAtAnnotation(at: At): AnnotationNode {
        return AnnotationNode(SPAt::class.descriptorString()).apply {
            if (at.id != null)
                visit("id", at.id)
            visit("value", at.value)
            if (at.slice != null)
                visit("slice", at.slice)
            if (at.shift != null)
                visit("shift", arrayOf(SPAt.Shift::class.java.descriptorString(), at.shift.name))
            if (at.by != null)
                visit("by", at.by)
            if (at.args != null)
                visit("args", at.args)
            if (at.target != null)
                visit("target", getAtTargetDescriptor(at).descriptor.mappedDescriptor())
            if (at.ordinal != null)
                visit("ordinal", at.ordinal)
            if (at.opcode != null)
                visit("opcode", at.opcode)
            if (at.remap != null)
                visit("remap", at.remap)

            visitEnd()
        }
    }

    fun createSliceAnnotation(slice: Slice): AnnotationNode {
        return AnnotationNode(SPSlice::class.descriptorString()).apply {
            if (slice.id != null)
                visit("id", slice.id)
            if (slice.from != null)
                visit("from", createAtAnnotation(slice.from))
            if (slice.to != null)
                visit("to", createAtAnnotation(slice.to))
            visitEnd()
        }
    }

    fun createConstantAnnotation(constant: Constant): AnnotationNode {
        return AnnotationNode(SPConstant::class.descriptorString()).apply {
            if (constant.nullValue != null)
                visit("nullValue", constant.nullValue)
            if (constant.intValue != null)
                visit("intValue", constant.intValue)
            if (constant.floatValue != null)
                visit("floatValue", constant.floatValue)
            if (constant.longValue != null)
                visit("longValue", constant.longValue)
            if (constant.doubleValue != null)
                visit("doubleValue", constant.doubleValue)
            if (constant.stringValue != null)
                visit("stringValue", constant.stringValue)
            if (constant.classValue != null) {
                val name = Mappings.getMappedClassName(constant.classValue)
                    ?: error("Unknown class \"${constant.classValue}\"")
                visit("classValue", Type.getObjectType(name))
            }
            if (constant.ordinal != null)
                visit("ordinal", constant.ordinal)
            if (constant.slice != null)
                visit("slice", constant.slice)
            if (constant.expandZeroConditions != null)
                visit("expandZeroConditions", constant.expandZeroConditions)
            if (constant.log != null)
                visit("log", constant.log)
        }
    }

    fun widenField(mappedClass: Mappings.MappedClass, fieldName: String, isMutable: Boolean) {
        val field = mappedClass.fields[fieldName]
            ?: error("Unable to find field $fieldName in class ${mappedClass.name.original}")

        FabricLoaderImpl.INSTANCE.accessWidener.visitField(
            mappedClass.name.value,
            field.name.value,
            field.type.value,
            AccessWidenerReader.AccessType.ACCESSIBLE,
            false,
        )

        if (isMutable) {
            FabricLoaderImpl.INSTANCE.accessWidener.visitField(
                mappedClass.name.value,
                field.name.value,
                field.type.value,
                AccessWidenerReader.AccessType.MUTABLE,
                false,
            )
        }
    }

    fun widenMethod(
        mappedClass: Mappings.MappedClass,
        methodName: String,
        isMutable: Boolean,
    ) {
        val descriptor = Descriptor.Parser(methodName).parseMethod(full = false)
        val mappedMethod = findMethod(mappedClass, descriptor).first

        FabricLoaderImpl.INSTANCE.accessWidener.visitMethod(
            mappedClass.name.value,
            mappedMethod.name.value,
            mappedMethod.toDescriptor(),
            AccessWidenerReader.AccessType.ACCESSIBLE,
            false,
        )

        if (isMutable) {
            FabricLoaderImpl.INSTANCE.accessWidener.visitMethod(
                mappedClass.name.value,
                mappedMethod.name.value,
                mappedMethod.toDescriptor(),
                AccessWidenerReader.AccessType.MUTABLE,
                false,
            )
        }
    }

    fun findMethod(
        mappedClass: Mappings.MappedClass,
        descriptor: Descriptor.Method,
    ): Pair<Mappings.MappedMethod, ClassInfo.Method> {
        val parameters = descriptor.parameters

        val classInfo = ClassInfo.forName(mappedClass.name.value)
        val mappedMethods = mappedClass.findMethods(descriptor.name, classInfo)
            ?: error("Cannot find method ${descriptor.name} in class ${mappedClass.name.original}")

        var value: Pair<Mappings.MappedMethod, ClassInfo.Method>? = null

        for (method in mappedMethods) {
            if (parameters != null) {
                if (method.parameters.size != parameters.size)
                    continue

                if (method.parameters.zip(parameters).any { it.first.type.original != it.second.originalDescriptor() })
                    continue
            }

            val result = classInfo.findMethodInHierarchy(
                method.name.value,
                method.toDescriptor(),
                ClassInfo.SearchType.ALL_CLASSES,
                ClassInfo.INCLUDE_ALL,
            ) ?: continue

            if (value != null)
                error(
                    "Multiple methods match name ${descriptor.name} in class ${mappedClass.name.original}, please " +
                        "provide a method descriptor"
                )

            value = method to result
        }

        if (value != null)
            return value

        error("Unable to match method $descriptor in class ${mappedClass.name.original}")
    }

    fun getParameterFromLocal(local: Local, name: String = "Local"): InjectorGenerator.Parameter {
        val descriptor = when {
            local.print == true -> {
                // The type doesn't matter, it won't actually be applied
                Descriptor.Primitive.INT
            }
            local.type != null -> {
                if (local.index != null) {
                    require(local.ordinal == null) {
                        "$name that specifies a type and index cannot specify an ordinal"
                    }
                } else {
                    require(local.ordinal != null) {
                        "$name that specifies a type must also specify an index or ordinal"
                    }
                }
                Descriptor.Parser(local.type).parseType(full = true)
            }
            else -> error("$name must specify \"print\", or \"type\" and either \"ordinal\" or \"index\"")
        }

        require(descriptor.isType)

        return InjectorGenerator.Parameter(descriptor, local)
    }

    sealed class AtTarget<T : Descriptor>(val descriptor: T, val targetName: String)

    class InvokeAtTarget(descriptor: Descriptor.Method) : AtTarget<Descriptor.Method>(descriptor, "INVOKE") {
        override fun toString() = descriptor.originalDescriptor()
    }

    class NewAtTarget(descriptor: Descriptor.New) : AtTarget<Descriptor.New>(descriptor, "NEW") {
        override fun toString() = descriptor.originalDescriptor()
    }

    class FieldAtTarget(
        descriptor: Descriptor.Field, val isGet: Boolean?, val isStatic: Boolean?,
    ) : AtTarget<Descriptor.Field>(descriptor, "FIELD") {
        override fun toString() = descriptor.originalDescriptor()
    }

    class ConstantAtTarget(val key: String, descriptor: Descriptor) : AtTarget<Descriptor>(descriptor, "CONSTANT") {
        init {
            require(descriptor.isType)
        }

        override fun toString() = "$key=$descriptor"
    }

    fun getAtTargetDescriptor(at: At): AtTarget<*> {
        return when (at.value) {
            "INVOKE" -> {
                requireNotNull(at.target) { "At targeting INVOKE expects its target to be a method descriptor" }
                InvokeAtTarget(Descriptor.Parser(at.target).parseMethod(full = true))
            }
            "NEW" -> {
                requireNotNull(at.target) { "At targeting NEW expects its target to be a new invocation descriptor" }
                NewAtTarget(Descriptor.Parser(at.target).parseNew(full = true))
            }
            "FIELD" -> {
                requireNotNull(at.target) { "At targeting FIELD expects its target to be a field descriptor" }
                if (at.opcode != null) {
                    require(
                        at.opcode in setOf(
                            Opcodes.GETFIELD,
                            Opcodes.GETSTATIC,
                            Opcodes.PUTFIELD,
                            Opcodes.PUTSTATIC
                        )
                    ) {
                        "At targeting FIELD expects its opcode to be one of: GETFIELD, GETSTATIC, PUTFIELD, PUTSTATIC"
                    }
                    val isGet = at.opcode == Opcodes.GETFIELD || at.opcode == Opcodes.GETSTATIC
                    val isStatic = at.opcode == Opcodes.GETSTATIC || at.opcode == Opcodes.PUTSTATIC
                    FieldAtTarget(Descriptor.Parser(at.target).parseField(full = true), isGet, isStatic)
                } else {
                    FieldAtTarget(Descriptor.Parser(at.target).parseField(full = true), null, null)
                }
            }
            "CONSTANT" -> {
                require(at.args != null) {
                    "At targeting CONSTANT requires args"
                }
                at.args.firstNotNullOfOrNull {
                    val key = it.substringBefore('=')
                    val type = when (key) {
                        "null" -> Any::class.descriptor() // Is this right?
                        "intValue" -> Descriptor.Primitive.INT
                        "floatValue" -> Descriptor.Primitive.FLOAT
                        "longValue" -> Descriptor.Primitive.LONG
                        "doubleValue" -> Descriptor.Primitive.DOUBLE
                        "stringValue" -> String::class.descriptor()
                        "classValue" -> Descriptor.Object("L${it.substringAfter("=")};")
                        else -> return@firstNotNullOfOrNull null
                    }

                    ConstantAtTarget(key, type)
                } ?: error("At targeting CONSTANT expects a typeValue arg")
            }
            else -> error("Invalid At.value for Utils.getAtTarget: ${at.value}")
        }
    }
}
