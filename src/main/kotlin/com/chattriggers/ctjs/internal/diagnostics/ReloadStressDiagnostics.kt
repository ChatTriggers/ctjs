package com.chattriggers.ctjs.internal.diagnostics

import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.client.KeyBind
import com.chattriggers.ctjs.api.commands.DynamicCommands
import com.chattriggers.ctjs.api.render.Gui
import com.chattriggers.ctjs.api.render.Renderer
import com.chattriggers.ctjs.api.render.Renderer3d
import com.chattriggers.ctjs.api.world.Scoreboard
import com.chattriggers.ctjs.api.world.TabList
import com.chattriggers.ctjs.api.world.World
import com.chattriggers.ctjs.engine.Register
import com.chattriggers.ctjs.internal.commands.StaticCommand
import com.chattriggers.ctjs.internal.engine.JSLoader
import com.chattriggers.ctjs.internal.lifecycle.OwnedKind
import com.chattriggers.ctjs.internal.lifecycle.RuntimeGenerations
import com.chattriggers.ctjs.internal.listeners.ClientListener
import java.nio.file.Files
import java.nio.file.Path

/**
 * Read-only release-gate telemetry for the reload stress fixture.
 *
 * This object deliberately owns no runtime state. It only snapshots the
 * generation and registries that already own CTJS work.
 */
internal object ReloadStressDiagnostics {
    private val classLoaderCache = Path.of("./config/ChatTriggers/classloader-cache")

    @JvmStatic
    fun snapshotJson(): String {
        val owner = RuntimeGenerations.currentOwner()
        val screen = Client.getMinecraft().screen
        return buildString {
            append('{')
            number("generationId", owner.generationId)
            string("generationState", owner.state.name)
            number("ownerHandles", owner.ownedCount())
            OwnedKind.entries.forEach { kind ->
                number("owner${kind.name.lowercase().replaceFirstChar(Char::uppercase)}Handles", owner.ownedCount(kind))
            }
            number("systemTrackedHandles", 0)
            number("taskQueue", ClientListener.pendingTaskCount())
            number("registeredTriggers", JSLoader.registeredTriggerCount())
            number("customTriggerTypes", Register.customTriggerCount())
            number("staticCommands", StaticCommand.registeredCount())
            number("dynamicCommands", DynamicCommands.registeredCount())
            number("keybinds", KeyBind.getKeyBinds().size)
            nullableNumber("activeLoaderGenerationId", JSLoader.activeLoaderGenerationId())
            number("classLoaderCacheDirectories", cacheDirectoryCount())
            boolean("scoreboardCustomTitle", Scoreboard.customTitle)
            boolean("tabListCustomHeader", TabList.customHeader)
            boolean("tabListCustomFooter", TabList.customFooter)
            boolean("worldLoaded", World.isLoaded())
            string("screenClass", screen?.javaClass?.name)
            boolean("ctjsGuiOpen", screen is Gui)
            boolean("cullEnabled", Renderer.isCullEnabled())
            number("cullScopeDepth", Renderer.cullScopeDepth())
            number("matrixPushCounter", Renderer.matrixPushCounter)
            boolean("renderer3dBegan", Renderer3d.isDrawing())
            boolean("renderer3dCullScope", Renderer3d.hasCullScope())
            number("pipelineCache", Renderer3d.pipelineCount(), trailingComma = false)
            append('}')
        }
    }

    private fun cacheDirectoryCount(): Int {
        if (!Files.isDirectory(classLoaderCache))
            return 0
        return Files.list(classLoaderCache).use { entries ->
            entries.filter(Files::isDirectory).count().toInt()
        }
    }

    private fun StringBuilder.number(name: String, value: Number, trailingComma: Boolean = true) {
        append('"').append(name).append("\":").append(value)
        if (trailingComma) append(',')
    }

    private fun StringBuilder.nullableNumber(name: String, value: Number?) {
        append('"').append(name).append("\":").append(value ?: "null").append(',')
    }

    private fun StringBuilder.boolean(name: String, value: Boolean) {
        append('"').append(name).append("\":").append(value).append(',')
    }

    private fun StringBuilder.string(name: String, value: String?) {
        append('"').append(name).append("\":")
        if (value == null) {
            append("null")
        } else {
            append('"').append(value.replace("\\", "\\\\").replace("\"", "\\\"")).append('"')
        }
        append(',')
    }
}
