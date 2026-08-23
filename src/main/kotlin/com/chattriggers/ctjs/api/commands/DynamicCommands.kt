package com.chattriggers.ctjs.api.commands

import com.chattriggers.ctjs.api.client.Player
import com.chattriggers.ctjs.api.commands.DynamicCommands.argument
import com.chattriggers.ctjs.api.commands.DynamicCommands.buildCommand
import com.chattriggers.ctjs.api.commands.DynamicCommands.custom
import com.chattriggers.ctjs.api.commands.DynamicCommands.literal
import com.chattriggers.ctjs.api.commands.DynamicCommands.message
import com.chattriggers.ctjs.api.commands.DynamicCommands.redirect
import com.chattriggers.ctjs.api.entity.Entity
import com.chattriggers.ctjs.api.entity.PlayerMP
import com.chattriggers.ctjs.api.inventory.Item
import com.chattriggers.ctjs.api.inventory.ItemType
import com.chattriggers.ctjs.api.inventory.nbt.NBTBase
import com.chattriggers.ctjs.api.inventory.nbt.NBTTagCompound
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.api.world.World
import com.chattriggers.ctjs.api.world.block.BlockFace
import com.chattriggers.ctjs.api.world.block.BlockPos
import com.chattriggers.ctjs.internal.commands.CommandCollection
import com.chattriggers.ctjs.internal.commands.DynamicCommand
import com.chattriggers.ctjs.internal.engine.JSLoader
import com.chattriggers.ctjs.internal.mixins.commands.EntitySelectorAccessor
import com.chattriggers.ctjs.MCEntity
import com.chattriggers.ctjs.MCNbtCompound
import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.internal.utils.asMixin
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.ImmutableStringReader
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.*
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.brigadier.tree.CommandNode
import net.minecraft.world.level.block.state.pattern.BlockInWorld
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.selector.EntitySelector
import net.minecraft.commands.arguments.*
import net.minecraft.commands.arguments.blocks.*
import net.minecraft.commands.arguments.coordinates.*
import net.minecraft.commands.arguments.item.*
import net.minecraft.commands.arguments.AngleArgument.SingleAngle
import net.minecraft.world.entity.player.Player as MCPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.data.registries.VanillaRegistries
import net.minecraft.commands.Commands
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.permissions.PermissionSet
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.mozilla.javascript.Function
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.WrappedException
import java.util.concurrent.CompletableFuture
import java.util.function.Predicate
import kotlin.math.min

