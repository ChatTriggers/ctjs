package com.chattriggers.ctjs.internal.compat

import com.chattriggers.ctjs.api.Config
import com.chattriggers.ctjs.engine.Console
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.awt.Color

internal class CTConfigScreen(private val parent: Screen? = null) :
    Screen(Component.literal("CTJS Reloaded Configuration")) {
    private enum class Page(val label: String) {
        GENERAL("General"),
        CONSOLE("Console"),
        COLORS("Colors"),
    }

    private data class SettingText(val name: String, val description: String?, val x: Int, val y: Int)
    private data class ColorField(val label: String, val field: EditBox, val previewX: Int, val previewY: Int)

    private var page = Page.GENERAL
    private var status = "Changes are saved automatically"
    private val settingTexts = mutableListOf<SettingText>()
    private val colorFields = mutableListOf<ColorField>()
    private var panelLeft = 0
    private var panelRight = 0
    private var panelBottom = 0

    override fun init() {
        settingTexts.clear()
        colorFields.clear()
        panelLeft = width / 2 - ((width - 24).coerceAtMost(540) / 2)
        panelRight = width - panelLeft
        panelBottom = height - 36

        val tabGap = 4
        val tabWidth = ((panelRight - panelLeft - tabGap * 2) / 3).coerceAtMost(120)
        val tabStart = width / 2 - (tabWidth * 3 + tabGap * 2) / 2
        Page.entries.forEachIndexed { index, target ->
            val button = Button.builder(Component.literal(target.label)) {
                if (page != target) {
                    page = target
                    status = "Changes are saved automatically"
                    rebuildWidgets()
                }
            }.bounds(tabStart + index * (tabWidth + tabGap), 24, tabWidth, 20).build()
            button.active = page != target
            addRenderableWidget(button)
        }

        when (page) {
            Page.GENERAL -> addGeneralSettings()
            Page.CONSOLE -> addConsoleSettings()
            Page.COLORS -> addColorSettings()
        }

        addRenderableWidget(
            Button.builder(Component.literal("Done")) { onClose() }
                .bounds(width / 2 - 75, height - 27, 150, 20)
                .build()
        )
    }

    private fun addGeneralSettings() {
        var y = 52
        addToggle(
            "Show module help on import",
            "Display a module's help message in chat after import.",
            y,
            { Config.moduleImportHelp },
        ) { Config.moduleImportHelp = it }
        y += ROW_HEIGHT
        addToggle(
            "Show module changelog on update",
            "Display the changelog in chat when a module is updated.",
            y,
            { Config.moduleChangelog },
        ) { Config.moduleChangelog = it }
        y += ROW_HEIGHT
        addToggle(
            "Show updates in chat",
            "Show module import and update messages in chat.",
            y,
            { Config.showUpdatesInChat },
        ) { Config.showUpdatesInChat = it }
        y += ROW_HEIGHT
        addToggle(
            "Auto-update modules",
            "Check for and download module updates whenever CT loads.",
            y,
            { Config.autoUpdateModules },
        ) { Config.autoUpdateModules = it }
    }

    private fun addConsoleSettings() {
        var y = 52
        addToggle("Clear console on CT load", null, y, { Config.clearConsoleOnLoad }) {
            Config.clearConsoleOnLoad = it
        }
        y += ROW_HEIGHT
        addToggle(
            "Open console on error",
            "Open the language-specific console when a module errors.",
            y,
            { Config.openConsoleOnError },
        ) { Config.openConsoleOnError = it }
        y += ROW_HEIGHT
        addToggle("Use Fira Code font", "Use Fira Code in the external console window.", y, { Config.consoleFiraCodeFont }) {
            Config.consoleFiraCodeFont = it
        }
        y += ROW_HEIGHT

        addSettingText("Console font size", "Allowed range: 6-32.", y)
        val controlRight = panelRight - 8
        addRenderableWidget(
            Button.builder(Component.literal("-")) {
                Config.consoleFontSize = (Config.consoleFontSize - 1).coerceIn(6..32)
                status = "Console font size: ${Config.consoleFontSize}"
                persist()
                rebuildWidgets()
            }.bounds(controlRight - 104, y + 4, 28, 20).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal(Config.consoleFontSize.toString())) { }
                .bounds(controlRight - 72, y + 4, 40, 20).build().also { it.active = false }
        )
        addRenderableWidget(
            Button.builder(Component.literal("+")) {
                Config.consoleFontSize = (Config.consoleFontSize + 1).coerceIn(6..32)
                status = "Console font size: ${Config.consoleFontSize}"
                persist()
                rebuildWidgets()
            }.bounds(controlRight - 28, y + 4, 28, 20).build()
        )
        y += ROW_HEIGHT

        addSettingText("Console theme", "Theme used when custom colors are disabled.", y)
        val theme = THEMES[Config.consoleTheme.coerceIn(THEMES.indices)]
        addRenderableWidget(
            Button.builder(Component.literal(theme)) {
                Config.consoleTheme = (Config.consoleTheme + 1) % THEMES.size
                status = "Console theme: ${THEMES[Config.consoleTheme]}"
                persist()
                rebuildWidgets()
            }.bounds(panelRight - 158, y + 4, 150, 20).build()
        )
    }

    private fun addColorSettings() {
        val y = 52
        addToggle(
            "Use custom console theme",
            "Override the selected theme with the four colors below.",
            y,
            { Config.customTheme },
            rebuildAfterChange = true,
        ) { Config.customTheme = it }

        val gap = 8
        val columnWidth = (panelRight - panelLeft - 16 - gap) / 2
        val startX = panelLeft + 8
        val startY = y + ROW_HEIGHT + 3
        addColorField("Text", Config.consoleTextColor, startX, startY, columnWidth) { Config.consoleTextColor = it }
        addColorField("Background", Config.consoleBackgroundColor, startX + columnWidth + gap, startY, columnWidth) {
            Config.consoleBackgroundColor = it
        }
        addColorField("Error", Config.consoleErrorColor, startX, startY + 39, columnWidth) { Config.consoleErrorColor = it }
        addColorField("Warning", Config.consoleWarningColor, startX + columnWidth + gap, startY + 39, columnWidth) {
            Config.consoleWarningColor = it
        }

        val save = Button.builder(Component.literal("Apply custom colors")) { saveColors() }
            .bounds(width / 2 - 75, startY + 72, 150, 20)
            .build()
        save.active = Config.customTheme
        addRenderableWidget(save)
    }

    private fun addToggle(
        name: String,
        description: String?,
        y: Int,
        getter: () -> Boolean,
        rebuildAfterChange: Boolean = false,
        setter: (Boolean) -> Unit,
    ) {
        addSettingText(name, description, y)
        addRenderableWidget(
            Button.builder(toggleValue(getter())) { button ->
                setter(!getter())
                button.message = toggleValue(getter())
                status = "$name: ${if (getter()) "ON" else "OFF"}"
                persist()
                if (rebuildAfterChange) rebuildWidgets()
            }.bounds(panelRight - 68, y + 4, 60, 20).build()
        )
    }

    private fun addSettingText(name: String, description: String?, y: Int) {
        settingTexts += SettingText(name, description, panelLeft + 8, y + 3)
    }

    private fun addColorField(
        label: String,
        color: Color,
        x: Int,
        y: Int,
        fieldWidth: Int,
        setter: (Color) -> Unit,
    ) {
        val inputWidth = (fieldWidth - 18).coerceAtLeast(70)
        val field = EditBox(font, x, y + 12, inputWidth, 20, Component.literal("$label color"))
        field.setMaxLength(9)
        field.value = color.toHex()
        field.setHint(Component.literal("#RRGGBB"))
        field.setEditable(Config.customTheme)
        field.setResponder { value ->
            val parsed = parseColor(value)
            if (parsed == null) {
                status = "$label must use #RRGGBB or #AARRGGBB"
            } else {
                setter(parsed)
                status = "$label color updated"
                persist()
            }
        }
        colorFields += ColorField(label, field, x + inputWidth + 4, y + 14)
        addRenderableWidget(field)
    }

    private fun saveColors() {
        val parsed = colorFields.map { colorField -> colorField.label to parseColor(colorField.field.value) }
        if (parsed.any { it.second == null }) {
            status = "Colors must use #RRGGBB or #AARRGGBB"
            return
        }

        parsed.forEach { (label, color) ->
            when (label) {
                "Text" -> Config.consoleTextColor = color!!
                "Background" -> Config.consoleBackgroundColor = color!!
                "Error" -> Config.consoleErrorColor = color!!
                "Warning" -> Config.consoleWarningColor = color!!
            }
        }
        status = "Custom console colors applied"
        persist()
    }

    private fun parseColor(value: String): Color? = runCatching {
        val hex = value.removePrefix("#")
        when (hex.length) {
            6 -> Color(hex.toInt(16))
            8 -> Color(hex.toLong(16).toInt(), true)
            else -> error("Invalid color")
        }
    }.getOrNull()

    private fun persist() {
        Config.markDirty()
        Config.writeData()
        Console.onConsoleSettingsChanged(Config.ConsoleSettings.make())
    }

    override fun extractRenderState(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        graphics.fill(panelLeft - 1, 5, panelRight + 1, panelBottom + 1, 0xD0101010.toInt())
        graphics.fill(panelLeft, 6, panelRight, 22, 0xFF202838.toInt())
        graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF.toInt())

        settingTexts.forEach { setting ->
            graphics.text(font, setting.name, setting.x, setting.y, 0xFFFFFFFF.toInt())
            setting.description?.let {
                graphics.text(font, it, setting.x, setting.y + 11, 0xFF9AA4B5.toInt(), false)
            }
        }

        colorFields.forEach { colorField ->
            graphics.text(font, colorField.label, colorField.field.x, colorField.field.y - 10, 0xFFFFFFFF.toInt())
            val previewColor = parseColor(colorField.field.value)?.rgb ?: 0xFF3A3A3A.toInt()
            graphics.fill(
                colorField.previewX,
                colorField.previewY,
                colorField.previewX + 12,
                colorField.previewY + 12,
                previewColor,
            )
        }

        graphics.centeredText(font, status, width / 2, height - 39, 0xFFB8C6E3.toInt())
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
    }

    override fun onClose() {
        persist()
        minecraft.setScreen(parent)
    }

    override fun isPauseScreen() = false

    override fun isInGameUi() = true

    private fun toggleValue(value: Boolean) = Component.literal(if (value) "ON" else "OFF")

    private fun Color.toHex() = "#%08X".format(rgb)

    companion object {
        private const val ROW_HEIGHT = 30

        private val THEMES = arrayOf(
            "Default Dark",
            "Ashes Dark",
            "Atelierforest Dark",
            "Isotope Dark",
            "Codeschool Dark",
            "Gotham",
            "Hybrid",
            "3024 Light",
            "Chalk Light",
            "Blue",
            "Slate",
            "Red",
            "Green",
            "Aids",
        )
    }
}
