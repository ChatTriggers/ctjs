package com.chattriggers.ctjs.internal.launch

import com.chattriggers.ctjs.api.Mappings
import com.chattriggers.ctjs.internal.launch.generation.Utils
import org.objectweb.asm.Type

sealed interface Descriptor {
    val isType get() = true

    fun originalDescriptor(): String
    fun mappedDescriptor(): String

    fun toType(): Type
    fun toMappedType(): Type

    enum class Primitive(private val type: Type) : Descriptor {
        VOID(Type.VOID_TYPE),
        BOOLEAN(Type.BOOLEAN_TYPE),
        CHAR(Type.CHAR_TYPE),
        BYTE(Type.BYTE_TYPE),
        SHORT(Type.SHORT_TYPE),
        INT(Type.INT_TYPE),
        FLOAT(Type.FLOAT_TYPE),
        LONG(Type.LONG_TYPE),
        DOUBLE(Type.DOUBLE_TYPE);

        override fun originalDescriptor(): String = type.descriptor
        override fun mappedDescriptor() = originalDescriptor()
        override fun toType() = type
        override fun toMappedType() = type

        override fun toString() = originalDescriptor()

        companion object {
            val IDENTIFIERS = entries.map { it.toString() }
        }
    }

    class Object(private val descriptor: String) : Descriptor {
        init {
            require(descriptor !in Primitive.IDENTIFIERS) {
                "Cannot pass a primitive type to Descriptor.Object"
            }
            require(!descriptor.startsWith('[')) {
                "Cannot pass an array type to Descriptor.Object"
            }
            require('(' !in descriptor && ')' !in descriptor) {
                "Cannot pass a method descriptor to Descriptor.Object"
            }
            require(':' !in descriptor) {
                "Cannot pass a field descriptor to Descriptor.Object"
            }
        }

        override fun originalDescriptor() = descriptor

        override fun mappedDescriptor(): String {
            // Do not map this class if it is not in a mapped package
            for (mappedPackage in Mappings.mappedPackages) {
                if (descriptor.startsWith(mappedPackage)) {
                    return Mappings.getMappedClassName(descriptor)?.let {
                        "L$it;"
                    } ?: error("Unknown class \"$descriptor\"")
                }
            }

            return descriptor
        }

        override fun toType(): Type = Type.getType(originalDescriptor())

        override fun toMappedType(): Type = Type.getType(mappedDescriptor())

        override fun toString() = originalDescriptor()
    }

    data class Array(val base: Descriptor, val dimensions: Int) : Descriptor {
        init {
            require(base.isType) {
                "Cannot pass a non-type object base to Descriptor.Array"
            }
            require(dimensions > 0) {
                "Cannot pass a dimensions count less than 1 to Descritor.Array"
            }
        }

        override fun originalDescriptor() = "[".repeat(dimensions) + base.originalDescriptor()

        override fun mappedDescriptor() = "[".repeat(dimensions) + base.mappedDescriptor()

        override fun toType(): Type = Type.getType(originalDescriptor())

        override fun toMappedType(): Type = Type.getType(mappedDescriptor())

        override fun toString() = originalDescriptor()
    }

    data class Field(val owner: Object?, val name: String, val type: Descriptor?) : Descriptor {
        override val isType get() = false
        private val mappedName by lazy {
            // Default to 'name' in case this field isn't mapped. If not, the Mixin will just fail to apply
            Mappings.getMappedClass(owner!!.originalDescriptor())?.fields?.get(name)?.name?.value ?: name
        }

        init {
            require(type?.isType != false) {
                "Cannot use non-type descriptor $type as the field type"
            }
        }

        override fun originalDescriptor() = buildString {
            if (owner != null)
                append(owner.originalDescriptor())
            append(name)
            append(':')
            if (type != null)
                append(type.originalDescriptor())
        }

        override fun mappedDescriptor() = buildString {
            require(owner != null) {
                "Cannot build mapped field descriptor from incomplete field descriptor"
            }
            append(owner.mappedDescriptor())
            append(mappedName)
            append(':')
            if (type != null)
                append(type.mappedDescriptor())
        }

        override fun toType() = error("Cannot convert Field descriptor to Type")

        override fun toMappedType() = error("Cannot convert Field descriptor to Type")

        override fun toString() = originalDescriptor()
    }

    data class Method(
        val owner: Object?,
        val name: String,
        val parameters: List<Descriptor>?,
        val returnType: Descriptor?,
    ) : Descriptor {
        override val isType get() = false
        private val mappedName by lazy {
            // Default to 'name' in case this field isn't mapped. If not, the Mixin will just fail to apply
            Mappings.getMappedClass(owner!!.originalDescriptor())
                ?.let { Utils.findMethod(it, this) }?.first?.name?.value
                ?: name
        }

        init {
            parameters?.forEach {
                require(it.isType) {
                    "Cannot use non-type descriptor $it as a method parameter"
                }
            }
            require(returnType?.isType != false) {
                "Cannot use non-type descriptor $returnType as the method return type"
            }

            require((parameters == null) == (returnType == null)) {
                "Parameters and return type must both be specified or omitted in a method descriptor"
            }
        }

        override fun originalDescriptor() = buildString {
            if (owner != null)
                append(owner.originalDescriptor())
            append(name)
            if (parameters != null) {
                append('(')
                parameters.forEach { append(it.originalDescriptor()) }
                append(')')
                append(returnType!!.originalDescriptor())
            }
        }

        override fun mappedDescriptor() = buildString {
            require(owner != null) {
                "Cannot build mapped method descriptor from incomplete method descriptor"
            }
            append(owner.mappedDescriptor())
            append(mappedName)
            if (parameters != null) {
                append('(')
                parameters.forEach { append(it.mappedDescriptor()) }
                append(')')
                append(returnType!!.mappedDescriptor())
            }
        }

        override fun toType(): Type = Type.getMethodType(originalDescriptor())

        override fun toMappedType(): Type = Type.getMethodType(mappedDescriptor())

        override fun toString() = originalDescriptor()
    }

