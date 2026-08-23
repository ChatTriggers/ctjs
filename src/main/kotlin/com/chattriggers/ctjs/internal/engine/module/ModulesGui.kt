package com.chattriggers.ctjs.internal.engine.module

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import kotlin.math.roundToInt

object ModulesGui : Screen(Component.literal("CTJS Reloaded Modules")) {
    private data class ModuleBounds(
        val module: Module,
        val headerLeft: Int,
        val headerTop: Int,
        val headerRight: Int,
        val headerBottom: Int,
        val deleteLeft: Int? = null,
        val deleteTop: Int? = null,
        val deleteRight: Int? = null,
        val deleteBottom: Int? = null,
    )

    private val expandedModules = mutableSetOf<String>()
    private val moduleBounds = mutableListOf<ModuleBounds>()
    private var scroll = 0
    private var maxScroll = 0
    private var listLeft = 0
    private var listTop = 0
    private var listRight = 0
    private var listBottom = 0
    private var status = ""

    override fun init() {
        addRenderableWidget(
            Button.builder(Component.literal("Done")) { onClose() }
                .bounds(width / 2 - 75, height - 27, 150, 20)
                .build()
        )
    }

    override fun extractRenderState(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        val panelWidth = (width - 32).coerceAtMost(520).coerceAtLeast(220)
        listLeft = width / 2 - panelWidth / 2
        listRight = listLeft + panelWidth
        listTop = 46
        listBottom = (height - 36).coerceAtLeast(listTop + 30)

        graphics.fill(listLeft - 1, 18, listRight + 1, listBottom + 1, 0xD0101010.toInt())
        graphics.fill(listLeft, 19, listRight, 44, 0xFF202838.toInt())
        graphics.centeredText(font, title, width / 2, 27, 0xFFFFFFFF.toInt())

        val modules = ModuleManager.cachedModules.toList().sortedBy { it.name.lowercase() }
        moduleBounds.clear()

        if (modules.isEmpty()) {
            maxScroll = 0
            scroll = 0
            val centerY = listTop + (listBottom - listTop) / 2
            graphics.centeredText(font, "No modules installed", width / 2, centerY - 10, 0xFFFFFFFF.toInt())
            graphics.centeredText(
                font,
                "Use /ct import <module> to install one.",
                width / 2,
                centerY + 6,
                0xFFAAAAAA.toInt(),
            )
        } else {
            val contentHeight = modules.sumOf(::moduleHeight) + (modules.size - 1) * ROW_GAP
            maxScroll = (contentHeight - (listBottom - listTop)).coerceAtLeast(0)
            scroll = scroll.coerceIn(0, maxScroll)

            graphics.enableScissor(listLeft, listTop, listRight, listBottom)
            var y = listTop - scroll
            modules.forEach { module ->
                drawModule(graphics, module, listLeft, y, panelWidth)
                y += moduleHeight(module) + ROW_GAP
            }
            graphics.disableScissor()

            if (maxScroll > 0) {
                val trackX = listRight - 3
                val viewportHeight = listBottom - listTop
                val thumbHeight = (viewportHeight * viewportHeight / contentHeight).coerceAtLeast(12)
                val thumbTop = listTop + (scroll * (viewportHeight - thumbHeight) / maxScroll)
                graphics.fill(trackX, listTop, listRight, listBottom, 0x50000000)
                graphics.fill(trackX, thumbTop, listRight, thumbTop + thumbHeight, 0xFF8A98B3.toInt())
            }
        }

        if (status.isNotEmpty()) {
            graphics.centeredText(font, status, width / 2, height - 39, 0xFFFFAAAA.toInt())
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
    }

    private fun drawModule(graphics: GuiGraphicsExtractor, module: Module, x: Int, y: Int, width: Int) {
        val expanded = module.name in expandedModules
        val headerBottom = y + HEADER_HEIGHT
        graphics.fill(x, y, x + width, headerBottom, 0xE0262E3D.toInt())

        val displayName = module.metadata.name ?: module.name
        graphics.text(font, displayName, x + 8, y + 8, 0xFFFFFFFF.toInt())
        graphics.text(font, if (expanded) "-" else "+", x + width - 14, y + 8, 0xFFB8C6E3.toInt())

        module.metadata.version?.let { version ->
            val versionText = "v$version"
            val versionX = x + width - 25 - font.width(versionText)
            graphics.text(font, versionText, versionX, y + 8, 0xFF8D99AE.toInt())
        }

        if (!expanded) {
            moduleBounds += ModuleBounds(module, x, y, x + width, headerBottom)
            return
        }

        val description = module.metadata.description ?: "No description provided in the metadata."
        val descriptionLines = font.split(Component.literal(description), width - 16)
        val bodyBottom = y + moduleHeight(module)
        graphics.fill(x, headerBottom, x + width, bodyBottom, 0xD0181D27.toInt())

        var textY = headerBottom + 7
        descriptionLines.forEach { line ->
            graphics.text(font, line, x + 8, textY, 0xFFD5D8DE.toInt(), false)
            textY += 10
        }

        module.metadata.creator?.let { creator ->
            graphics.text(font, "by $creator", x + 8, bodyBottom - 15, 0xFF8D99AE.toInt(), false)
        }

        val required = module.metadata.isRequired && module.requiredBy.isNotEmpty()
        if (required) {
            val requiredText = "Required by ${module.requiredBy.sorted().joinToString()}"
            graphics.text(
                font,
                requiredText,
                x + width - 8 - font.width(requiredText),
                bodyBottom - 15,
                0xFF8D99AE.toInt(),
                false,
            )
            moduleBounds += ModuleBounds(module, x, y, x + width, headerBottom)
        } else {
            val deleteText = "Delete"
            val deleteWidth = font.width(deleteText)
            val deleteLeft = x + width - 8 - deleteWidth
            val deleteTop = bodyBottom - 18
            graphics.text(font, deleteText, deleteLeft, bodyBottom - 15, 0xFFFF6B6B.toInt())
            moduleBounds += ModuleBounds(
                module,
                x,
                y,
                x + width,
                headerBottom,
                deleteLeft - 3,
                deleteTop,
                x + width - 5,
                bodyBottom,
            )
        }
    }

    private fun moduleHeight(module: Module): Int {
        if (module.name !in expandedModules) return HEADER_HEIGHT
        val description = module.metadata.description ?: "No description provided in the metadata."
        val contentWidth = (listRight - listLeft - 16).coerceAtLeast(1)
        return HEADER_HEIGHT + 7 + font.split(Component.literal(description), contentWidth).size * 10 + 22
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (super.mouseClicked(event, doubleClick)) return true
        if (event.button() != 0) return false

        val mouseX = event.x()
        val mouseY = event.y()
        val clicked = moduleBounds.firstOrNull { bounds ->
            mouseX >= bounds.headerLeft && mouseX < bounds.headerRight &&
                mouseY >= bounds.headerTop && mouseY < bounds.headerBottom
        }
        if (clicked != null) {
            if (!expandedModules.add(clicked.module.name)) expandedModules.remove(clicked.module.name)
            return true
        }

        val delete = moduleBounds.firstOrNull { bounds ->
            bounds.deleteLeft != null &&
                mouseX >= bounds.deleteLeft && mouseX < bounds.deleteRight!! &&
                mouseY >= bounds.deleteTop!! && mouseY < bounds.deleteBottom!!
        }
        if (delete != null) {
            val name = delete.module.name
            status = if (ModuleManager.deleteModule(name)) "Deleted $name" else "Could not delete $name"
            expandedModules.remove(name)
            return true
        }

        return false
    }

    override fun mouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean {
        if (x < listLeft || x >= listRight || y < listTop || y >= listBottom || maxScroll == 0) {
            return super.mouseScrolled(x, y, scrollX, scrollY)
        }

        scroll = (scroll - scrollY * 20.0).roundToInt().coerceIn(0, maxScroll)
        return true
    }

    override fun onClose() {
        minecraft.setScreen(null)
    }

    override fun isPauseScreen() = false

    override fun isInGameUi() = true

    private const val HEADER_HEIGHT = 25
    private const val ROW_GAP = 3
}
