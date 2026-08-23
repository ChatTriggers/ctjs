package com.chattriggers.ctjs.api

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.spongepowered.asm.mixin.transformer.ClassInfo
import org.spongepowered.asm.service.MixinService

/**
 * Allows runtime inspection of mappings
 */
object Mappings {
    // If this is changed, also change the Java.type function in mixinProvidedLibs.js
    internal val mappedPackages = setOf("Lnet/minecraft/", "Lcom/mojang/blaze3d/")

    private val unmappedClasses = mutableMapOf<String, MappedClass>()
    private val mappedToUnmappedClassNames = mutableMapOf<String, String>()

    internal fun initialize() {
        // Minecraft 26.1+ ships unobfuscated. Runtime and source names are the
        // same, so mappings are populated lazily from Mixin's bytecode provider.
        unmappedClasses.clear()
        mappedToUnmappedClassNames.clear()
    }

    internal fun getMappedClass(unmappedClassName: String): MappedClass? {
        var name = normalizeClassName(unmappedClassName)
        mappedToUnmappedClassNames[name]?.also { name = it }
        unmappedClasses[name]?.let { return it }

        return runCatching { getUnmappedClass(name) }.getOrNull()
    }

    internal fun getUnmappedClass(unmappedClassName: String): MappedClass {
        val name = normalizeClassName(unmappedClassName)
        unmappedClasses[name]?.let { return it }

        val classNode = MixinService.getService().bytecodeProvider.getClassNode(name.replace('/', '.'))

        val fields = classNode.fields.associate {
            val type = it.desc
            val fieldName = it.name

            fieldName to MappedField(Mapping(fieldName, fieldName), Mapping(type, type))
        }

        val methods = mutableMapOf<String, MutableList<MappedMethod>>()
        for (method in classNode.methods) {
            val isStatic = method.access and Opcodes.ACC_STATIC != 0
            var lvtIndex = if (isStatic) 0 else 1

            val params = mutableListOf<MappedParameter>()
            Type.getArgumentTypes(method.desc).forEachIndexed { index, type ->
                val paramType = type.descriptor
                val paramName = method.parameters?.getOrNull(index)?.name ?: "arg$index"

                params.add(
                    MappedParameter(
                        Mapping(paramName, paramName),
                        Mapping(paramType, paramType),
                        lvtIndex
                    )
                )

                if (type == Type.DOUBLE_TYPE || type == Type.LONG_TYPE) {
                    lvtIndex += 2
                } else {
                    lvtIndex++
                }
            }

            val returnType = Type.getReturnType(method.desc).descriptor
            val methodName = method.name
            methods.getOrPut(methodName, ::mutableListOf).add(
                MappedMethod(
                    Mapping(methodName, methodName),
                    params,
                    Mapping(returnType, returnType)
                )
            )
        }

        mappedToUnmappedClassNames[name] = name
        return MappedClass(Mapping(name, name), fields, methods).also {
            unmappedClasses[name] = it
        }
    }

    internal fun getMappedClassName(unmappedClassName: String) = getMappedClass(unmappedClassName)?.name?.value

    /**
     * Gets a classes unmapped class name, or throws an error if it is not mapped
     */
    @JvmStatic
    fun unmapClass(clazz: Class<*>) = unmapClassName(clazz.name)

    /**
     * Gets an unmapped class name from a mapped class name, or returns null if
     * it either does not exist or is not mapped.
     */
    @JvmStatic
    fun unmapClassName(className: String): String? {
        val name = normalizeClassName(className)
        return mappedToUnmappedClassNames[name] ?: getMappedClass(name)?.name?.original
    }

    /**
     * Gets the mapped class name from an unmapped class name or null if the class
     * name does not exist. Note that this is not required to use mapped classes,
     * as Rhino performs this mapping automatically during runtime.
     */
    @JvmStatic
    fun mapClassName(className: String) = getMappedClassName(className)

    private fun normalizeClassName(className: String) = (if (className.startsWith('L') && className.endsWith(';')) {
        className.drop(1).dropLast(1)
    } else className).replace('.', '/')

    internal data class Mapping(val original: String, val mapped: String) {
        val value: String
            get() = mapped
    }

    internal data class MappedField(val name: Mapping, val type: Mapping)

    internal class MappedParameter(
        val name: Mapping,
        val type: Mapping,
        val lvtIndex: Int,
    )

    internal class MappedMethod(
        val name: Mapping,
        val parameters: List<MappedParameter>,
        val returnType: Mapping,
    ) {
        fun toDescriptor() = buildString {
            append('(')
            parameters.forEach {
                append(it.type.value)
            }
            append(')')
            append(returnType.value)
        }

        fun toFullDescriptor() = name.value + toDescriptor()
    }

    internal class MappedClass(
        val name: Mapping,
        val fields: Map<String, MappedField>,
        val methods: Map<String, List<MappedMethod>>,
    ) {
        fun findMethods(name: String, classInfo: ClassInfo?): List<MappedMethod>? {
            methods[name]?.let { return it }

            if (classInfo == null)
                return null

            classInfo.superName?.let { superName ->
                getMappedClass(superName)?.findMethods(name, classInfo.superClass)?.let { return it }
            }

            val methods = mutableListOf<MappedMethod>()
            for (itf in classInfo.interfaces) {
                getMappedClass(itf)?.findMethods(name, null)?.let { methods += it }
            }

            return if (methods.isEmpty()) null else methods
        }
    }

}
