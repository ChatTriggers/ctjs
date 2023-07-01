package com.chattriggers.ctjs.minecraft.objects.display

import com.chattriggers.ctjs.minecraft.CTEvents
import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer
import com.chattriggers.ctjs.minecraft.libs.renderer.Text
import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.triggers.RegularTrigger
import com.chattriggers.ctjs.triggers.TriggerType
import org.mozilla.javascript.NativeObject

class DisplayLine {
    private lateinit var text: Text

    private var textWidth = 0f

    private var textColor: Long? = null
    private var backgroundColor: Long? = null
    private var background: Display.Background? = null
    private var align: Display.Align? = null

    private var onClicked: RegularTrigger? = null
    private var onHovered: RegularTrigger? = null
    private var onDragged: RegularTrigger? = null
    private var onMouseLeave: RegularTrigger? = null

    internal var shouldRender: Boolean = true

    private var hovered: Boolean = false
    private var cachedX = 0.0
    private var cachedY = 0.0
    private var cachedWidth = 0.0
    private var cachedHeight = 0.0

    constructor(text: String) {
        setText(text)
    }

    constructor(text: String, config: NativeObject) {
        setText(text)

        textColor = config.getOption("textColor", null)?.toLong()
        backgroundColor = config.getOption("backgroundColor", null)?.toLong()

        setAlign(config.getOption("align", null))
        setBackground(config.getOption("background", null))
    }

    private fun NativeObject?.getOption(key: String, default: Any?): String? {
        return (this?.get(key) ?: default)?.toString()
    }

    init {
        CTEvents.MOUSE_CLICKED.register { x, y, button, pressed ->
            if (shouldRender && x in cachedX..cachedX + cachedWidth && y in cachedY..cachedY + cachedHeight)
                onClicked?.trigger(arrayOf(x, y, button, pressed))
        }
        CTEvents.MOUSE_DRAGGED.register { dx, dy, x, y, button ->
            if (shouldRender)
                onDragged?.trigger(arrayOf(dx, dy, x, y, button))
        }
    }

    fun getText(): Text = text

    fun setText(text: String) = apply {
        this.text = Text(text)
        textWidth = Renderer.getStringWidth(text) * this.text.getScale()
    }

    fun getTextColor(): Long? = textColor

    fun setTextColor(color: Long?) = apply {
        textColor = color
    }

    fun getTextWidth(): Float = textWidth

    fun setShadow(shadow: Boolean) = apply { text.setShadow(shadow) }

    fun setScale(scale: Float) = apply {
        text.setScale(scale)
        textWidth = Renderer.getStringWidth(text.getString()) * scale
    }

    fun getAlign(): Display.Align? = align

    fun setAlign(align: Any?) = apply {
        this.align = when (align) {
            is String -> Display.Align.valueOf(align.uppercase())
            is Display.Align -> align
            else -> null
        }
    }

    fun getBackground(): Display.Background? = background

    fun setBackground(background: Any?) = apply {
        this.background = when (background) {
            is String -> Display.Background.valueOf(background.uppercase().replace(" ", "_"))
            is Display.Background -> background
            else -> null
        }
    }

    fun getBackgroundColor(): Long? = backgroundColor

    fun setBackgroundColor(backgroundColor: Long?) = apply {
        this.backgroundColor = backgroundColor
    }

    fun registerClicked(method: Any) = apply {
        onClicked = RegularTrigger(method, TriggerType.OTHER)
    }

    fun registerHovered(method: Any) = apply {
        onHovered = RegularTrigger(method, TriggerType.OTHER)
    }

    fun registerMouseLeave(method: Any) = apply {
        onMouseLeave = RegularTrigger(method, TriggerType.OTHER)
    }

    fun registerDragged(method: Any) = apply {
        onDragged = RegularTrigger(method, TriggerType.OTHER)
    }

    fun unregisterClicked() = apply {
        onClicked?.unregister()
        onClicked = null
    }

    fun unregisterHovered() = apply {
        onHovered?.unregister()
        onHovered = null
    }

    fun unregisterMouseLeave() = apply {
        onMouseLeave?.unregister()
        onMouseLeave = null
    }

    fun unregisterDragged() = apply {
        onDragged?.unregister()
        onDragged = null
    }

    fun draw(
        x: Float,
        y: Float,
        totalWidth: Float,
        displayBackground: Display.Background,
        displayBackgroundColor: Long,
        displayTextColor: Long,
        align: Display.Align,
    ) {
        val background = this.background ?: displayBackground
        val backgroundColor = this.backgroundColor ?: displayBackgroundColor
        val textColor = this.textColor ?: displayTextColor

        // X relative to the top left of the display
        val baseX = when (align) {
            Display.Align.LEFT -> x
            Display.Align.CENTER -> x - totalWidth / 2
            Display.Align.RIGHT -> x - totalWidth
        }

        if (background == Display.Background.FULL)
            Renderer.drawRect(backgroundColor, baseX - 1, y - 1, totalWidth + 1, text.getHeight())

        if (text.getString().isEmpty())
            return

        val xOffset = when (this.align ?: align) {
            Display.Align.LEFT -> baseX
            Display.Align.CENTER -> baseX + (totalWidth - textWidth) / 2
            Display.Align.RIGHT -> baseX + (totalWidth - textWidth)
        }

        if (background == Display.Background.PER_LINE)
            Renderer.drawRect(backgroundColor, xOffset - 1, y - 1, textWidth + 1, text.getHeight())

        text.setX(xOffset).setY(y).setColor(textColor).draw()

        cachedX = baseX - 1.0
        cachedY = y - 2.0
        cachedWidth = totalWidth + 1.0
        cachedHeight = text.getHeight() + 1.0

        if (!shouldRender)
            return

        if (
            Client.getMouseX() in cachedX..cachedX + cachedWidth &&
            Client.getMouseY() in cachedY..cachedY + cachedHeight
        ) {
            hovered = true
            onHovered?.trigger(
                arrayOf(
                    Client.getMouseX(),
                    Client.getMouseY()
                )
            )
        } else {
            if (hovered) {
                onMouseLeave?.trigger(
                    arrayOf(
                        Client.getMouseX(),
                        Client.getMouseY()
                    )
                )
            }

            hovered = false
        }
    }

    override fun toString() =
        "DisplayLine{" +
            "text=$text, textColor=$textColor, align=$align, " +
            "background=$background, backgroundColor=$backgroundColor" +
            "}"
}
