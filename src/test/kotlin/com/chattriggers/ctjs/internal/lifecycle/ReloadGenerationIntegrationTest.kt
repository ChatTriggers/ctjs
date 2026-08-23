package com.chattriggers.ctjs.internal.lifecycle

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReloadGenerationIntegrationTest {
    @Test
    fun `reload unload order stops generation before detaching owned state`() {
        val companion = classNode("com.chattriggers.ctjs.CTJS\$Companion")
        val unload = companion.method("unloadOnClientThread")

        val worldUnload = unload.fieldIndex(TRIGGER_TYPE, "WORLD_UNLOAD")
        val gameUnload = unload.fieldIndex(TRIGGER_TYPE, "GAME_UNLOAD")
        val stopGeneration = unload.invokeIndex(RUNTIME_GENERATIONS, "stopCurrent")
        val teardown = unload.invokeIndex(MODULE_MANAGER, "teardown")
        val clearTabList = unload.invokeIndex(TAB_LIST, "clearCustom\$ctjs")

        assertTrue(worldUnload < stopGeneration)
        assertTrue(gameUnload < stopGeneration)
        assertTrue(stopGeneration < teardown)
        assertTrue(teardown < clearTabList)
        assertEquals(1, unload.invocations(TAB_LIST, "clearCustom\$ctjs"))
        assertEquals(1, unload.invocations(CLIENT_LISTENER, "pruneInvalidatedTasks\$ctjs"))
    }

    @Test
    fun `new generation publishes before module entry and load triggers`() {
        val companion = classNode("com.chattriggers.ctjs.CTJS\$Companion")
        val publishMethod = companion.methods.single { it.invocations(RUNTIME_GENERATIONS, "publishNext") == 1 }

        val publish = publishMethod.invokeIndex(RUNTIME_GENERATIONS, "publishNext")
        val entry = publishMethod.invokeIndex(MODULE_MANAGER, "entryPass\$default")
            .takeIf { it >= 0 } ?: publishMethod.invokeIndex(MODULE_MANAGER, "entryPass")
        val gameLoad = publishMethod.fieldIndex(TRIGGER_TYPE, "GAME_LOAD")
        val worldLoad = publishMethod.fieldIndex(TRIGGER_TYPE, "WORLD_LOAD")

        assertTrue(publish < entry)
        assertTrue(entry < gameLoad)
        assertTrue(gameLoad < worldLoad)
        assertEquals(1, publishMethod.invocations(RUNTIME_GENERATIONS, "publishNext"))
    }

    @Test
    fun `client public and system task paths select different owners`() {
        val client = classNode("com.chattriggers.ctjs.api.client.Client")
        assertEquals(
            1,
            client.methods
                .filter { it.name == "scheduleTask" }
                .sumOf { it.invocations(RUNTIME_GENERATIONS, "currentOwner") },
        )
        assertEquals(
            1,
            client.methods
                .filter { it.name == "scheduleSystemTask\$ctjs" }
                .sumOf { it.invocations(RUNTIME_GENERATIONS, "systemOwner") },
        )
    }

    @Test
    fun `resources and ui contain generation invalidation boundaries`() {
        val image = classNode("com.chattriggers.ctjs.api.render.Image")
        assertEquals(1, image.method("destroy").invocations(GENERATION_GUARD, "close"))
        assertEquals(1, image.method("destroyInternal").invocations(TEXTURE_MANAGER, "release"))

        val sound = classNode("com.chattriggers.ctjs.api.client.Sound")
        assertEquals(1, sound.method("destroy").invocations(GENERATION_GUARD, "close"))
        assertEquals(0, sound.method("destroyInternal").invocations(SOUND, "bootstrap"))

        val gui = classNode("com.chattriggers.ctjs.api.render.Gui")
        assertEquals(1, gui.method("invalidateGeneration").invocations(MINECRAFT, "setScreen"))

        val toast = classNode("com.chattriggers.ctjs.api.render.Toast")
        assertTrue(toast.method("invalidateGeneration").fieldWrites("customRenderFunction") >= 1)
        assertTrue(toast.method("invalidateGeneration").fieldWrites("jsReceiver") >= 1)
    }

    @Test
    fun `tab list custom names retain a server value restoration path`() {
        val name = classNode("com.chattriggers.ctjs.api.world.TabList\$Name")
        assertTrue(name.fields.any { it.name == "originalName" })
        val restore = name.methods.single { it.name.startsWith("restoreOriginalName") }
        assertEquals(
            1,
            restore.invocations(PLAYER_INFO, "setTabListDisplayName"),
        )
    }

    private fun classNode(binaryName: String): ClassNode {
        val resource = binaryName.replace('.', '/') + ".class"
        val stream = checkNotNull(javaClass.classLoader.getResourceAsStream(resource)) { "Missing $resource" }
        return stream.use { ClassNode().also { node -> ClassReader(it).accept(node, 0) } }
    }

    private fun ClassNode.method(name: String): MethodNode = methods.single { it.name == name }

    private fun MethodNode.invocations(owner: String, name: String): Int =
        instructions.iterator().asSequence().filterIsInstance<MethodInsnNode>().count { it.owner == owner && it.name == name }

    private fun MethodNode.invokeIndex(owner: String, name: String): Int =
        instructions.iterator().asSequence().withIndex().firstOrNull {
            val instruction = it.value
            instruction is MethodInsnNode && instruction.owner == owner && instruction.name == name
        }?.index ?: -1

    private fun MethodNode.fieldIndex(owner: String, name: String): Int =
        instructions.iterator().asSequence().withIndex().firstOrNull {
            val instruction = it.value
            instruction is FieldInsnNode && instruction.owner == owner && instruction.name == name
        }?.index ?: -1

    private fun MethodNode.fieldWrites(name: String): Int =
        instructions.iterator().asSequence().filterIsInstance<FieldInsnNode>().count { it.name == name }

    private companion object {
        const val TRIGGER_TYPE = "com/chattriggers/ctjs/api/triggers/TriggerType"
        const val RUNTIME_GENERATIONS = "com/chattriggers/ctjs/internal/lifecycle/RuntimeGenerations"
        const val GENERATION_GUARD = "com/chattriggers/ctjs/internal/lifecycle/GenerationGuard"
        const val MODULE_MANAGER = "com/chattriggers/ctjs/internal/engine/module/ModuleManager"
        const val CLIENT_LISTENER = "com/chattriggers/ctjs/internal/listeners/ClientListener"
        const val TAB_LIST = "com/chattriggers/ctjs/api/world/TabList"
        const val TEXTURE_MANAGER = "net/minecraft/client/renderer/texture/TextureManager"
        const val MINECRAFT = "net/minecraft/client/Minecraft"
        const val PLAYER_INFO = "net/minecraft/client/multiplayer/PlayerInfo"
        const val SOUND = "com/chattriggers/ctjs/api/client/Sound"
    }
}
