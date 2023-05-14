package com.chattriggers.ctjs.launch

import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.impl.launch.FabricLauncherBase
import net.fabricmc.mapping.tree.Descriptored
import net.fabricmc.mapping.tree.Mapped
import net.fabricmc.mapping.tree.TinyMappingFactory
import org.mozilla.javascript.JavaObjectMappingProvider
import org.mozilla.javascript.JavaObjectMappingProvider.MethodSignature
import org.mozilla.javascript.JavaObjectMappingProvider.RenameableField
import org.mozilla.javascript.JavaObjectMappingProvider.RenameableMethod
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.service.MixinService
import java.io.ByteArrayInputStream
import java.lang.reflect.Modifier
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.zip.ZipFile

object Mappings {
    private val isDevelopment = FabricLauncherBase.getLauncher().isDevelopment
    private const val YARN_MAPPINGS_URL_PREFIX = "https://maven.fabricmc.net/net/fabricmc/yarn/"

    private val unmappedClasses = mutableMapOf<String, MappedClass>()
    private val mappedToUnmappedClassNames = mutableMapOf<String, String>()

    var rhinoProvider: JavaObjectMappingProvider? = null

    fun initialize() {
        // TODO: Change modid to ctjs
        val container = FabricLoader.getInstance().getModContainer("chattriggers")
        val mappingVersion = container.get().metadata.getCustomValue("ctjs:yarn-mappings").asString

        val mappingUrlSuffix =
            URLEncoder.encode("$mappingVersion/yarn-$mappingVersion-v2.jar", Charset.defaultCharset())
        val jarBytes = URL(YARN_MAPPINGS_URL_PREFIX + mappingUrlSuffix).readBytes()
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

                methods.getOrPut(method.unmappedName, ::mutableListOf).add(MappedMethod(
                    name = Mapping.fromMapped(method),
                    parameterTypes = unmappedType.argumentTypes.zip(mappedType.argumentTypes).map {
                        Mapping(it.first.descriptor, it.second.descriptor)
                    },
                    returnType = Mapping(unmappedType.returnType.descriptor, mappedType.returnType.descriptor)
                ))
            }

            unmappedClasses[clazz.unmappedName] = MappedClass(
                name = Mapping.fromMapped(clazz),
                fields,
                methods
            )

            if (isDevelopment) {
                mappedToUnmappedClassNames[clazz.unmappedName] = clazz.unmappedName
            } else {
                mappedToUnmappedClassNames[clazz.mappedName] = clazz.unmappedName
            }
        }

        // Create a remapper for Rhino
        if (isDevelopment)
            return

        rhinoProvider = object : JavaObjectMappingProvider {
            override fun findExtraMethods(
                clazz: Class<*>,
                map: MutableMap<MethodSignature, RenameableMethod>,
                includeProtected: Boolean,
                includePrivate: Boolean
            ) {
                var current: Class<*>? = clazz
                while (current != null) {
                    findRemappedMethods(current, map, includeProtected, includePrivate)
                    current = current.superclass
                }
            }

            override fun findExtraFields(
                clazz: Class<*>,
                list: MutableList<RenameableField>,
                includeProtected: Boolean,
                includePrivate: Boolean
            ) {
                var current: Class<*>? = clazz
                while (current != null) {
                    // TODO: Why do I need the null assertion here but not above? Compiler bug?
                    findRemappedFields(current!!, list, includeProtected, includePrivate)
                    current = current!!.superclass
                }
            }

            private fun findRemappedMethods(
                clazz: Class<*>,
                map: MutableMap<MethodSignature, RenameableMethod>,
                includeProtected: Boolean,
                includePrivate: Boolean,
            ) {
                val mappedClass = getMappedClassFromObject(clazz) ?: return

                for ((unmappedMethodName, mappedMethods) in mappedClass.methods) {
                    for (mappedMethod in mappedMethods) {
                        val method = clazz.methods.find {
                            when {
                                it.name != mappedMethod.name.value -> false
                                Modifier.isProtected(it.modifiers) && !includeProtected -> false
                                Modifier.isPrivate(it.modifiers) && !includePrivate -> false
                                else -> true
                            }
                        } ?: continue

                        map[MethodSignature(unmappedMethodName, method.parameterTypes)] = RenameableMethod(method, unmappedMethodName)
                    }
                }
            }

            private fun findRemappedFields(
                clazz: Class<*>,
                list: MutableList<RenameableField>,
                includeProtected: Boolean,
                includePrivate: Boolean
            ) {
                val mappedClass = getMappedClassFromObject(clazz) ?: return

                for ((unmappedFieldName, mappedField) in mappedClass.fields) {
                    val field = clazz.fields.find {
                        when {
                            it.name != mappedField.name.value -> false
                            Modifier.isProtected(it.modifiers) && !includeProtected -> false
                            Modifier.isPrivate(it.modifiers) && !includePrivate -> false
                            else -> true
                        }
                    } ?: continue

                    list.add(RenameableField(field, unmappedFieldName))
                }
            }

            private fun getMappedClassFromObject(clazz: Class<*>): MappedClass? {
                val unmappedClassName = mappedToUnmappedClassNames[clazz.name.replace('.', '/')] ?: return null
                return getMappedClass(unmappedClassName)
            }
        }
    }

    fun getMappedClass(name: String) = unmappedClasses[name]

    data class Mapping(val original: String, val mapped: String) {
        val value: String
            get() = if (isDevelopment) original else mapped

        companion object {
            fun fromMapped(mapped: Mapped) = Mapping(mapped.unmappedName, mapped.mappedName)
        }
    }

    data class MappedField(val name: Mapping, val type: Mapping)

    class MappedMethod(
        val name: Mapping,
        val parameterTypes: List<Mapping>,
        val returnType: Mapping,
    )

    class MappedClass(
        val name: Mapping,
        val fields: Map<String, MappedField>,
        val methods: Map<String, List<MappedMethod>>,
    ) {
        fun findMethods(name: String, classNode: ClassNode): List<MappedMethod>? {
            if (name in methods)
                return methods[name]

            val unmappedSuperClass = mappedToUnmappedClassNames[classNode.superName]!!
            return unmappedClasses[unmappedSuperClass]?.findMethods(
                name,
                MixinService.getService().bytecodeProvider.getClassNode(classNode.superName)
            )
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
