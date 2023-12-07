package com.chattriggers.ctjs.api.message

import com.chattriggers.ctjs.MCEntity
import com.chattriggers.ctjs.api.entity.Entity
import com.chattriggers.ctjs.api.inventory.Item
import com.chattriggers.ctjs.api.inventory.ItemType
import com.chattriggers.ctjs.api.render.Renderer
import com.chattriggers.ctjs.internal.utils.hoverEventActionByName
import gg.essential.universal.UChat
import net.minecraft.item.ItemStack
import net.minecraft.text.*
import net.minecraft.util.Formatting
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
class TextComponent : Text {
    lateinit var component: MutableText
        private set

    private var text: String
    private var formatted = true
    private var clickAction: ClickEvent.Action? = null
    private var clickValue: String? = null
    private var hoverAction: HoverEvent.Action<*>? = null
    private var hoverValue: Any? = null

    /**
     * Creates a [TextComponent] from a string.
     *
     * @param text the text string in the component.
     */
    constructor(text: String) {
        this.text = text
        reInstance()
    }

    /**
     * Creates a [TextComponent] from an existing [Text] instance.
     *
     * @param component the [Text] to convert
     */
    constructor(component: Text) : this(component.copy())

    /**
     * Creates a [TextComponent] from an existing [Text] instance.
     *
     * @param component the [Text] to convert
     */
    constructor(component: MutableText) {
        this.component = component
        text = formattedText

        val clickEvent = component.style.clickEvent
        if (clickEvent != null) {
            clickAction = clickEvent.action
            clickValue = clickEvent.value
        }

        val hoverEvent = component.style.hoverEvent
        if (hoverEvent != null) {
            hoverAction = hoverEvent.action
            hoverValue = hoverEvent.getValue(hoverAction)
        }
    }

    /**
     * Gets the component text
     */
    fun getText() = text

    /**
     * Sets the component text
     */
    fun setText(value: String) = apply {
        text = value
        reInstance()
    }

    /**
     * Whether this component is formatted. A formatted component interprets
     * color codes (using both & and §) and applies them as style.
     */
    fun isFormatted() = formatted

    /**
     * Sets whether this component is formatted. A formatted component interprets
     * color codes (using both & and §) and applies them as style.
     */
    fun setFormatted(value: Boolean) = apply {
        formatted = value
        reInstance()
    }

    /**
     * Gets the action to be performed when the component is clicked on. See [setClickAction]
     * for possible values.
     */
    fun getClickAction() = clickAction

    /**
     * Sets the action to be performed when the component is clicked on. Possible actions include:
     * - open_url
     * - open_file
     * - run_command
     * - suggest_command
     * - change_page
     *
     * @param value The new click action, can be a [ClickEvent.Action], [String], or null
     */
    fun setClickAction(value: Any?) = apply {
        clickAction = when (value) {
            is ClickEvent.Action -> value
            is String -> ClickEvent.Action.valueOf(value.uppercase())
            null -> null
            else -> error(
                "TextComponent.setClickAction() expects a String, ClickEvent.Action, or null, but got " +
                    value::class
            )
        }

        reInstanceClick()
    }

    /**
     * The value to be used by the click action. The value is interpreted according to [clickAction]
     */
    fun getClickValue() = clickValue

    /**
     * Sets the value to be used by the click action. The value is interpreted according to [clickAction].
     */
    fun setClickValue(value: String?) = apply {
        clickValue = value
        reInstanceClick()
    }

    /**
     * Sets the click action and value of the component.See [clickAction] for
     * possible click actions.
     *
     * @param action the click action
     * @param value the click value
     */
    fun setClick(action: ClickEvent.Action?, value: String?) = apply {
        clickAction = action
        clickValue = value
        reInstanceClick()
    }

    /**
     * Sets the click action and value of the component.See [clickAction] for
     * possible click actions.
     *
     * @param action the click action as a [String]
     * @param value the click value
     */
    fun setClick(action: String, value: String?) = apply {
        setClick(ClickEvent.Action.valueOf(action.uppercase()), value)
    }

