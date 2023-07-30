package com.chattriggers.ctjs.utils

import com.chattriggers.ctjs.CTJS
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.mapping.tree.Descriptored
import net.fabricmc.mapping.tree.Mapped
import net.fabricmc.mapping.tree.TinyMappingFactory
import org.objectweb.asm.Type
import org.spongepowered.asm.mixin.transformer.ClassInfo
import java.io.ByteArrayInputStream
import java.net.URL
import java.nio.file.Files
import java.util.zip.ZipFile

/**
 * Allows runtime inspection of mappings
 */
object Mappings {
    private const val YARN_MAPPINGS_URL_PREFIX = "https://maven.fabricmc.net/net/fabricmc/yarn/"

    // If this is changed, also change the Java.type function in mixinProvidedLibs.js
    internal val mappedPackages = setOf("Lnet/minecraft/", "Lcom/mojang/blaze3d/")

    private val unmappedClasses = mutableMapOf<String, MappedClass>()
    private val mappedToUnmappedClassNames = mutableMapOf<String, String>()

    internal fun initialize() {
        val container = FabricLoader.getInstance().getModContainer("chattriggers")
        val mappingVersion = container.get().metadata.getCustomValue("ctjs:yarn-mappings").asString
        val jarName = "yarn-$mappingVersion-v2.jar".urlEncode()

        val jarBytes = URL("$YARN_MAPPINGS_URL_PREFIX${mappingVersion.urlEncode()}/$jarName").readBytes()
        val tempFile = Files.createTempFile("ctjs", "mapping").toFile()
        tempFile.writeBytes(jarBytes)

        val mappingBytes = ZipFile(tempFile).use { file ->
            file.getInputStream(file.getEntry("mappings/mappings.tiny")).readAllBytes()
        }

        val tree = TinyMappingFactory.load(ByteArrayInputStream(mappingBytes).bufferedReader())

        tree.classes.forEach { clazz ->
            val fields = mutableMapOf<String, MappedField>()

            clazz.fields.forEach { field ->
                fields[field.unmappedName] = MappedField(
                    name = Mapping.fromMapped(field),
                    type = Mapping(field.unmappedType.descriptor, field.mappedType.descriptor)
                )
            }

            val methods = mutableMapOf<String, MutableList<MappedMethod>>()

            clazz.methods.forEach { method ->
                val unmappedType = method.unmappedType
                val mappedType = method.mappedType

                methods.getOrPut(method.unmappedName, ::mutableListOf).add(
                    MappedMethod(
                        name = Mapping.fromMapped(method),
                        parameters = method.parameters.sortedBy { it.localVariableIndex }.mapIndexed { index, param ->
                            MappedParameter(
                                Mapping(param.unmappedName, param.mappedName),
                                Mapping(
                                    unmappedType.argumentTypes[index].descriptor,
                                    mappedType.argumentTypes[index].descriptor,
                                ),
                                param.localVariableIndex,
                            )
                        },
                        returnType = Mapping(unmappedType.returnType.descriptor, mappedType.returnType.descriptor)
                    )
                )
            }

            unmappedClasses[clazz.unmappedName] = MappedClass(
                name = Mapping.fromMapped(clazz),
                fields,
                methods
            )

            if (CTJS.isDevelopment) {
                mappedToUnmappedClassNames[clazz.unmappedName] = clazz.unmappedName
            } else {
                mappedToUnmappedClassNames[clazz.mappedName] = clazz.unmappedName
            }
        }
    }

    internal fun getMappedClass(unmappedClassName: String): MappedClass? {
        var name = unmappedClassName.let(Mappings::normalizeClassName)
        mappedToUnmappedClassNames[name]?.also { name = it }
        return unmappedClasses[name]
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
        return mappedToUnmappedClassNames[normalizeClassName(className)]
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
            get() = if (CTJS.isDevelopment) original else mapped

        companion object {
            fun fromMapped(mapped: Mapped) = Mapping(mapped.unmappedName, mapped.mappedName)
        }
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

            val unmappedSuperClass = mappedToUnmappedClassNames[classInfo.superName] ?: return null
            return unmappedClasses[unmappedSuperClass]?.findMethods(name, classInfo.superClass)
        }
    }

    private val Mapped.unmappedName: String
        get() = getName("named")

    private val Mapped.mappedName: String
        get() = getName("intermediary")

    private val Descriptored.unmappedType: Type
        get() = Type.getType(getDescriptor("named"))

    private val Descriptored.mappedType: Type
        get() = Type.getType(getDescriptor("intermediary"))
}
