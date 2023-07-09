package com.chattriggers.ctjs.commands

import com.chattriggers.ctjs.engine.js.JSLoader
import com.chattriggers.ctjs.minecraft.wrappers.Player
import com.chattriggers.ctjs.minecraft.wrappers.World
import com.chattriggers.ctjs.minecraft.wrappers.world.block.BlockPos
import com.mojang.brigadier.ImmutableStringReader
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.*
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import gg.essential.universal.wrappers.message.UTextComponent
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.block.pattern.CachedBlockPosition
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.command.argument.BlockPredicateArgumentType
import net.minecraft.command.argument.BlockStateArgument
import net.minecraft.command.argument.BlockStateArgumentType
import net.minecraft.command.argument.PosArgument
import net.minecraft.registry.BuiltinRegistries
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import org.mozilla.javascript.Function
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.WrappedException
import java.util.concurrent.CompletableFuture

/**
 * An alternative to the command register that allows full use of the
 * functionality provided by Brigadier.
 *
 * @see buildCommand
 */
object DynamicCommand : CommandCollection() {
    private var currentContext: CommandBuilder? = null

    @JvmStatic
    fun buildCommand(names: Any, builder: Function) {
        require(currentContext == null) { "Command.buildCommand() called while already building a command" }

        val nameSet = when (names) {
            is String -> setOf(names)
            is Array<*> -> names.mapTo(mutableSetOf()) {
                require(it is String) { "Command names list must contain strings" }
                it
            }
            is Collection<*> -> names.mapTo(mutableSetOf()) {
                require(it is String) { "Command names list must contain strings" }
                it
            }
            else -> error("Commands.register() expects a string or array of strings as its first argument")
        }

        val context = pushContext { JSLoader.invoke(builder, emptyArray()) }
        register(CommandImpl(nameSet, context))
    }

    @JvmStatic
    fun argument(name: String, type: ArgumentType<Any>, builder: Function) {
        requireNotNull(currentContext) { "Call to Commands.argument() outside of Commands.buildCommand()" }
        val nestedContext = pushContext { JSLoader.invoke(builder, emptyArray()) }
        currentContext!!.children.add(CommandBuilder.Argument(name, type, nestedContext))
    }

    @JvmStatic
    fun literal(name: String, builder: Function) {
        requireNotNull(currentContext) { "Call to Commands.literal() outside of Commands.buildCommand()" }
        val nestedContext = pushContext { JSLoader.invoke(builder, emptyArray()) }
        currentContext!!.children.add(CommandBuilder.Literal(name, nestedContext))
    }