    /**
     * Gets the action to be performed when the component is hovered. See [setHoverAction]
     * for possible values
     */
    fun getHoverAction() = hoverAction

    /**
     * Sets the action to be performed when the component is hovered. Possible actions include:
     * - show_text
     * - show_item
     * - show_entity
     *
     * @param value The new hover action, can be a [HoverEvent.Action], [String], or null
     */
    fun setHoverAction(value: Any?) = apply {
        hoverAction = when (value) {
            is HoverEvent.Action<*> -> value
            is String -> hoverEventActionByName(value.lowercase())
            null -> null
            else -> error(
                "TextComponent.setHoverAction() expects a String, HoverEvent.Action, or null, but got " +
                    value::class
            )
        }

        // Trigger re-wrapping if necessary
        setHoverValue(hoverValue)
    }

    /**
     * Gets the value to be used by the hover action. The value is interpreted according to [hoverAction]
     */
    fun getHoverValue() = hoverValue

    /**
     * Sets the value to be used by the hover action. The value is interpreted according to [hoverAction]
     */
    fun setHoverValue(value: Any?) = apply {
        hoverValue = value?.let {
            when (hoverAction) {
                HoverEvent.Action.SHOW_TEXT -> from(it)
                HoverEvent.Action.SHOW_ITEM -> parseItemContent(it)
                HoverEvent.Action.SHOW_ENTITY -> parseEntityContent(it)
                else -> value
            }
        }

        reInstanceHover()
    }

    /**
     * Sets the hover action and value of the component. See [hoverAction] for possible hover actions.
     *
     * @param action the hover action
     * @param value the hover value
     */
    fun setHover(action: HoverEvent.Action<*>?, value: Any?) = apply {
        hoverAction = action
        setHoverValue(value)
    }

    /**
     * Sets the hover action and value of the component. See [hoverAction] for possible hover actions.
     *
     * @param action the hover action as a [String]
     * @param value the hover value
     */
    fun setHover(action: String, value: Any?) = apply {
        setHover(hoverEventActionByName(action.lowercase()), value)
    }

    /**
     * Sets the color of this [TextComponent]
     * This won't override your color codes unless &r is explicitly used.
     *
     * @param color RGB value acquired using [Renderer.getColor]. Alpha values will be ignored
     */
    fun setColor(color: Long) = apply {
        component.setStyle(component.style.withColor(color.toInt()));
    }

    /**
     * Sets the color of this [TextComponent]
     * This won't override your color codes unless &r is explicitly used.
     *
     * @param red value between 0 and 255
     * @param green value between 0 and 255
     * @param blue value between 0 and 255
     */
    fun setColor(red: Int, green: Int, blue: Int) = setColor(Renderer.getColor(red, green, blue))

    /**
     * Shows the component in chat as a new [Message]
     */
    fun chat() = apply {
        Message(this).chat()
    }

    /**
     * Shows the component on the actionbar as a new [Message]
     */
    fun actionBar() = apply {
        Message(this).actionBar()
    }

    override fun toString() = "TextComponent(${if (formatted) formattedText else unformattedText})"

    private fun parseItemContent(obj: Any): HoverEvent.ItemStackContent {
        return when (obj) {
            is ItemStack -> obj
            is Item -> obj.toMC()
            is String -> ItemType(obj).asItem().toMC()
            is HoverEvent.ItemStackContent -> return obj
            else -> error("${obj::class} cannot be parsed as an item HoverEvent")
        }.let(HoverEvent::ItemStackContent)
    }

    private fun parseEntityContent(obj: Any): HoverEvent.EntityContent? {
        return when (obj) {
            is MCEntity -> obj
            is Entity -> obj.toMC()
            //#if MC>=12004
            is String -> return HoverEvent.EntityContent.legacySerializer(from(obj)).getOrThrow(false) {}
            //#else
            //$$ is String -> return HoverEvent.EntityContent.parse(from(obj))
            //#endif
            is HoverEvent.EntityContent -> return obj
            else -> error("${obj::class} cannot be parsed as an entity HoverEvent")
        }.let { HoverEvent.EntityContent(it.type, it.uuid, it.name) }
    }