/**
 * An alternative to the command register that allows full use of the
 * functionality provided by Brigadier.
 *
 * For more information about Brigadier, see
 * <a href="https://github.com/Mojang/brigadier">their GitHub page.</a>
 * Also see [CTCommand] for an example Brigadier command.
 *
 * ## General
 *
 * This API works similarly to Brigadier, however much of the annoyance
 * of using the Brigadier API has been eliminated, mainly the excessive
 * use of nested function calls. It works via a global context, so function
 * calls are free. However, this means that multiples commands cannot be
 * built at once. This means that commands should only ever be built on the
 * main thread. If two commands are built at the same time, an error will be
 * thrown.
 *
 * ## Argument Types
 *
 * The [ArgumentType] interface is a fundamental part of Brigadier, and
 * most of the MC argument types have been exposed via helper function
 * in this class. It is also possible to build new instances of
 * [ArgumentType] via [custom].
 *
 * When possible, the argument types returned from the helper function on
 * this class resolve in a way that their Minecraft variants do. For example,
 * the [message] type will replace selectors with their target entity, if
 * possible.
 *
 * ## Basic Example
 *
 * Here is an example command that recreates the `/advancement` command
 * (without any of the actual functionality, of course):
 *
 * ```js
 * // The `Commands` object supports destructuring, which makes assembling long
 * // commands much nicer
 * const { argument, choices, exec, greedyString, literal, registerCommand, resource, players } = Commands;
 *
 * registerCommand('ctadvancement', () => {
 *     // Note the use of choices to avoid having to copy-paste two separate literal() trees
 *     argument('kind', choices('grant', 'revoke'), () => {
 *         argument('targets', players(), () => {
 *             literal('everything', () => {
 *                 // exec() receives a single object with all of the arguments, which means we can
 *                 // destructure it to pull out the ones we want. Only values from argument() calls
 *                 // are included here; the literal nodes are ignored and have no impact on this object.
 *                 exec(({ kind, targets }) => {
 *                     ChatLib.chat(`${kind} everything from ${targets}`);
 *                 });
 *             });
 *
 *             literal('only', () => {
 *                 argument('advancement', resource(), () => {
 *                     argument('criterion', greedyString(), () => {
 *                         exec(({ kind, targets, advancement, criterion }) => {
 *                             ChatLib.chat(`${kind} only ${advancement} applied to ${targets} (criterion = ${criterion})`);
 *                         });
 *                     });
 *                 });
 *             });
 *
 *             argument('subkind', choices('from', 'through', 'until'), () => {
 *                 argument('advancement', resource(), () => {
 *                     exec(({ kind, subkind, targets, advancement }) => {
 *                         ChatLib.chat(`kind = ${kind}, subkind = ${subkind}, advancement = ${advancement}, targets = ${targets}`);
 *                     });
 *                 });
 *             });
 *         });
 *     });
 * });
 * ```
 *
 * ## Redirect
 *
 * Like Brigadier, this API supports assembling partial command nodes for use
 * in redirection. To do this, use [buildCommand], which returns the command node
 * (well, an internal representation of it). This object can then be passed to
 * further calls to [redirect] inside of a [literal] or [argument] block.
 *
 * Examples:
 *
 * ```js
 * // destructuring omitted
 *
 * const testCmdNode = buildCommand('testcmd', () => {
 *     exec(({ arg }) => {
 *         if (arg) {
 *             ChatLib.chat(`arg supplied, value = ${arg}`);
 *         } else {
 *             ChatLib.chat('no arg supplied');
 *         }
 *     });
 * });
 *
 * // Manually register it since we used buildCommand() instead of registerCommand()
 * testCmdNode.register()
 *
 * registerCommand('testcmd', () => {
 *     argument('arg', greedyString(), () => {
 *         redirect(testCmdNode);
 *     });
 * });
 * ```
 */
object DynamicCommands : CommandCollection() {
    private var currentNode: DynamicCommand.Node? = null

    ////////////////////
    // Tree Functions //
    ////////////////////

    @JvmStatic
    @JvmOverloads
    fun registerCommand(name: String, builder: Function? = null) = buildCommand(name, builder).also {
        it.register()
    }

