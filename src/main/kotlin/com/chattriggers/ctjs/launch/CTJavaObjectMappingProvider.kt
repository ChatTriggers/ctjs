package com.chattriggers.ctjs.launch

import com.chattriggers.ctjs.utils.Mappings
import org.mozilla.javascript.JavaObjectMappingProvider
import java.lang.reflect.Modifier

object CTJavaObjectMappingProvider : JavaObjectMappingProvider {
    override fun findExtraMethods(
        clazz: Class<*>,
        map: MutableMap<JavaObjectMappingProvider.MethodSignature, JavaObjectMappingProvider.RenameableMethod>,
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
        list: MutableList<JavaObjectMappingProvider.RenameableField>,
        includeProtected: Boolean,
        includePrivate: Boolean
    ) {
        var current: Class<*>? = clazz
        while (current != null) {
            findRemappedFields(current, list, includeProtected, includePrivate)
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