    private fun reInstance() {
        component = Text.literal(text.formatIf(formatted))

        reInstanceClick()
        reInstanceHover()
    }

    private fun reInstanceClick() {
        if (clickAction == null || clickValue == null)
            return

        val event = ClickEvent(clickAction, clickValue!!.formatIf(formatted))
        component.style = component.style.withClickEvent(event)
    }

    private fun reInstanceHover() {
        if (hoverAction == null || hoverValue == null)
            return

        @Suppress("UNCHECKED_CAST")
        val event = HoverEvent(hoverAction as HoverEvent.Action<Any>, hoverValue!!)
        component.style = component.style.withHoverEvent(event)
    }

    private fun String.formatIf(predicate: Boolean) = if (predicate) UChat.addColor(this) else this

    private class TextBuilder(private val isFormatted: Boolean) : CharacterVisitor {
        private val builder = StringBuilder()
        private var cachedStyle: Style? = null

        override fun accept(index: Int, style: Style, codePoint: Int): Boolean {
            if (isFormatted && style != cachedStyle) {
                cachedStyle = style
                builder.append(formatString(style))
            }

            builder.append(codePoint.toChar())
            return true
        }

        fun getString() = builder.toString()

        private fun formatString(style: Style): String {
            val builder = StringBuilder("§r")

            when {
                style.isBold -> builder.append("§l")
                style.isItalic -> builder.append("§o")
                style.isUnderlined -> builder.append("§n")
                style.isStrikethrough -> builder.append("§m")
                style.isObfuscated -> builder.append("§k")
            }

            style.color?.let(colorToFormatChar::get)?.run(builder::append)

            return builder.toString()
        }

        companion object {
            private val colorToFormatChar = Formatting.values().mapNotNull { format ->
                TextColor.fromFormatting(format)?.let { it to format }
            }.toMap()
        }
    }

    // **********************
    // * METHOD DELEGATIONS *
    // **********************

    override fun getContent(): TextContent = component.content

    val unformattedText: String
        get() {
            val builder = TextBuilder(false)
            component.asOrderedText().accept(builder)
            return builder.getString()
        }

    val formattedText: String
        get() {
            val builder = TextBuilder(true)
            component.asOrderedText().accept(builder)
            return builder.getString()
        }

    fun appendSibling(text: Text): MutableText = component.append(text)

    fun append(text: Text) = appendSibling(text)

    override fun getString(): String = component.string

    override fun getStyle(): Style = component.style

    override fun getSiblings(): MutableList<Text> = component.siblings

    override fun <T : Any?> visit(styledVisitor: StringVisitable.StyledVisitor<T>?, style: Style?): Optional<T> =
        component.visit(styledVisitor, style)

    override fun <T : Any?> visit(visitor: StringVisitable.Visitor<T>?): Optional<T> = component.visit(visitor)

    override fun asTruncatedString(length: Int): String = component.asTruncatedString(length)

    override fun copyContentOnly(): MutableText = component.copyContentOnly()

    override fun copy(): MutableText = component.copy()

    override fun asOrderedText(): OrderedText = component.asOrderedText()

    override fun withoutStyle(): MutableList<Text> = component.withoutStyle()

    override fun getWithStyle(style: Style?): MutableList<Text> = component.getWithStyle(style)

    override fun contains(text: Text?): Boolean = component.contains(text)

    companion object {
        fun from(obj: Any): TextComponent? {
            return when (obj) {
                is TextComponent -> obj
                is String -> TextComponent(obj)
                is Text -> TextComponent(obj)
                else -> null
            }
        }

        fun stripFormatting(string: String): String {
            return Formatting.strip(string)!!
        }
    }
}
