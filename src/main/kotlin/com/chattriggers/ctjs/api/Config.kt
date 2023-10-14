package com.chattriggers.ctjs.api

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.engine.Console
import com.chattriggers.ctjs.internal.utils.CategorySorting
import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.Property
import gg.essential.vigilance.data.PropertyType
import java.awt.Color
import java.io.File
import kotlin.reflect.KProperty

object Config : Vigilant(File(CTJS.configLocation, "ChatTriggers.toml"), sortingBehavior = CategorySorting) {
    @JvmStatic
    @Property(
        PropertyType.SWITCH,
        name = "Show module help on import",
        category = "General",
        description = "If a module is imported and it has a help message, display it in chat"
    )
    var moduleImportHelp = true

    @JvmStatic
    @Property(
        PropertyType.SWITCH,
        name = "Show module changelog on update",
        category = "General",
        description = "If a module is updated and it has a changelog, display it in chat"
    )
    var moduleChangelog = true

    @JvmStatic
    @Property(
        PropertyType.SWITCH,
        name = "Show updates in chat",
        category = "General",
        description = "Show CT module import/update messages in the chat",
    )
    var showUpdatesInChat = true

    @JvmStatic
    @Property(
        PropertyType.SWITCH,
        name = "Auto-update modules",
        category = "General",
        description = "Check for and download module updates every time CT loads",
    )
    var autoUpdateModules = true

    @JvmStatic
    @Property(
        PropertyType.SWITCH,
        name = "Clear console on CT load",
        category = "Console",
    )
    var clearConsoleOnLoad = true

    @JvmStatic
    @Property(
        PropertyType.SWITCH,
        name = "Open console on error",
        category = "Console",
        description = "Opens the language-specific console if there is an error in a module",
    )
    var openConsoleOnError = false

    @JvmStatic
    @Property(
        PropertyType.SWITCH,
        name = "Use Fira Code font for console",
        category = "Console",
    )
    var consoleFiraCodeFont = true

    @JvmStatic
    @Property(
        PropertyType.NUMBER,
        name = "Console font size",
        category = "Console",
        min = 6,
        max = 32,
    )
    var consoleFontSize = 12

    @JvmStatic
    @Property(
        PropertyType.SWITCH,
        name = "Use custom console theme",
        category = "Console",
    )
    var customTheme = false

    @JvmStatic
    @Property(
        PropertyType.SELECTOR,
        name = "Console theme",
        category = "Console",
        options = [
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
            "Aids"
        ]
    )
    var consoleTheme = 0

    @JvmStatic
    @Property(
        PropertyType.COLOR,
        name = "Console Text color",
        category = "Console",
    )
    var consoleTextColor = Color(208, 208, 208)

    @JvmStatic
    @Property(
        PropertyType.COLOR,
        name = "Console background color",
        category = "Console",
    )
    var consoleBackgroundColor = Color(21, 21, 21)

    @JvmStatic
    @Property(
        PropertyType.COLOR,
        name = "Console error color",
        category = "Console",
    )
    var consoleErrorColor = Color(225, 65, 73)

    @JvmStatic
    @Property(
        PropertyType.COLOR,
        name = "Console warning color",
        category = "Console",
    )
    var consoleWarningColor = Color(248, 191, 84)

    init {
        initialize()

        addDependency("consoleTextColor", "customTheme")
        addDependency("consoleBackgroundColor", "customTheme")
        addDependency("consoleErrorColor", "customTheme")
        addDependency("consoleWarningColor", "customTheme")

        listenToConsoleProperty(::clearConsoleOnLoad)
        listenToConsoleProperty(::openConsoleOnError)
        listenToConsoleProperty(::consoleFiraCodeFont)
        listenToConsoleProperty(::consoleFontSize)
        listenToConsoleProperty(::customTheme)
        listenToConsoleProperty(::consoleTheme)
        listenToConsoleProperty(::consoleTextColor)
        listenToConsoleProperty(::consoleBackgroundColor)
        listenToConsoleProperty(::consoleErrorColor)
        listenToConsoleProperty(::consoleWarningColor)
    }

    private inline fun <reified T> listenToConsoleProperty(property: KProperty<T>) {
        val field = ConsoleSettings::class.java.getDeclaredField(property.name)
        field.isAccessible = true

        registerListener<T>(property.name) {
            val settings = ConsoleSettings.make()
            field.set(settings, it)
            Console.onConsoleSettingsChanged(settings)
        }
    }

    // The listener properties above get called before the property is updated, so we have
    // to keep track of it ourselves and use the new value that is passed in
    data class ConsoleSettings(
        var clearConsoleOnLoad: Boolean,
        var openConsoleOnError: Boolean,
        var consoleFiraCodeFont: Boolean,
        var consoleFontSize: Int,
        var customTheme: Boolean,
        var consoleTheme: Int,
        var consoleTextColor: Color,
        var consoleBackgroundColor: Color,
        var consoleErrorColor: Color,
        var consoleWarningColor: Color,
    ) {
        companion object {
            fun make() = ConsoleSettings(
                clearConsoleOnLoad,
                openConsoleOnError,
                consoleFiraCodeFont,
                consoleFontSize,
                customTheme,
                consoleTheme,
                consoleTextColor,
                consoleBackgroundColor,
                consoleErrorColor,
                consoleWarningColor,
            )
        }
    }
}
