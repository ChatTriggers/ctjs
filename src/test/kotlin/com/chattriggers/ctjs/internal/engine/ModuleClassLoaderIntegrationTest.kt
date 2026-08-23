package com.chattriggers.ctjs.internal.engine

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.ClassNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModuleClassLoaderIntegrationTest {
    @Test
    fun `client shutdown closes the active generation classloader`() {
        val ctjs = classNode("com.chattriggers.ctjs.CTJS")
        val shutdown = ctjs.methods.single {
            it.invocations(RUNTIME_GENERATIONS, "stopCurrent") == 1 &&
                it.fieldIndex(OWNED_KIND, "CLASS_LOADER") >= 0
        }

        val stop = shutdown.invokeIndex(RUNTIME_GENERATIONS, "stopCurrent")
        val classLoaderKind = shutdown.fieldIndex(OWNED_KIND, "CLASS_LOADER")
        val invalidate = shutdown.invokeIndex(GENERATION_SNAPSHOT, "invalidate")

        assertTrue(stop < classLoaderKind)
        assertTrue(classLoaderKind < invalidate)
    }

    @Test
    fun `reload detaches classloader before module teardown`() {
        val companion = classNode("com.chattriggers.ctjs.CTJS\$Companion")
        val unload = companion.methods.single { it.name == "unloadOnClientThread" }

        val classLoaderKind = unload.fieldIndex(OWNED_KIND, "CLASS_LOADER")
        val teardown = unload.invokeIndex(MODULE_MANAGER, "teardown")

        assertTrue(classLoaderKind >= 0)
        assertTrue(classLoaderKind < teardown)
    }

    @Test
    fun `new loader publishes after generation and before module entry`() {
        val companion = classNode("com.chattriggers.ctjs.CTJS\$Companion")
        val reload = companion.methods.single {
            it.invocations(RUNTIME_GENERATIONS, "publishNext") == 1 &&
                it.invocations(MODULE_MANAGER, "publishPrepared\$ctjs") == 1
        }

        val generation = reload.invokeIndex(RUNTIME_GENERATIONS, "publishNext")
        val loader = reload.invokeIndex(MODULE_MANAGER, "publishPrepared\$ctjs")
        val entry = reload.invokeIndex(MODULE_MANAGER, "entryPass\$default")
            .takeIf { it >= 0 } ?: reload.invokeIndex(MODULE_MANAGER, "entryPass")

        assertTrue(generation < loader)
        assertTrue(loader < entry)
    }

    @Test
    fun `bootstrap setup publishes generation loader before mixin discovery`() {
        val plugin = classNode("com.chattriggers.ctjs.internal.launch.CTMixinPlugin")
        val onLoad = plugin.methods.single { it.name == "onLoad" }

        val setup = onLoad.invokeIndex(MODULE_MANAGER, "setup")
        val publish = onLoad.invokeIndex(MODULE_MANAGER, "publishInitial")
        val mixins = onLoad.invokeIndex(MIXIN_EXTRAS, "init")

        assertTrue(setup < publish)
        assertTrue(publish < mixins)
    }

    @Test
    fun `module deletion never closes bootstrap or shared URL loader`() {
        val manager = classNode("com.chattriggers.ctjs.internal.engine.module.ModuleManager")
        val delete = manager.methods.single { it.name == "deleteModule" }

        assertEquals(0, delete.invocations("java/net/URLClassLoader", "close"))
        assertEquals(0, delete.invocations(JS_CONTEXT_FACTORY, "enterContext"))
        assertEquals(1, delete.invocations(CTJS_COMPANION, "load\$default"))
    }

    @Test
    fun `dynamic import extends only the active generation loader`() {
        val manager = classNode("com.chattriggers.ctjs.internal.engine.module.ModuleManager")
        val prepareImport = manager.methods.single { it.name.startsWith("prepareImport") }
        val activateImport = manager.methods.single { it.name.startsWith("activateImport") }

        assertEquals(1, prepareImport.invocations(JS_LOADER, "addGenerationJars\$ctjs"))
        assertEquals(0, prepareImport.invocations(JS_LOADER, "prepareGeneration\$ctjs"))
        assertEquals(1, activateImport.invocations(JS_LOADER, "isActiveLoader\$ctjs"))
    }

    private fun classNode(binaryName: String): ClassNode {
        val resource = binaryName.replace('.', '/') + ".class"
        val stream = checkNotNull(javaClass.classLoader.getResourceAsStream(resource)) { "Missing $resource" }
        return stream.use { ClassNode().also { node -> ClassReader(it).accept(node, 0) } }
    }

    private fun MethodNode.invocations(owner: String, name: String): Int =
        instructions.iterator().asSequence().filterIsInstance<MethodInsnNode>().count {
            it.owner == owner && it.name == name
        }

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

    private companion object {
        const val OWNED_KIND = "com/chattriggers/ctjs/internal/lifecycle/OwnedKind"
        const val RUNTIME_GENERATIONS = "com/chattriggers/ctjs/internal/lifecycle/RuntimeGenerations"
        const val GENERATION_SNAPSHOT = "com/chattriggers/ctjs/internal/lifecycle/GenerationSnapshot"
        const val MODULE_MANAGER = "com/chattriggers/ctjs/internal/engine/module/ModuleManager"
        const val JS_LOADER = "com/chattriggers/ctjs/internal/engine/JSLoader"
        const val JS_CONTEXT_FACTORY = "com/chattriggers/ctjs/internal/engine/JSContextFactory"
        const val MIXIN_EXTRAS = "com/llamalad7/mixinextras/MixinExtrasBootstrap"
        const val CTJS_COMPANION = "com/chattriggers/ctjs/CTJS\$Companion"
    }
}