    @JvmStatic
    @JvmOverloads
    fun buildCommand(name: String, builder: Function? = null): RootCommand {
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
    fun redirect(node: RootCommand) {
        requireNotNull(currentNode) { "Call to Commands.redirect() outside of Commands.buildCommand()" }
        require(!currentNode!!.hasRedirect) { "Duplicate call to Commands.redirect()" }
        currentNode!!.children.add(DynamicCommand.Node.Redirect(currentNode, node as DynamicCommand.Node.Root))
        currentNode!!.hasRedirect = true
    }

    @JvmStatic
    fun redirect(node: CommandNode<SharedSuggestionProvider>) {
        requireNotNull(currentNode) { "Call to Commands.redirect() outside of Commands.buildCommand()" }
        require(!currentNode!!.hasRedirect) { "Duplicate call to Commands.redirect()" }
        currentNode!!.children.add(DynamicCommand.Node.RedirectToCommandNode(currentNode, node))
        currentNode!!.hasRedirect = true
    }

    @JvmStatic
    fun exec(method: Function) {
        requireNotNull(currentNode) { "Call to Commands.argument() outside of Commands.buildCommand()" }
        require(!currentNode!!.hasRedirect) { "Cannot execute node with children" }
        require(currentNode!!.method == null) { "Duplicate call to Commands.exec()" }
        currentNode!!.method = method
    }

    /**
     * A helper method for getting Fabric's client CommandDispatcher root node. This allows user
     * commands to be redirected to the root node in the same way that "/execute run ..." does.
     *
     * As the result is a CommandNode, `.getChild(name)` can be used to access sub-command nodes
     * to, for example, redirect to just `/advancement` instead of `/`.
     */
    @JvmStatic
    fun getDispatcherRoot() = Client.getConnection()?.commands?.root

    /////////////////////////
    // Brigadier Arg Types //
    /////////////////////////

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#brigadier:bool">Argument Types: bool</a>
     */
    @JvmStatic
    fun bool(): BoolArgumentType = BoolArgumentType.bool()

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#brigadier:double">brigadier:double</a>
     */
    @JvmStatic
    @JvmOverloads
    fun double(min: Double = Double.MIN_VALUE, max: Double = Double.MAX_VALUE): DoubleArgumentType =
        DoubleArgumentType.doubleArg(min, max)

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#brigadier:float">brigadier:float</a>
     */
    @JvmStatic
    @JvmOverloads
    fun float(min: Float = Float.MIN_VALUE, max: Float = Float.MAX_VALUE): FloatArgumentType =
        FloatArgumentType.floatArg(min, max)

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#brigadier:integer">brigadier:integer</a>
     */
    @JvmStatic
    @JvmOverloads
    fun integer(min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): IntegerArgumentType =
        IntegerArgumentType.integer(min, max)

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#brigadier:long">brigadier:long</a>
     */
    @JvmStatic
    @JvmOverloads
    fun long(min: Long = Long.MIN_VALUE, max: Long = Long.MAX_VALUE): LongArgumentType =
        LongArgumentType.longArg(min, max)

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#brigadier:string">brigadier:string</a>
     */
    @JvmStatic
    fun string(): StringArgumentType = StringArgumentType.string()

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#brigadier:string">brigadier:string</a>
     */
    @JvmStatic
    fun greedyString(): StringArgumentType = StringArgumentType.greedyString()

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#brigadier:string">brigadier:string</a>
     */
    @JvmStatic
    fun word(): StringArgumentType = StringArgumentType.word()

    //////////////////
    // MC Arg Types //
    //////////////////

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:angle">minecraft:angle</a>
     */
    @JvmStatic
    fun angle() = wrapArgument(AngleArgument.angle(), ::AngleArgumentWrapper)

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:block_pos">minecraft:block_pos</a>
     */
    @JvmStatic
    fun blockPos(): ArgumentType<Coordinates> {
        return wrapArgument(BlockPosArgument.blockPos(), ::PosArgumentWrapper)
    }

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:block_predicate">minecraft:block_predicate</a>
     */
    @JvmStatic
    fun blockPredicate(): ArgumentType<BlockPredicateWrapper> {
        val registryAccess = Commands.createValidationContext(VanillaRegistries.createLookup())
        val predicate = BlockPredicateArgument.blockPredicate(registryAccess)
        return wrapArgument(predicate, ::BlockPredicateWrapper)
    }

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:block_state">minecraft:block_state</a>
     */
    @JvmStatic
    fun blockState(): ArgumentType<BlockStateArgumentWrapper> {
        val registryAccess = Commands.createValidationContext(VanillaRegistries.createLookup())
        val predicate = BlockStateArgument.block(registryAccess)
        return wrapArgument(predicate, ::BlockStateArgumentWrapper)
    }

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:color">minecraft:color</a>
     */
    @JvmStatic
    fun color() = ColorArgument.color()

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:column_pos">minecraft:column_pos</a>
     */
    @JvmStatic
    fun columnPos() = wrapArgument(ColumnPosArgument.columnPos(), ::PosArgumentWrapper)

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:dimension">minecraft:dimension</a>
     */
    @JvmStatic
    fun dimension() = wrapArgument(
        choices(
            "minecraft:overworld",
            "minecraft:the_nether",
            "minecraft:the_end",
            "minecraft:overworld_caves",
        )
    ) { name ->
        Entity.DimensionType.entries.first { it.toMC().identifier().toString() == name }
    }

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:entity">minecraft:entity</a>
     */
    @JvmStatic
    fun entity() = wrapArgument(EntityArgument.entity()) { EntitySelectorWrapper(it).getEntity() }

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:entity">minecraft:entity</a>
     */
    @JvmStatic
    fun entities() = wrapArgument(EntityArgument.entities()) { EntitySelectorWrapper(it).getEntities() }

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:float_range">minecraft:float_range</a>
     */
    @JvmStatic
    fun floatRange() = RangeArgument.Floats()

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:game_profile">minecraft:game_profile</a>
     */
    @JvmStatic
    fun gameProfile() = players()

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:game_profile">minecraft:game_profile</a>
     */
    @JvmStatic
    fun player() = wrapArgument(EntityArgument.player()) {
        EntitySelectorWrapper(it).getPlayers().let { players ->
            when {
                players.isEmpty() -> throw EntityArgument.NO_PLAYERS_FOUND.create()
                players.size > 1 -> throw EntityArgument.ERROR_NOT_SINGLE_PLAYER.create()
                else -> players[0]
            }
        }
    }

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:game_profile">minecraft:game_profile</a>
     */
    @JvmStatic
    fun players() = wrapArgument(EntityArgument.players()) { EntitySelectorWrapper(it).getPlayers() }

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:gamemode">minecraft:gamemode</a>
     */
    @JvmStatic
    fun gameMode() = GameModeArgument.gameMode()

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:int_range">minecraft:int_range</a>
     */
    @JvmStatic
    fun intRange() = RangeArgument.Ints()

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:item_predicate">minecraft:item_predicate</a>
     */
    @JvmStatic
    fun itemPredicate(): ArgumentType<(Item) -> Boolean> {
        val registryAccess = Commands.createValidationContext(VanillaRegistries.createLookup())
        val predicate = ItemPredicateArgument.itemPredicate(registryAccess)
        return wrapArgument(predicate) { pred -> { pred.test(it.mcValue) } }
    }

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:item_slot">minecraft:item_slot</a>
     */
    @JvmStatic
    fun itemSlot() = SlotArgument.slot()

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:item_stack">minecraft:item_stack</a>
     */
    @JvmStatic
    fun itemStack(): ArgumentType<ItemStackArgumentWrapper> {
        val registryAccess = Commands.createValidationContext(VanillaRegistries.createLookup())
        val arg = ItemArgument.item(registryAccess)
        return wrapArgument(arg, ::ItemStackArgumentWrapper)
    }

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:message">minecraft:message</a>
     */
    @JvmStatic
    fun message() = wrapArgument(MessageArgument.message(), ::MessageFormatArgumentWrapper)

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:nbt_compound_tag">minecraft:nbt_compound_tag</a>
     */
    @JvmStatic
    fun nbtCompoundTag() = wrapArgument(CompoundTagArgument.compoundTag(), ::NBTTagCompound)

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:nbt_path">minecraft:nbt_path</a>
     */
    @JvmStatic
    fun nbtPath() = wrapArgument(NbtPathArgument.nbtPath(), ::NbtPathWrapper)

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:nbt_tag">minecraft:nbt_tag</a>
     */
    @JvmStatic
    fun nbtTag() = wrapArgument(NbtTagArgument.nbtTag(), NBTBase::fromMC)

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:resource">minecraft:resource</a>
     */
    @JvmStatic
    fun resource() = IdentifierArgument.id()

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:rotation">minecraft:rotation</a>
     */
    @JvmStatic
    fun rotation() = wrapArgument(RotationArgument.rotation(), ::PosArgumentWrapper)

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:swizzle">minecraft:swizzle</a>
     */
    @JvmStatic
    fun swizzle() = wrapArgument(SwizzleArgument.swizzle()) { it.map(BlockFace.Axis::fromMC) }

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:time">minecraft:time</a>
     */
    @JvmStatic
    @JvmOverloads
    fun time(minimum: Int = 0) = TimeArgument.time(minimum)

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:uuid">minecraft:uuid</a>
     */
    @JvmStatic
    fun uuid() = UuidArgument.uuid()

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:vec2">minecraft:vec2</a>
     */
    @JvmStatic
    @JvmOverloads
    fun vec2(centerIntegers: Boolean = true) =
        wrapArgument(Vec2Argument.vec2(centerIntegers), ::PosArgumentWrapper)

    /**
     * @see <a href="https://minecraft.wiki/w/Argument_types#minecraft:vec3">minecraft:vec3</a>
     */
    @JvmStatic
    @JvmOverloads
    fun vec3(centerIntegers: Boolean = true) =
        wrapArgument(Vec3Argument.vec3(centerIntegers), ::PosArgumentWrapper)

    /**
     * Allows choosing from a set list of strings. When suggested to the user, this
     * will look as though this argument is multiple "literal()" nodes.
     */
    @JvmStatic
    fun choices(vararg options: String): ArgumentType<String> {
        require(options.isNotEmpty()) {
            "No strings passed to Commands.choices()"
        }
        require(options.all { CommandDispatcher.ARGUMENT_SEPARATOR_CHAR !in it }) {
            "Commands.choices() cannot accept strings with spaces"
        }
        require(options.none(String::isEmpty)) {
            "Commands.choices() cannot accept empty strings"
        }

        return object : ArgumentType<String> {
            override fun parse(reader: StringReader): String {
                val start = reader.cursor
                val optionChars = options.toMutableList()

                var offset = 0
                while (reader.canRead()) {
                    val ch = reader.read()
                    optionChars.removeIf { it[offset] != ch }
                    if (optionChars.isEmpty())
                        reader.fail(start)
                    offset += 1

                    val found = optionChars.find { it.length == offset }
                    if (found != null)
                        return found
                }

                reader.fail(start)
            }

            override fun <S : Any?> listSuggestions(
                context: CommandContext<S>,
                builder: SuggestionsBuilder
            ): CompletableFuture<Suggestions> {
                options.forEach(builder::suggest)
                return builder.buildFuture()
            }

            override fun getExamples(): MutableCollection<String> = options.toMutableList()

            private fun StringReader.fail(originalOffset: Int): Nothing {
                cursor = originalOffset
                error(this, "Expected one of: ${options.joinToString(", ")}")
            }
        }
    }

    /**
     * Allows easy creation of a custom ArgumentType without needing to use
     * JavaAdapter. Example:
     *
     * ```js
     * const HEADS = 0;
     * const TAILS = 1;
     *
     * const coinFlipArgType = Commands.custom({
     *     parse(reader) {
     *         // `reader` is a com.mojang.brigadier.StringReader
     *
     *         const savedCursor = reader.getCursor();
     *         const str = reader.readString();
     *         if (str === 'heads')
     *             return HEADS;
     *         if (str === 'tails')
     *             return TAILS;
     *         Commands.error(reader, `Expected one of: 'heads', 'tails'`);
     *     },
     *     suggest(ctx, builder) {
     *         // ctx is a com.mojang.brigadier.context.CommandContext<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>
     *         // builder is a com.mojang.brigadier.suggestion.SuggestionsBuilder
     *         builder.suggest('heads');
     *         builder.suggest('tails');
     *         return builder.buildFuture();
     *     },
     *     getExamples() {
     *         return ['heads', 'tails'];
     *     }
     * });
     * ```
     *
     * @see StringReader
     * @see CommandContext
     * @see SuggestionsBuilder
     */
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

    /**
     * Throw a detailed error given the reader, meant to be used with [custom]
     */
    @JvmStatic
    fun error(reader: ImmutableStringReader, message: String): Nothing {
        throw SimpleCommandExceptionType(TextComponent(message)).createWithContext(reader)
    }

    /**
     * Throw a detailed error given the reader, meant to be used with [custom]
     */
    @JvmStatic
    fun error(reader: ImmutableStringReader, message: TextComponent): Nothing =
        throw SimpleCommandExceptionType(message).createWithContext(reader)

    private fun getMockCommandSource(): CommandSourceStack {
        var source = Commands.createCompilationContext(PermissionSet.NO_PERMISSIONS)
            .withPosition(Player.getPos().toVec3d())
            .withRotation(Player.getRotation())
        Player.toMC()?.let { source = source.withEntity(it) }
        return source
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

    data class AngleArgumentWrapper(val angle: SingleAngle) {
        @JvmOverloads
        fun getAngle(entity: Entity = Player.asPlayerMP()!!) = angle.getAngle(
            getMockCommandSource().withRotation(entity.getRotation())
        )
    }

    data class PosArgumentWrapper(val impl: Coordinates) : Coordinates by impl {
        fun toAbsolutePos(): Vec3 = impl.getPosition(getMockCommandSource())

        fun toAbsoluteBlockPos(): BlockPos = BlockPos(impl.getBlockPos(getMockCommandSource()))

        fun toAbsoluteRotation(): Vec2 = impl.getRotation(getMockCommandSource())

        override fun toString() = "Coordinates"
    }

    data class BlockPredicateWrapper(val impl: BlockPredicateArgument.Result) {
        fun test(blockPos: BlockPos): Boolean {
            return impl.test(BlockInWorld(World.toMC() ?: return false, blockPos.toMC(), true))
        }

        override fun toString() = "BlockPredicateArgument"
    }

    data class BlockStateArgumentWrapper(val impl: BlockInput) {
        fun test(blockPos: BlockPos): Boolean =
            World.toMC()?.let { impl.test(BlockInWorld(it, blockPos.toMC(), true)) } ?: false

        override fun toString() = "BlockStateArgument"
    }

    class EntitySelectorWrapper(private val impl: EntitySelector) {
        private val mixed get() = impl.asMixin<EntitySelectorAccessor>()

        fun getEntity(): Entity {
            val entities = getEntities()
            return when {
                entities.isEmpty() -> throw EntityArgument.NO_ENTITIES_FOUND.create()
                entities.size > 1 -> throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create()
                else -> entities[0]
            }
        }

        fun getEntities(): List<Entity> {
            return getUnfilteredEntities().filter {
                it.toMC().type.isEnabled(World.toMC()!!.enabledFeatures())
            }
        }

        private fun getUnfilteredEntities(): List<Entity> {
            if (!mixed.includesNonPlayers)
                return getPlayers()

            if (mixed.playerName != null) {
                val entity = World.getAllEntitiesOfType(Player::class.java).find {
                    it.getName() == mixed.playerName
                }
                return listOfNotNull(entity)
            }

            if (mixed.uuid != null) {
                val entity = World.getAllEntitiesOfType(Player::class.java).find {
                    it.getUUID() == mixed.uuid
                }
                return listOfNotNull(entity)
            }

            val position = mixed.positionOffset.apply(Player.getPos().toVec3d())
            val predicate = getPositionPredicate(position)
            if (mixed.senderOnly) {
                if (predicate.test(Player.toMC()!!))
                    return listOf(Player.asPlayerMP()!!)
                return emptyList()
            }

            val entities = mutableListOf<MCEntity>()
            appendEntitiesFromWorld(entities, position, predicate)
            return getEntities(position, entities).map(Entity::fromMC)
        }

        fun getPlayers(): List<PlayerMP> {
            if (mixed.playerName != null) {
                val entity = World.getAllEntitiesOfType(Player::class.java).find {
                    it.getName() == mixed.playerName
                }
                @Suppress("UNCHECKED_CAST")
                return listOfNotNull(entity) as List<PlayerMP>
            }

            if (mixed.uuid != null) {
                val entity = World.getAllEntitiesOfType(Player::class.java).find {
                    it.getUUID() == mixed.uuid
                }
                @Suppress("UNCHECKED_CAST")
                return listOfNotNull(entity) as List<PlayerMP>
            }

            val position = mixed.positionOffset.apply(Player.getPos().toVec3d())
            val predicate = getPositionPredicate(position)
            if (mixed.senderOnly) {
                if (predicate.test(Player.toMC()!!))
                    return listOf(Player.asPlayerMP()!!)
                return emptyList()
            }

            val limit = if (mixed.sorter == EntitySelector.ORDER_ARBITRARY) mixed.limit else Int.MAX_VALUE
            val players = World.toMC()!!.players().filter(predicate::test).take(limit).toMutableList()
            return getEntities(position, players).map { PlayerMP(it as MCPlayer) }
        }

        private fun <T : MCEntity> getEntities(pos: Vec3, entities: MutableList<T>): List<T> {
            if (entities.size > 1)
                mixed.sorter.accept(pos, entities)
            return entities.subList(0, min(mixed.limit, entities.size))
        }

        private fun appendEntitiesFromWorld(
            entities: MutableList<MCEntity>,
            pos: Vec3,
            predicate: Predicate<MCEntity>
        ) {
            val limit = if (mixed.sorter == EntitySelector.ORDER_ARBITRARY) mixed.limit else Int.MAX_VALUE
            if (entities.size >= limit)
                return

            val min = pos.add(Vec3(-1000.0, -1000.0, -1000.0))
            val max = pos.add(Vec3(1000.0, 1000.0, 1000.0))
            val box = mixed.box?.move(pos) ?: AABB(min, max)
            World.toMC()!!.getEntities(mixed.entityFilter, box, predicate, entities, limit)
        }

        private fun getPositionPredicate(pos: Vec3): Predicate<MCEntity> {
            var predicate = mixed.predicates.reduceOrNull { acc, predicate -> acc.and(predicate) } ?: Predicate { true }
            if (mixed.box != null) {
                val box = mixed.box!!.move(pos)
                predicate = predicate.and { box.intersects(it.boundingBox) }
            }
            mixed.distance?.takeUnless { it.isAny }?.let { distance ->
                predicate = predicate.and { distance.matchesSqr(it.distanceToSqr(pos)) }
            }
            return predicate
        }
    }

    data class ItemStackArgumentWrapper(private val impl: ItemInput) : Predicate<Item> {
        val itemType = ItemType(impl.item.value())

        override fun test(item: Item) = ItemStack.isSameItemSameComponents(itemType.asItem().toMC(), item.toMC())

        fun test(type: ItemType) = itemType.getRegistryName() == type.getRegistryName()
    }

    data class MessageFormatArgumentWrapper(private val impl: MessageArgument.Message) {
        var text = impl.text

        fun format(): TextComponent {
            if (impl.parts.isEmpty())
                return TextComponent(text)

            var component = TextComponent(text.substring(0, impl.parts[0].start))
            var i = impl.parts[0].start

            for (part in impl.parts) {
                val entities = EntitySelectorWrapper(part.selector).getEntities()
                val nameComponent = EntitySelector.joinNames(entities.map(Entity::toMC))
                if (i < part.start)
                    component = component.withText(text.substring(i, part.start))

                component = component.withText(nameComponent)

                i = part.end
            }

            if (i < text.length)
                component = component.withText(text.drop(i))

            return component
        }

        override fun toString() = text
    }

    data class NbtPathWrapper(private val impl: NbtPathArgument.NbtPath) {
        fun get(nbt: NBTBase) = impl.get(nbt.toMC())
        fun count(nbt: NBTBase) = impl.countMatching(nbt.toMC())
        fun getOrInit(nbt: NBTBase, supplier: () -> NBTBase) = impl.getOrCreate(nbt.toMC()) { supplier().toMC() }
        fun put(nbt: NBTBase, source: NBTBase) = impl.set(nbt.toMC(), source.toMC())
        fun insert(index: Int, compound: NBTTagCompound, elements: List<NBTBase>) =
            impl.insert(index, compound.toMC() as MCNbtCompound, elements.map(NBTBase::toMC))

        fun remove(element: NBTBase) = impl.remove(element.toMC())

        override fun toString() = impl.toString()
    }

    private fun processNode(node: DynamicCommand.Node, builder: Function) {
        currentNode = node
        try {
            JSLoader.invoke(builder, emptyArray())
        } finally {
            currentNode = node.parent
        }
    }
}
