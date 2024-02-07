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
    private var indent = 1

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
        indent -= 1
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

        val functions = clazz.getDeclaredFunctions().filter {
            it.isPublic()
        }.filterNot {
            it.findOverridee() != null || it.simpleName.asString().let { name ->
                name in excludedMethods || name in typescriptReservedWords
            }
        }.toList()

        // Unlike Java, JS does not allow properties and functions to have the same name,
        // so in the case that a pair does share the same name, we prefer the function
        val functionNames = functions.map { it.simpleName.asString() }

        val properties = clazz.getDeclaredProperties().filter {
            it.isPublic()
        }.filterNot {
            it.simpleName.asString() in functionNames || it.findOverridee() != null
        }.toList()

        val (staticFunctions, instanceFunctions) = functions.partition { it.isStatic() }
        val (staticProperties, instanceProperties) = properties.partition { it.isStatic() }
        val isEnum = clazz.classKind == ClassKind.ENUM_CLASS

        val nestedClasses = clazz.declarations.filterIsInstance<KSClassDeclaration>().filter {
            it.isPublic()
        }.filter {
            it.classKind == ClassKind.ENUM_CLASS || it.classKind == ClassKind.CLASS
        }.toList()

        // Output static object first, if necessary
        if (staticProperties.isNotEmpty() || staticFunctions.isNotEmpty() || nestedClasses.isNotEmpty() || isEnum) {
            appendLine("const $name: {")
            indented {
                if (isEnum) {
                    clazz.declarations
                        .filterIsInstance<KSClassDeclaration>()
                        .filter { it.classKind == ClassKind.ENUM_ENTRY }
                        .forEach {
                            appendLine("${it.simpleName.asString()}: ${clazz.path};")
                        }
                }

                nestedClasses.forEach {
                    appendLine("${it.simpleName.asString()}: typeof ${it.path};")
                }

                staticProperties.forEach { buildProperty(it, resolver) }
                staticFunctions.forEach { buildFunction(it, resolver, omitName = false) }
            }
            appendLine("}")
        }

        // Then output the instance interface
        appendLine(buildString {
            append("interface ")
            append(name)

            if (clazz.typeParameters.isNotEmpty()) {
                append(clazz.typeParameters.joinToString(", ", "<", ">") {
                    it.name.asString()
                })
            }

            val (superInterfaces, superClasses) = clazz.superTypes
                .map {
                    var decl = it.resolve().declaration
                    while (decl is KSTypeAlias)
                        decl = decl.type.resolve().declaration
                    it to decl as KSClassDeclaration
                }
                .filter { it.first.resolve() !== resolver.builtIns.anyType }
                .partition { it.second.classKind == ClassKind.INTERFACE }

            val superMembers = if (superClasses.isNotEmpty()) {
                require(superClasses.size == 1)
                listOf(superClasses.single()) + superInterfaces
            } else superInterfaces

            if (superMembers.isNotEmpty()) {
                val clause = superMembers.map { (ref, _) ->
                    buildType(ref, resolver).let {
                        if (it == "number") "kotlin.Number" else it
                    }
                }.filter { it != "unknown" }.joinToString()
                if (clause.isNotBlank()) {
                    append(" extends ")
                    append(clause.trim())
                }
            }

            append(" { ")
        })

        indented {
            if (clazz.classKind == ClassKind.OBJECT) {
                staticProperties.forEach { buildProperty(it, resolver) }
                staticFunctions.forEach { buildFunction(it, resolver, omitName = false) }
            }
            instanceProperties.forEach { buildProperty(it, resolver) }
            instanceFunctions.forEach { buildFunction(it, resolver, omitName = false) }

            // If this is a functional interface, output a call method
            if (clazz.isAnnotationPresent(FunctionalInterface::class)) {
                val functionalMethod = getFunctionalInterfaceMethod(clazz)
                if (functionalMethod != null) {
                    buildFunction(functionalMethod, resolver, omitName = true)
                }
            } else if (clazz.path.startsWith("kotlin.Function") && clazz.path != "kotlin.Function") {
                val functionalMethod = clazz.getDeclaredFunctions().single()
                buildFunction(functionalMethod, resolver, omitName = true)
            }
        }

        appendLine("}")
    }

    private fun buildProperty(property: KSPropertyDeclaration, resolver: Resolver) {
        if (property.docString != null)
            append(formatDocString(property.docString!!))

        if (property.isAnnotationPresent(JvmField::class) || (property.getter == null && property.setter == null)) {
            appendLine(buildString {
                append(property.simpleName.asString())
                append(": ")
                append(buildType(property.type, resolver))
                append(';')
            })
        } else {
            val getter = property.getter
            val setter = property.setter

            if (getter != null && (getter.modifiers.isEmpty() || getter.modifiers.contains(Modifier.PUBLIC))) {
                appendLine(buildString {
                    append(resolver.getJvmName(getter)!!)
                    if (getter.returnType != null) {
                        append("(): ")
                        append(buildType(getter.returnType!!, resolver))
                        append(';')
                    } else {
                        append("(): void;")
                    }
                })
            }

            if (setter != null && (setter.modifiers.isEmpty() || setter.modifiers.contains(Modifier.PUBLIC))) {
                appendLine(buildString {
                    append(resolver.getJvmName(setter)!!)
                    append("(value: ")
                    append(buildType(setter.parameter.type, resolver))
                    append("): void;")
                })
            }
        }
    }

    private fun buildFunction(function: KSFunctionDeclaration, resolver: Resolver, omitName: Boolean) {
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

        val functionName = if (omitName) "" else function.simpleName.asString()

        for (parameters in parameterSets) {
            if (function.docString != null)
                append(formatDocString(function.docString!!))

            appendLine(buildString {
                append(if (functionName == "<init>") "new" else functionName)

                if (function.typeParameters.isNotEmpty())
                    append(function.typeParameters.joinToString(", ", "<", ">") { it.name.asString() })

                append('(')
                append(parameters.joinToString {
                    "${it.name!!.asString().safeName()}: ${buildType(it.type, resolver)}"
                })
                append(')')

                if (function.returnType != null) {
                    append(": ")
                    append(buildType(function.returnType!!, resolver))
                }

                append(';')
            })
        }
    }

    private fun buildType(reference: KSTypeReference, resolver: Resolver): String {
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
                type == resolver.getClassDeclarationByName(resolver.getKSNameFromString("org.mozilla.javascript.NativeObject")) ->
                    append("object")
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
        repeat(indent) { builder.append("  ") }
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

    fun KSPropertyDeclaration.isStatic() = Modifier.JAVA_STATIC in modifiers ||
        isAnnotationPresent(JvmStatic::class) ||
        isAnnotationPresent(JvmField::class)

    fun KSFunctionDeclaration.isStatic() = Modifier.JAVA_STATIC in modifiers ||
        isAnnotationPresent(JvmStatic::class) ||
        isConstructor()

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