    data class New(val type: Descriptor, val parameters: List<Descriptor>?) : Descriptor {
        override val isType get() = false

        init {
            // TODO: What would `new int[]{...}` look like here?
            require(type is Object) {
                "New descriptor cannot use non-object descriptor as its return type"
            }
            parameters?.forEach {
                require(it.isType) {
                    "Cannot use non-type descriptor $it as a new descriptor parameter"
                }
            }
        }

        override fun originalDescriptor() = buildString {
            if (parameters != null) {
                append('(')
                parameters.forEach { append(it.originalDescriptor()) }
                append(')')
            }
            append(type.originalDescriptor())
        }

        override fun mappedDescriptor() = buildString {
            if (parameters != null) {
                append('(')
                parameters.forEach { append(it.mappedDescriptor()) }
                append(')')
            }
            append(type.mappedDescriptor())
        }

        override fun toType() = error("Cannot convert New descriptor to Type")

        override fun toMappedType() = error("Cannot convert New descriptor to Type")

        override fun toString() = originalDescriptor()
    }

    class Parser(private val text: String) {
        private var cursor = 0
        private val ch get() = text[cursor]
        private val done get() = cursor > text.lastIndex

        init {
            require(text.isNotEmpty()) { "Invalid descriptor \"$text\"" }
        }

        private fun parseJavaIdentifier(): String? {
            if (done)
                return null

            listOf("<init>", "<clinit>").forEach {
                if (text.substring(cursor).startsWith(it)) {
                    cursor += it.length
                    return it
                }
            }

            if (!ch.isJavaIdentifierStart())
                return null

            val start = cursor
            cursor++
            while (!done && ch.isJavaIdentifierPart())
                cursor++
            return text.substring(start, cursor)
        }

        fun parseType(full: Boolean = false): Descriptor {
            if (full)
                check(cursor == 0)

            val descriptor = if (ch == 'L') {
                val semicolonIndex = text.indexOf(';', cursor)
                if (semicolonIndex == -1)
                    error("Invalid descriptor \"$text\": expected ';' after position $cursor")
                val objectType = text.substring(cursor, semicolonIndex + 1)
                cursor += objectType.length
                Object(objectType)
            } else if (ch in "VZCBSIFJD") {
                val primitive = Primitive.entries.first { it.toString() == ch.toString() }
                cursor++
                primitive
            } else if (ch == '[') {
                cursor++
                val base = parseType()
                if (base is Array) {
                    Array(base.base, base.dimensions + 1)
                } else Array(base, 1)
            } else {
                throw IllegalArgumentException("Invalid descriptor \"$text\": unexpected character at position $cursor")
            }

            if (full)
                require(done) { "Invalid descriptor: \"$text\"" }

            return descriptor
        }

        fun parseField(full: Boolean): Field {
            check(cursor == 0)

            val owner = try {
                val type = parseType()
                require(type is Object) {
                    "Cannot create field descriptor with a non-object owner"
                }
                type
            } catch (e: IllegalArgumentException) {
                if (full)
                    throw e
                null
            }

            val name = parseJavaIdentifier()
            requireNotNull(name) { "Invalid field descriptor: \"$text\"" }

            val type = if (!done && ch == ':') {
                cursor++
                parseType()
            } else null

            if (full)
                requireNotNull(type) { "Invalid field descriptor: \"$text\"" }

            require(done) { "Invalid field descriptor: \"$text\"" }
            return Field(owner, name, type)
        }

        fun parseMethod(full: Boolean): Method {
            check(cursor == 0)

            val owner = try {
                val type = parseType()
                require(type is Object) {
                    "Cannot create method descriptor with a non-object owner"
                }
                type
            } catch (e: IllegalArgumentException) {
                if (full)
                    throw e
                null
            }

            val name = parseJavaIdentifier()
            requireNotNull(name) { "Invalid method descriptor: \"$text\"" }

            val parameters = parseParameters()
            if (parameters == null && full)
                throw IllegalArgumentException("Expected full method descriptor, found \"$text\"")

            val returnType = if (parameters != null) {
                parseType()
            } else null

            require(done) { "Invalid method descriptor: \"$text\"" }

            return Method(owner, name, parameters, returnType)
        }

        fun parseNew(full: Boolean): New {
            check(cursor == 0)

            val parameters = parseParameters()
            if (parameters == null && full)
                throw IllegalArgumentException("Expected full new descriptor, found \"$text\"")

            val type = parseType()

            require(done) { "Invalid new descriptor: \"$text\"" }
            return New(type, parameters)
        }

        private fun parseParameters(): List<Descriptor>? {
            return if (!done && ch == '(') {
                cursor++
                val parameters = mutableListOf<Descriptor>()
                while (!done && ch != ')')
                    parameters.add(parseType())
                require(!done && ch == ')') { "Invalid method descriptor: \"$text\"" }
                cursor++
                parameters
            } else null
        }
    }
}
