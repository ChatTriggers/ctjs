package com.chattriggers.ctjs.internal.triggers

import com.chattriggers.ctjs.api.entity.Entity
import com.chattriggers.ctjs.api.triggers.TriggerType
import com.chattriggers.ctjs.api.world.block.BlockPos
import net.minecraft.core.BlockPos as MCBlockPos
import net.minecraft.world.entity.Entity as MCEntity
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TriggerContractIntegrationTest {
    @Test
    fun `wrapper and mutable-list adapters expose the required runtime classes`() {
        val blockPos = TriggerContractAdapters.blockPos(MCBlockPos(3, 4, 5))
        assertIs<BlockPos>(blockPos)
        assertEquals(listOf(3, 4, 5), listOf(blockPos.x, blockPos.y, blockPos.z))

        val lore = TriggerContractAdapters.mutableList(listOf("a", "b"))
        assertIs<ArrayList<*>>(lore)
        lore.add("c")
        lore[0] = "changed"
        lore.removeAt(1)
        assertEquals(listOf("changed", "c"), lore)

        val entityAdapter = TriggerContractAdapters::class.java.getMethod("entity", MCEntity::class.java)
        assertEquals(Entity::class.java, entityAdapter.returnType)
        assertFalse(TriggerContractAdapters.shouldTriggerGuiOpened(null))
    }

    @Test
    fun `normal disconnect and direct world replacement are exactly once`() {
        val disconnect =
            TriggerLifecycleTransitions.beforeSetLevel(wasLoaded = true, willBeLoaded = false) +
                TriggerLifecycleTransitions.afterSetLevel(willBeLoaded = false)
        assertEquals(
            listOf(
                TriggerLifecycleTransitions.Event.SERVER_DISCONNECT,
                TriggerLifecycleTransitions.Event.WORLD_UNLOAD,
            ),
            disconnect,
        )
        assertEquals(1, disconnect.count { it == TriggerLifecycleTransitions.Event.SERVER_DISCONNECT })
        assertEquals(1, disconnect.count { it == TriggerLifecycleTransitions.Event.WORLD_UNLOAD })

        val replacement =
            TriggerLifecycleTransitions.beforeSetLevel(wasLoaded = true, willBeLoaded = true) +
                TriggerLifecycleTransitions.afterSetLevel(willBeLoaded = true)
        assertEquals(
            listOf(
                TriggerLifecycleTransitions.Event.WORLD_UNLOAD,
                TriggerLifecycleTransitions.Event.WORLD_LOAD,
            ),
            replacement,
        )
        assertFalse(replacement.contains(TriggerLifecycleTransitions.Event.SERVER_DISCONNECT))
        assertFalse(replacement.contains(TriggerLifecycleTransitions.Event.SERVER_CONNECT))
    }

    @Test
    fun `connect and no-world transitions are explicit`() {
        val connect =
            TriggerLifecycleTransitions.beforeSetLevel(wasLoaded = false, willBeLoaded = true) +
                TriggerLifecycleTransitions.afterSetLevel(willBeLoaded = true)
        assertEquals(
            listOf(
                TriggerLifecycleTransitions.Event.SERVER_CONNECT,
                TriggerLifecycleTransitions.Event.WORLD_LOAD,
            ),
            connect,
        )
        assertTrue(TriggerLifecycleTransitions.beforeSetLevel(false, false).isEmpty())
        assertTrue(TriggerLifecycleTransitions.afterSetLevel(false).isEmpty())
    }

    @Test
    fun `entity death and block highlight invoke wrapper adapters once`() {
        val livingEntityMixin = classNode("com.chattriggers.ctjs.internal.mixins.LivingEntityMixin")
        val death = livingEntityMixin.method("chattriggers\$entityDeath")
        assertEquals(1, death.invocations(ADAPTERS, "entity"))
        assertEquals(1, death.triggerField(TriggerType.ENTITY_DEATH))
        assertEquals(1, death.invocations(TRIGGER_TYPE, "triggerAll"))

        val worldListener = classNode("com.chattriggers.ctjs.internal.listeners.WorldListener")
        val highlight = worldListener.methods.single { it.triggerField(TriggerType.BLOCK_HIGHLIGHT) == 1 }
        assertEquals(1, highlight.invocations(ADAPTERS, "blockPos"))
        assertEquals(1, highlight.invocations(TRIGGER_TYPE, "triggerAll"))
    }

    @Test
    fun `tooltip is mutable and cancellable at the vanilla extraction boundary`() {
        val handledScreenMixin = classNode("com.chattriggers.ctjs.internal.mixins.HandledScreenMixin")
        val tooltip = handledScreenMixin.method("injectDrawMouseoverTooltip")

        assertEquals(1, tooltip.invocations(ADAPTERS, "mutableList"))
        assertEquals(1, tooltip.triggerField(TriggerType.ITEM_TOOLTIP))
        assertEquals(1, tooltip.invocations(TRIGGER_TYPE, "triggerAll"))
        assertTrue(tooltip.injectAnnotation().booleanValue("cancellable"))
    }

    @Test
    fun `gui opened is non-null guarded cancellable and invoked once`() {
        val minecraftMixin = classNode("com.chattriggers.ctjs.internal.mixins.MinecraftClientMixin")
        val opened = minecraftMixin.method("injectScreenOpened")

        assertEquals(1, opened.invocations(ADAPTERS, "shouldTriggerGuiOpened"))
        assertEquals(1, opened.triggerField(TriggerType.GUI_OPENED))
        assertEquals(1, opened.invocations(TRIGGER_TYPE, "triggerAll"))
        assertTrue(opened.injectAnnotation().booleanValue("cancellable"))
    }

    @Test
    fun `world lifecycle has one mixin source and no disconnect injection`() {
        val minecraftMixin = classNode("com.chattriggers.ctjs.internal.mixins.MinecraftClientMixin")
        val unload = minecraftMixin.method("injectWorldUnload")
        val load = minecraftMixin.method("injectWorldLoad")

        assertEquals(1, unload.invocations(LIFECYCLE, "dispatchBeforeSetLevel"))
        assertEquals(1, load.invocations(LIFECYCLE, "dispatchAfterSetLevel"))
        assertTrue(
            minecraftMixin.methods
                .flatMap { it.visibleAnnotations.orEmpty() + it.invisibleAnnotations.orEmpty() }
                .none { annotation -> annotation.stringValues().any { "disconnect" in it } },
        )
    }

    @Test
    fun `ct reload synthetic world lifecycle stays separate and exactly once`() {
        val companion = classNode("com.chattriggers.ctjs.CTJS\$Companion")
        val syntheticUnload = companion.method("unloadOnClientThread")
        val syntheticLoad = companion.method("loadOnClientThread\$lambda\$0\$1")

        assertEquals(1, syntheticUnload.triggerField(TriggerType.WORLD_UNLOAD))
        assertEquals(1, syntheticLoad.triggerField(TriggerType.WORLD_LOAD))
        assertEquals(0, companion.methods.sumOf { it.triggerField(TriggerType.SERVER_DISCONNECT) })
        assertEquals(0, companion.methods.sumOf { it.triggerField(TriggerType.SERVER_CONNECT) })
    }

    @Test
    fun `drop container clear and BreakBlock cancellation reach vanilla result`() {
        val screenHandler = classNode("com.chattriggers.ctjs.internal.mixins.ScreenHandlerMixin")
        val clear = screenHandler.method("injectDropInventory")
        assertTrue(clear.injectAnnotation().booleanValue("cancellable"))
        assertEquals(1, clear.triggerField(TriggerType.DROP_ITEM))
        assertEquals(0, clear.instructions.asSequence().filterIsInstance<org.objectweb.asm.tree.TypeInsnNode>()
            .count { it.desc.endsWith("CancellableEvent") })

        val interaction = classNode("com.chattriggers.ctjs.internal.mixins.ClientPlayerInteractionManagerMixin")
        val breakBlock = interaction.method("injectBreakBlock")
        assertTrue(breakBlock.injectAnnotation().booleanValue("cancellable"))
        assertEquals("HEAD", breakBlock.injectAtValue())
        assertEquals(1, breakBlock.invocations("org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable", "setReturnValue"))
    }

    private fun classNode(binaryName: String): ClassNode {
        val resource = binaryName.replace('.', '/') + ".class"
        val stream = checkNotNull(javaClass.classLoader.getResourceAsStream(resource)) { "Missing class resource $resource" }
        return stream.use {
            ClassNode().also { node -> ClassReader(it).accept(node, 0) }
        }
    }

    private fun ClassNode.method(name: String): MethodNode =
        methods.single { it.name == name }

    private fun MethodNode.invocations(owner: String, name: String): Int =
        instructions.asSequence()
            .filterIsInstance<MethodInsnNode>()
            .count { it.owner == owner && it.name == name }

    private fun MethodNode.triggerField(type: TriggerType): Int =
        instructions.asSequence()
            .filterIsInstance<FieldInsnNode>()
            .count { it.owner == TRIGGER_TYPE && it.name == type.name }

    private fun MethodNode.injectAnnotation(): AnnotationNode =
        (visibleAnnotations.orEmpty() + invisibleAnnotations.orEmpty())
            .single { it.desc == INJECT_ANNOTATION }

    private fun MethodNode.injectAtValue(): String {
        val inject = injectAnnotation()
        val raw = inject.values.orEmpty().chunked(2).single { it[0] == "at" }[1]
        val at = when (raw) {
            is AnnotationNode -> raw
            is List<*> -> raw.single() as AnnotationNode
            else -> error("Unexpected @At value $raw")
        }
        return at.values.orEmpty().chunked(2).single { it[0] == "value" }[1] as String
    }

    private fun AnnotationNode.booleanValue(name: String): Boolean =
        values.orEmpty().chunked(2).single { it[0] == name }[1] as Boolean

    private fun AnnotationNode.stringValues(): List<String> =
        values.orEmpty().flatMap { value ->
            when (value) {
                is String -> listOf(value)
                is List<*> -> value.filterIsInstance<String>()
                else -> emptyList()
            }
        }

    private fun Iterable<AbstractInsnNode>.asSequence(): Sequence<AbstractInsnNode> = sequence {
        for (instruction in this@asSequence)
            yield(instruction)
    }

    private companion object {
        const val ADAPTERS = "com/chattriggers/ctjs/internal/triggers/TriggerContractAdapters"
        const val LIFECYCLE = "com/chattriggers/ctjs/internal/triggers/TriggerLifecycleTransitions"
        const val TRIGGER_TYPE = "com/chattriggers/ctjs/api/triggers/TriggerType"
        const val INJECT_ANNOTATION = "Lorg/spongepowered/asm/mixin/injection/Inject;"
    }
}
