package com.chattriggers.ctjs.typing

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*

class Provider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) = Processor(environment)
}

@OptIn(KspExperimental::class)
class Processor(environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator
    private val builder = StringBuilder()
    private val dependentFiles = mutableListOf<KSFile>()
    private val classNames = mutableSetOf<String>()
    private var indent = 0

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // 1. Get all root classes
        val rootClasses = collectRoots(resolver)

        // 2. Perform a depth-first traversal and collect all types that are within MAX_DEPTH "steps" of a root class
        val classes = mutableSetOf<KSClassDeclaration>()
        rootClasses.forEach { collectAllReachableClasses(it, classes, 0) }

        // 3. Transform all classes into a package-based tree structure
        val packages = transformToPackageTree(classes)

        // 4. Build the file's content
        build(packages, resolver)

        // 5. The output file in generated in finish()

        return emptyList()
    }

    private fun collectRoots(resolver: Resolver): Set<KSClassDeclaration> {
        val manualRootDeclarations = manualRoots
            .map(resolver::getKSNameFromString)
            .mapNotNull(resolver::getClassDeclarationByName)
            .toSet()

        return manualRootDeclarations + resolver.getAllFiles().flatMap {
            dependentFiles.add(it)

            it.declarations.filter { decl ->
                val qualifier = decl.packageName.asString()
                !qualifier.startsWith("com.chattriggers.ctjs.internal") &&
                    !qualifier.startsWith("com.chattriggers.ctjs.typing") &&
                    decl.isPublic()
            }
        }.filterIsInstance<KSClassDeclaration>().toSet()
    }

    private fun collectAllReachableClasses(decl: KSDeclaration, classes: MutableSet<KSClassDeclaration>, depth: Int) {
        if (depth > MAX_DEPTH || decl in classes || decl is KSTypeParameter || !decl.isPublic())
            return

        if (decl is KSTypeAlias) {
            collectAllReachableClasses(decl.type.resolve().declaration, classes, depth)
            return
        }

        if (decl !is KSClassDeclaration) {
            logger.error("Tried to collect declaration of type ${decl::class.simpleName}")
            return
        }

        classes.add(decl)

        decl.superTypes.forEach { collectAllReachableClasses(it.resolve().declaration, classes, depth) }

        decl.declarations.forEach {
            when (it) {
                is KSPropertyDeclaration -> {
                    if (!it.isPrivate() && !it.isJavaPackagePrivate())
                        collectAllReachableClasses(it.type.resolve().declaration, classes, depth + 1)
                }
                is KSFunctionDeclaration -> {
                    if (it.isPrivate() || it.isJavaPackagePrivate())
                        return@forEach

                    it.parameters.forEach { parameter ->
                        collectAllReachableClasses(parameter.type.resolve().declaration, classes, depth + 1)
                    }
                    if (it.returnType != null)
                        collectAllReachableClasses(it.returnType!!.resolve().declaration, classes, depth + 1)
                }
                is KSClassDeclaration -> collectAllReachableClasses(it, classes, depth + 1)
                else -> TODO("Handle declaration class ${decl::class.simpleName} in collectAllReachableClasses")
            }
        }
    }

    // Note: This returns a package that represents the top-level package, and is really only used as a container for
    //       other packages and declarations
    private fun transformToPackageTree(classes: Set<KSClassDeclaration>): Package {
        val root = Package(null, ROOT_PACKAGE)

        // Skip packages (and all classes) with invalid package names
        fun getPackage(pkg: String): Package? {
            return pkg.split(".").fold(root) { p, name ->
                if (name in typescriptReservedWords)
                    return null
                p.getPackage(name)
            }
        }

        classes.forEach {
            if (it.classKind == ClassKind.ENUM_ENTRY)
                return@forEach

            val pkg = getPackage(it.packageName.asString()) ?: return@forEach
            val name = it.name

            require(name !in pkg.classes)
            pkg.classes[name] = it

            classNames.add("${pkg.path()}.$name")
        }

        return root
    }

    private fun build(rootPackage: Package, resolver: Resolver) {
        append(prologue)
        buildPackage(rootPackage, resolver)
        appendLine('}')
    }

    private fun buildPackage(pkg: Package, resolver: Resolver) {
        val isRoot = pkg.name == ROOT_PACKAGE

        if (!isRoot) {
            appendLine("namespace ${pkg.name} {")
            indent++
        }

        pkg.subpackages.values.forEach { buildPackage(it, resolver) }
        pkg.classes.forEach { buildClass(it.key, it.value, resolver) }

        if (!isRoot) {
            indent--
            appendLine("}")
        }
    }

    private fun buildClass(name: String, clazz: KSClassDeclaration, resolver: Resolver) {
        // Note: We take a name parameter so that we can override the name of clazz. This is done for nested classes

        appendLine(buildString {
            if (clazz.classKind == ClassKind.INTERFACE || Modifier.ABSTRACT in clazz.modifiers)
                append("abstract ")
            append("class $name")

            if (clazz.typeParameters.isNotEmpty()) {
                append(clazz.typeParameters.joinToString(", ", "<", ">") {
                    it.name.asString()
                })
            }

            append(" { ")
        })

        indented {
            val (enumEntries, innerClasses) = clazz.declarations
                .filterIsInstance<KSClassDeclaration>()
                .partition { it.classKind == ClassKind.ENUM_ENTRY }

            enumEntries.forEach {
                if (it.docString != null)
                    append(formatDocString(it.docString!!))
                appendLine("static ${it.simpleName.asString()}: ${clazz.path};")
            }

            innerClasses.forEach {
                if (it.isPrivate() || it.isJavaPackagePrivate())
                    return@forEach

                if (it.docString != null)
                    append(formatDocString(it.docString!!))
                val path = it.path
                if (path in classNames) {
                    appendLine("static ${it.simpleName.asString()}: typeof $path;")
                } else {
                    appendLine("static ${it.simpleName.asString()}: unknown;")
                }
            }

            val properties = clazz.getDeclaredProperties().filter { it.isPublic() || it.isProtected() }.toList()
            val functions = clazz.getDeclaredFunctions().filter { it.isPublic() || it.isProtected() }.toList()

            // Unlike Java, JS does not allow properties and functions to have the same name,
            // so in the case that a pair does share the same name, we prefer the function
            val functionNames = functions.map { it.simpleName.asString() }

            for (property in properties) {
                if (property.simpleName.asString() in functionNames)
                    continue

                val isStatic = Modifier.JAVA_STATIC in property.modifiers ||
                    property.isAnnotationPresent(JvmStatic::class) ||
                    property.isAnnotationPresent(JvmField::class)

                if (property.docString != null)
                    append(formatDocString(property.docString!!))

                if (property.isAnnotationPresent(JvmField::class)) {
                    appendLine(buildString {
                        if (isStatic)
                            append("static ")
                        append(property.simpleName.asString())
                        append(": ")
                        append(buildType(property.type, resolver))
                        append(';')
                    })
                } else {
                    val getter = property.getter
                    val setter = property.setter

                    if (getter != null) {
                        appendLine(buildString {
                            if (isStatic)
                                append("static ")
                            append(resolver.getJvmName(getter)!!)
                            if (getter.returnType != null) {
                                append("(): ")
                                append(buildType(getter.returnType!!, resolver))
                                append(';')
                            } else {
                                append("();")
                            }
                        })
                    }

                    if (setter != null) {
                        appendLine(buildString {
                            if (isStatic)
                                append("static ")
                            append(resolver.getJvmName(setter)!!)
                            append("(value: ")
                            append(buildType(setter.parameter.type, resolver))
                            append(");")
                        })
                    }
                }
            }

            if (properties.isNotEmpty() && functions.isNotEmpty())
                append('\n')

            for (function in functions) {
                val parameterSets = if (function.isAnnotationPresent(JvmOverloads::class)) {
                    // Append Int.MAX_VALUE to ensure we get an overload that contains all default parameters
                    val defaultIndicesToStopAt = function.parameters.mapIndexedNotNull { index, parameter ->
                        if (parameter.hasDefault) index else null
                    } + Int.MAX_VALUE

                    defaultIndicesToStopAt.map { defaultIndexToStopAt ->
                        function.parameters.filterIndexed { index, parameter ->
                            !parameter.hasDefault || index < defaultIndexToStopAt
                        }
                    }
                } else listOf(function.parameters)

                for (parameters in parameterSets) {
                    if (function.docString != null)
                        append(formatDocString(function.docString!!))

                    val functionName = function.simpleName.asString()
                    if (functionName in excludedMethods || functionName in typescriptReservedWords)
                        continue

                    appendLine(buildString {
                        if (Modifier.JAVA_STATIC in function.modifiers || function.isAnnotationPresent(JvmStatic::class))
                            append("static ")

                        append(if (functionName == "<init>") "constructor" else functionName)

                        // Constructor functions will have the same type parameters as the class
                        if (functionName != "<init>" && function.typeParameters.isNotEmpty())
                            append(function.typeParameters.joinToString(", ", "<", ">") { it.name.asString() })

                        append('(')
                        append(parameters.joinToString {
                            "${it.name!!.asString().safeName()}: ${buildType(it.type, resolver)}"
                        })
                        append(')')

                        if (functionName != "<init>" && function.returnType != null) {
                            append(": ")
                            append(buildType(function.returnType!!, resolver))
                        }

                        append(';')
                    })
                }
            }
        }

        appendLine("}")
    }

    fun buildType(reference: KSTypeReference, resolver: Resolver): String {
        val type = reference.resolve()
        (type.declaration as? KSTypeParameter)?.let {
            return it.name.asString()
        }
        (type.declaration as? KSTypeAlias)?.let {
            return buildType((type.declaration as KSTypeAlias).type, resolver)
        }

        return buildString {
            val builtinType = when (type) {
                resolver.builtIns.anyType -> "any"
                resolver.builtIns.booleanType -> "boolean"
                resolver.builtIns.byteType,
                resolver.builtIns.charType,
                resolver.builtIns.doubleType,
                resolver.builtIns.floatType,
                resolver.builtIns.intType,
                resolver.builtIns.longType,
                resolver.builtIns.numberType,
                resolver.builtIns.shortType -> "number"
                resolver.builtIns.stringType -> "string"
                resolver.builtIns.unitType -> "void"
                resolver.builtIns.nothingType -> "never"
                else -> null
            }

            when {
                builtinType != null -> append(builtinType)
                type.declaration.qualifiedName?.asString()
                    ?.startsWith("kotlin.Function") == true && type.declaration is KSClassDeclaration -> {
                    var i = 0
                    append(reference.element!!.typeArguments.dropLast(1).joinToString(", ", "(", ")") { arg ->
                        // TODO: "unknown" instead of "any"?
                        val typeString = arg.type?.let { buildType(it, resolver) } ?: "any"
                        "p${i++}: $typeString"
                    })
                    append(" => ")
                    val returnType = reference.element?.typeArguments?.last()?.type?.let { buildType(it, resolver) }
                    append(returnType ?: "any")
                }
                type.declaration.isAnnotationPresent(java.lang.FunctionalInterface::class) -> {
                    append(buildFunctionalInterfaceType(reference, type, resolver))
                }
                type == resolver.getClassDeclarationByName(resolver.getKSNameFromString("org.mozilla.javascript.NativeObject")) -> {
                    append("object")
                }
                else -> {
                    val path = when (val path = type.declaration.path) {
                        "kotlin.Any" -> "any"
                        "kotlin.Nothing" -> "never"
                        "kotlin.Unit" -> "void"
                        "kotlin.Byte",
                        "kotlin.Char",
                        "kotlin.Short",
                        "kotlin.Int",
                        "kotlin.Long",
                        "kotlin.Float",
                        "kotlin.Double" -> "number"
                        "kotlin.Boolean" -> "boolean"
                        "kotlin.String" -> "string"
                        "kotlin.collections.Set",
                        "kotlin.collections.MutableSet" -> "Set"
                        "kotlin.ByteArray",
                        "kotlin.CharArray",
                        "kotlin.ShortArray",
                        "kotlin.IntArray",
                        "kotlin.LongArray" -> return "Array<number>"
                        "kotlin.Array",
                        "kotlin.collections.List",
                        "kotlin.collections.MutableList",
                        "kotlin.collections.Collection",
                        "kotlin.collections.MutableCollection" -> "Array"
                        "kotlin.collections.Map",
                        "kotlin.collections.MutableMap" -> "Map"
                        else -> path.takeIf { it in classNames }
                    } ?: return "unknown"

                    append(path)
                    val typeArgs = reference.element?.typeArguments ?: type.arguments
                    if (typeArgs.isNotEmpty()) {
                        append(typeArgs.joinToString(", ", "<", ">") { arg ->
                            if (arg.variance == Variance.STAR) {
                                // TODO: "unknown"?
                                "any"
                            } else {
                                buildType(arg.type!!, resolver)
                            }
                        })
                    }
                }
            }

            if (type.isMarkedNullable)
                append(" | null | undefined")
        }
    }

    private fun append(s: Any) = builder.append(s)

    private fun appendLine(s: Any) {
        repeat(indent) { builder.append("\t") }
        builder.append(s)
        builder.append('\n')
    }

    private fun formatDocString(str: String) = buildString {
        append("/**\n")
        str.trim().lines().forEach { append(" * $it\n") }
        append(" */")
    }.prependIndent("\t".repeat(indent)) + "\n"

    private fun indented(block: () -> Unit) {
        indent++
        block()
        indent--
        check(indent >= 0)
    }

    override fun finish() {
        check(indent == 0)

        codeGenerator
            .createNewFileByPath(Dependencies(true, *dependentFiles.toTypedArray()), "typings", "d.ts")
            .write(builder.toString().toByteArray())
    }

    private fun buildFunctionalInterfaceType(reference: KSTypeReference, type: KSType, resolver: Resolver): String {
        val genericTypes = mutableMapOf<String, String>()
        var typeArguments = reference.element?.typeArguments ?: type.arguments
        var typeParameters = type.declaration.typeParameters

        require(typeArguments.size == typeParameters.size)
        for (i in typeArguments.indices)
            genericTypes[typeParameters[i].toString()] = buildType(typeArguments[i].type!!, resolver)

        var method = getFunctionalInterfaceMethod(type.declaration as KSClassDeclaration)

        if (method == null) {
            for (superClass in (type.declaration as KSClassDeclaration).superTypes) {
                val declaration = superClass.resolve().declaration as KSClassDeclaration
                typeArguments = superClass.element?.typeArguments ?: type.arguments
                typeParameters = declaration.typeParameters

                require(typeArguments.size == typeParameters.size)
                for (i in typeArguments.indices)
                    genericTypes[typeParameters[i].toString()] =
                        buildType(typeArguments[i].type!!, resolver).let { genericTypes[it] ?: it }

                method = getFunctionalInterfaceMethod(declaration)
                if (method != null)
                    break
            }
        }

        if (method == null) {
            logger.error("Failed to find functional interface method for \"${type.declaration.qualifiedName!!.asString()}\"")
            return "(...args: Array<unknown>) => unknown"
        }

        return buildString {
            append('(')

            for ((index, parameter) in method.parameters.withIndex()) {
                if (index != 0)
                    append(", ")

                append("${parameter.name!!.asString().safeName()}: ")
                val typeStr = genericTypes[parameter.type.toString()]
                when {
                    typeStr != null -> append(typeStr)
                    else -> append(buildType(parameter.type, resolver))
                }
            }

            append(") => ")
            val typeStr = genericTypes[method.returnType.toString()]
            when {
                typeStr != null -> append(typeStr)
                method.returnType != null -> append(buildType(method.returnType!!, resolver))
                else -> append("void")
            }
        }
    }

    private fun getFunctionalInterfaceMethod(clazz: KSClassDeclaration): KSFunctionDeclaration? {
        return clazz.getDeclaredFunctions().firstOrNull { it.isPublic() && it.isAbstract }
    }

    private val classNameCache = mutableMapOf<KSClassDeclaration, String>()
    private val KSClassDeclaration.name: String
        get() = classNameCache.getOrPut(this) {
            val parent = parentDeclaration
            if (parent is KSClassDeclaration) {
                "${parent.name}$${simpleName.asString()}"
            } else simpleName.asString()
        }

    private val classPathCache = mutableMapOf<KSDeclaration, String>()
    private val KSDeclaration.path: String
        get() = classPathCache.getOrPut(this) {
            if (this is KSClassDeclaration) {
                val parent = parentDeclaration
                if (parent is KSClassDeclaration) {
                    // Omit the parent class from the path
                    return qualifiedName!!.getQualifier().substringBeforeLast('.') + ".$name"
                }
            }

            qualifiedName!!.asString()
        }

    private class Package(val parent: Package?, val name: String) {
        val subpackages = mutableMapOf<String, Package>()
        val classes = mutableMapOf<String, KSClassDeclaration>()

        fun getPackage(name: String): Package {
            return subpackages.getOrPut(name) { Package(this, name) }
        }

        fun path() = generateSequence(this, Package::parent)
            .toList()
            .asReversed()
            .drop(1) // Ignore the root package
            .joinToString(".", transform = Package::name)

        override fun toString() = "Package(${path()})"
    }

    companion object {
        private const val MAX_DEPTH = 1000
        private const val ROOT_PACKAGE = "#root"

        private val excludedMethods = setOf(
            "<clinit>", "equals", "hashCode", "toString", "finalize", "compareTo", "clone",
        )

        // Typescript keywords
        private val typescriptReservedWords = setOf(
            "break", "case", "catch", "class", "const", "constructor", "continue", "debugger", "default", "delete",
            "do", "else", "enum", "export", "extends", "false", "finally", "for", "function", "if", "import", "in",
            "instanceof", "new", "null", "return", "super", "switch", "this", "throw", "true", "try", "typeof", "var",
            "void", "while", "with",
        )

        private fun String.safeName() = this + if (this in typescriptReservedWords) "_" else ""
    }
}
