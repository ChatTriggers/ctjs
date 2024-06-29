package com.chattriggers.ctjs.internal.launch

import com.chattriggers.ctjs.api.Mappings
import org.mozilla.javascript.JavaObjectMappingProvider
import java.lang.reflect.Modifier

object CTJavaObjectMappingProvider : JavaObjectMappingProvider {
    override fun mapClassName(className: String) = Mappings.mapClassName(className)?.replace('/', '.')

    override fun unmapClassName(className: String) = Mappings.unmapClassName(className)?.replace('/', '.')

    override fun findExtraMethods(
        clazz: Class<*>,
        map: MutableMap<JavaObjectMappingProvider.MethodSignature, JavaObjectMappingProvider.RenameableMethod>,
        includeProtected: Boolean,
        includePrivate: Boolean
    ) {
        val queue = ArrayDeque<Class<*>>()
        var current: Class<*>? = clazz

        while (current != null) {
            findRemappedMethods(
                current,
                map,
                includeProtected,
                includePrivate
            )

            val superClass = current.superclass
            if (superClass != null)
                queue.add(superClass)

            for (itf in current.interfaces)
                queue.add(itf)

            current = queue.removeFirstOrNull()
        }
    }

    override fun findExtraFields(
        clazz: Class<*>,
        list: MutableList<JavaObjectMappingProvider.RenameableField>,
        includeProtected: Boolean,
        includePrivate: Boolean
    ) {
        var current: Class<*>? = clazz
        while (current != null) {
            findRemappedFields(
                current,
                list,
                includeProtected,
                includePrivate
            )
            current = current.superclass
        }
    }

    private fun findRemappedMethods(
        clazz: Class<*>,
        map: MutableMap<JavaObjectMappingProvider.MethodSignature, JavaObjectMappingProvider.RenameableMethod>,
        includeProtected: Boolean,
        includePrivate: Boolean,
    ) {
        val mappedClass = Mappings.getMappedClass(clazz.name) ?: return

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

                map[JavaObjectMappingProvider.MethodSignature(unmappedMethodName, method.parameterTypes)] =
                    JavaObjectMappingProvider.RenameableMethod(method, unmappedMethodName)
            }
        }
    }

    private fun findRemappedFields(
        clazz: Class<*>,
        list: MutableList<JavaObjectMappingProvider.RenameableField>,
        includeProtected: Boolean,
        includePrivate: Boolean
    ) {
        val mappedClass = Mappings.getMappedClass(clazz.name) ?: return

        for ((unmappedFieldName, mappedField) in mappedClass.fields) {
            val field = clazz.fields.find {
                when {
                    it.name != mappedField.name.value -> false
                    Modifier.isProtected(it.modifiers) && !includeProtected -> false
                    Modifier.isPrivate(it.modifiers) && !includePrivate -> false
                    else -> true
                }
            } ?: continue

            list.add(JavaObjectMappingProvider.RenameableField(field, unmappedFieldName))
        }
    }
}
