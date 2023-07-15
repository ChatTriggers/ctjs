package com.chattriggers.ctjs.commands

import com.chattriggers.ctjs.engine.js.JSLoader
import com.mojang.brigadier.arguments.*
import org.mozilla.javascript.Function

/**
 * An alternative to the command register that allows full use of the
 * functionality provided by Brigadier.
 */
object DynamicCommands : CommandCollection() {
    private var currentNode: DynamicCommand.Node? = null

    @JvmStatic
    @JvmOverloads
    fun registerCommand(name: String, builder: Function? = null) = buildCommand(name, builder).also {
        it.register()
    }

    @JvmStatic
    @JvmOverloads
    fun buildCommand(name: String, builder: Function? = null): DynamicCommand.Node.Root {
        require(currentNode == null) { "Command.buildCommand() called while already building a command" }
        val node = DynamicCommand.Node.Root(name)
        if (builder != null)
            processNode(node, builder)
        return node
    }

    @JvmStatic
    fun argument(name: String, type: ArgumentType<Any>, builder: Function) {
        requireNotNull(currentNode) { "Call to Commands.argument() outside of Commands.buildCommand()" }
        require(!currentNode!!.hasRedirect) { "Cannot redirect node with children" }
        val node = DynamicCommand.Node.Argument(currentNode, name, type)
        processNode(node, builder)
        currentNode!!.children.add(node)
    }

    @JvmStatic
    fun literal(name: String, builder: Function) {
        requireNotNull(currentNode) { "Call to Commands.literal() outside of Commands.buildCommand()" }
        require(!currentNode!!.hasRedirect) { "Cannot redirect node with children" }
        val node = DynamicCommand.Node.Literal(currentNode, name)
        processNode(node, builder)
        currentNode!!.children.add(node)
    }

    @JvmStatic
    fun redirect(node: DynamicCommand.Node.Root) {
        requireNotNull(currentNode) { "Call to Commands.redirect() outside of Commands.buildCommand()" }
        require(!currentNode!!.hasRedirect) { "Duplicate call to Commands.redirect()" }
        currentNode!!.children.add(DynamicCommand.Node.Redirect(currentNode, node))
        currentNode!!.hasRedirect = true
    }

    @JvmStatic
    fun exec(method: Function) {
        requireNotNull(currentNode) { "Call to Commands.argument() outside of Commands.buildCommand()" }
        require(!currentNode!!.hasRedirect) { "Cannot execute node with children" }
        require(currentNode!!.method == null) { "Duplicate call to Commands.exec()" }
        currentNode!!.method = method
    }

    @JvmStatic
    fun boolean(): BoolArgumentType = BoolArgumentType.bool()

    @JvmStatic
    @JvmOverloads
    fun int(min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): IntegerArgumentType =
        IntegerArgumentType.integer(min, max)

    @JvmStatic
    @JvmOverloads
    fun long(min: Long = Long.MIN_VALUE, max: Long = Long.MAX_VALUE): LongArgumentType =
        LongArgumentType.longArg(min, max)

    @JvmStatic
    @JvmOverloads
    fun float(min: Float = Float.MIN_VALUE, max: Float = Float.MAX_VALUE): FloatArgumentType =
        FloatArgumentType.floatArg(min, max)

    @JvmStatic
    @JvmOverloads
    fun double(min: Double = Double.MIN_VALUE, max: Double = Double.MAX_VALUE): DoubleArgumentType =
        DoubleArgumentType.doubleArg(min, max)

    @JvmStatic
    fun string(): StringArgumentType = StringArgumentType.string()

    @JvmStatic
    fun greedyString(): StringArgumentType = StringArgumentType.greedyString()

    @JvmStatic
    fun word(): StringArgumentType = StringArgumentType.word()

    private fun processNode(node: DynamicCommand.Node, builder: Function) {
        currentNode = node
        try {
            JSLoader.invoke(builder, emptyArray())
        } finally {
            currentNode = node.parent
        }
    }
}