    @JvmStatic
    fun exec(method: Function) {
        requireNotNull(currentContext) { "Call to Commands.argument() outside of Commands.buildCommand()" }
        require(currentContext!!.method == null) { "Duplicate call ot Commands.exec()" }
        currentContext!!.method = method
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

    @JvmStatic
    fun blockPos(): ArgumentType<PosArgument> {
        return wrapArgument(BlockPosArgumentType.blockPos(), ::PosArgumentWrapper)
    }

    @JvmStatic
    fun blockPredicate(): ArgumentType<BlockPredicateWrapper> {
        val registryAccess = CommandManager.createRegistryAccess(BuiltinRegistries.createWrapperLookup())
        val predicate = BlockPredicateArgumentType.blockPredicate(registryAccess)
        return wrapArgument(predicate, ::BlockPredicateWrapper)
    }

    @JvmStatic
    fun blockState(): ArgumentType<BlockStateArgumentWrapper> {
        val registryAccess = CommandManager.createRegistryAccess(BuiltinRegistries.createWrapperLookup())
        val predicate = BlockStateArgumentType.blockState(registryAccess)
        return wrapArgument(predicate, ::BlockStateArgumentWrapper)
    }

    @JvmStatic
    fun custom(obj: NativeObject): ArgumentType<Any> {
        val parse = obj["parse"] as? Function ?: error(
            "Object provided to Commands.custom() must contain a \"parse\" function"
        )

        val suggest = obj["suggest"]?.let {
            require(it is Function) { "A \"suggest\" key in a custom command argument type must be a Function" }
            it
        }

        val getExamples = obj["getExamples"]?.let {
            require(it is Function) { "A \"getExamples\" key in a custom command argument type must be a Function" }
            it
        }

        return object : ArgumentType<Any> {
            override fun parse(reader: StringReader?): Any? {
                return try {
                    JSLoader.invoke(parse, arrayOf(reader))
                } catch (e: WrappedException) {
                    throw e.wrappedException
                }
            }

            override fun <S : Any?> listSuggestions(
                context: CommandContext<S>?,
                builder: SuggestionsBuilder?
            ): CompletableFuture<Suggestions> {
                return if (suggest != null) {
                    @Suppress("UNCHECKED_CAST")
                    JSLoader.invoke(suggest, arrayOf(context, builder)) as CompletableFuture<Suggestions>
                } else super.listSuggestions(context, builder)
            }

            override fun getExamples(): MutableCollection<String> {
                return if (getExamples != null) {
                    @Suppress("UNCHECKED_CAST")
                    JSLoader.invoke(getExamples, emptyArray()) as MutableCollection<String>
                } else super.getExamples()
            }

            override fun toString() = obj.toString()
        }
    }

    @JvmStatic
    fun error(reader: ImmutableStringReader, message: String): Nothing {
        throw SimpleCommandExceptionType(UTextComponent(message)).createWithContext(reader)
    }

    @JvmStatic
    fun error(reader: ImmutableStringReader, message: UTextComponent): Nothing =
        throw SimpleCommandExceptionType(message).createWithContext(reader)

    private fun getMockCommandSource(): ServerCommandSource {
        return ServerCommandSource(
            Player.toMC(),
            Player.getPos().toVec3d(),
            Player.getRotation(),
            null,
            0,
            Player.getName(),
            Player.getDisplayName(),
            null,
            Player.toMC(),
        )
    }

    private fun <T, U> wrapArgument(base: ArgumentType<T>, block: (T) -> U): ArgumentType<U> {
        return object : ArgumentType<U> {
            override fun parse(reader: StringReader): U = block(base.parse(reader))

            override fun <S : Any?> listSuggestions(
                context: CommandContext<S>,
                builder: SuggestionsBuilder,
            ) = base.listSuggestions(context, builder)

            override fun getExamples() = base.examples

            override fun toString() = base.toString()
        }
    }

    data class PosArgumentWrapper(val impl: PosArgument) : PosArgument by impl {
        fun toAbsolutePos(): Vec3d = impl.toAbsolutePos(getMockCommandSource())

        fun toAbsoluteBlockPos(): BlockPos = BlockPos(impl.toAbsoluteBlockPos(getMockCommandSource()))

        fun toAbsoluteRotation(): Vec2f = impl.toAbsoluteRotation(getMockCommandSource())

        override fun toString() = "PosArgument"
    }

    data class BlockPredicateWrapper(val impl: BlockPredicateArgumentType.BlockPredicate) {
        fun test(blockPos: BlockPos): Boolean {
            return impl.test(CachedBlockPosition(World.toMC(), blockPos.toMC(), true))
        }

        override fun toString() = "BlockPredicateArgument"
    }

    data class BlockStateArgumentWrapper(val impl: BlockStateArgument) {
        fun test(blockPos: BlockPos): Boolean =
            impl.test(CachedBlockPosition(World.toMC(), blockPos.toMC(), true))

        override fun toString() = "BlockStateArgument"
    }

    private fun pushContext(block: () -> Unit): CommandBuilder {
        val newContext = CommandBuilder(currentContext)
        currentContext = newContext
        try {
            block()
        } finally {
            currentContext = newContext.parent
        }
        return newContext
    }

    class CommandBuilder(val parent: CommandBuilder?) {
        val children = mutableListOf<Child>()
        var method: Function? = null

        fun allArguments() = generateSequence(this.parent) { it.parent }.flatMap {
            it.children.filterIsInstance<Argument>().asReversed()
        }.toList().asReversed()

        sealed class Child(val name: String, val nestedContext: CommandBuilder)

        class Argument(name: String, val type: ArgumentType<Any>, nestedContext: CommandBuilder) : Child(name, nestedContext)

        class Literal(name: String, nestedContext: CommandBuilder) : Child(name, nestedContext)
    }

    private class CommandImpl(private val names: Set<String>, private val commandBuilder: CommandBuilder) : Command {
        override val overrideExisting = false

        override fun registerImpl(): Command.Registration {
            return Command.Registration(names) {
                val node = LiteralArgumentBuilder.literal<FabricClientCommandSource>(it)
                applyCommandNode(node, commandBuilder)
                node
            }
        }

        private fun applyCommandNode(base: ArgumentBuilder<FabricClientCommandSource, *>, commandBuilder: CommandBuilder) {
            for (child in commandBuilder.children) {
                val newBuilder = when (child) {
                    is CommandBuilder.Literal -> literal(child.name)
                    is CommandBuilder.Argument -> argument(child.name, child.type)
                }
                applyCommandNode(newBuilder, child.nestedContext)
                base.then(newBuilder)
            }

            if (commandBuilder.method != null) {
                val args = commandBuilder.allArguments()
                base.executes { ctx ->
                    val argValues = Array(args.size) { i -> ctx.getArgument(args[i].name, Any::class.java) }
                    JSLoader.invoke(commandBuilder.method!!, argValues)
                    1
                }
            }
        }
    }
}
