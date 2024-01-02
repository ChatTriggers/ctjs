package com.chattriggers.ctjs.api.render

import com.chattriggers.ctjs.api.message.ChatLib
import com.chattriggers.ctjs.internal.utils.getOption
import net.minecraft.text.Style
import org.mozilla.javascript.NativeObject

class Text {
    private lateinit var string: String
    private var x: Float = 0f
    private var y: Float = 0f

    private val lines = mutableListOf<String>()

    private var color = 0xffffffff
    private var backgroundColor = 0xff000000
    private var formatted = true
    private var shadow = false
    private var align = Align.LEFT
    private var background = false

    private var width = 0f
    private var maxWidth = 0
    private var maxLines = Int.MAX_VALUE
    private var scale = 1f

    @JvmOverloads
    constructor(string: String, x: Float = 0f, y: Float = 0f) {
        setString(string)
        setX(x)
        setY(y)
    }

    constructor(string: String, config: NativeObject) {
        setString(string)
        setColor(config.getOption("color", 0xffffffff).toLong())
        setFormatted(config.getOption("formatted", true).toBoolean())
        setShadow(config.getOption("shadow", false).toBoolean())
        setAlign(config.getOption("align", Align.LEFT))
        setBackground(config.getOption("background", false).toBoolean())
        setBackgroundColor(config.getOption("backgroundColor", 0x00000000).toLong())
        setX(config.getOption("x", 0f).toFloat())
        setY(config.getOption("y", 0f).toFloat())
        setMaxLines((config.getOption("maxLines", Int.MAX_VALUE)).toDouble().toInt())
        setScale(config.getOption("scale", 1f).toFloat())
        setMaxWidth(config.getOption("maxWidth", 0).toInt())
    }

    fun getString(): String = string

    fun setString(string: String) = apply {
        this.string = string
        updateFormatting()
    }

    fun getColor(): Long = color

    fun setColor(color: Long) = apply { this.color = Renderer.fixAlpha(color) }

    fun getFormatted(): Boolean = formatted

    fun setFormatted(formatted: Boolean) = apply {
        this.formatted = formatted
        updateFormatting()
    }

    fun getShadow(): Boolean = shadow

    fun setShadow(shadow: Boolean) = apply { this.shadow = shadow }

    fun getAlign(): Align = align

    fun setAlign(align: Any) = apply {
        this.align = when (align) {
            is CharSequence -> Align.valueOf(align.toString().uppercase())
            is Align -> align
            else -> Align.LEFT
        }
    }

    fun getBackground(): Boolean = background

    /**
     * Set the background
     *
     * true: Background is enabled
     * false: Background is disabled
     */
    fun setBackground(background: Boolean) = apply {
        this.background = background
    }

    fun getBackgroundColor(): Long = backgroundColor

    fun setBackgroundColor(backgroundColor: Long) = apply {
        this.backgroundColor = backgroundColor
    }

    fun getX(): Float = x

    fun setX(x: Float) = apply { this.x = x }

    fun getY(): Float = y

    fun setY(y: Float) = apply { this.y = y }

    /**
     * Gets the width of the text
     * This is automatically updated when the text is drawn.
     *
     * @return the width of the text
     */
    fun getWidth(): Float = width

    fun getLines(): List<String> = lines

    fun getMaxLines(): Int = maxLines

    fun setMaxLines(maxLines: Int) = apply { this.maxLines = maxLines }

    fun getScale(): Float = scale

    fun setScale(scale: Float) = apply { this.scale = scale }

    /**
     * Sets the maximum width of the text, splitting it into multiple lines if necessary.
     *
     * @param maxWidth the maximum width of the text
     * @return the Text object for method chaining
     */
    fun setMaxWidth(maxWidth: Int) = apply {
        this.maxWidth = maxWidth
        updateFormatting()
    }

    fun getMaxWidth(): Int = maxWidth

    fun getHeight(): Float {
        return if (lines.size > 1)
            lines.size.coerceAtMost(maxLines) * scale * 10
        else scale * 10
    }

    fun exceedsMaxLines(): Boolean {
        return lines.size > maxLines
    }

    @JvmOverloads
    fun draw(x: Float? = null, y: Float? = null) = apply {
        draw(x, y, null, null)
    }

    internal fun draw(x: Float? = null, y: Float? = null, backgroundX: Float? = null, backgroundWidth: Float? = null) =
        apply {
            Renderer.pushMatrix()
            Renderer.enableBlend()
            Renderer.scale(scale, scale, scale)

            var longestLine = lines.maxOf { Renderer.getStringWidth(it) * scale }
            if (maxWidth != 0)
                longestLine = longestLine.coerceAtMost(maxWidth.toFloat())
            width = longestLine

            var yHolder = y ?: this.y

            val xHolder = when (align) {
                Align.CENTER -> (x ?: this.x) - width / 2
                Align.RIGHT -> (x ?: this.x) - width
                else -> x ?: this.x
            }

            if (background)
                Renderer.drawRect(
                    backgroundColor,
                    backgroundX ?: xHolder,
                    yHolder,
                    backgroundWidth ?: width,
                    getHeight()
                )

            for (i in 0 until maxLines) {
                if (i >= lines.size) break
                Renderer.drawString(lines[i], xHolder, yHolder, color, shadow)
                yHolder += scale * 10
            }
            Renderer.disableBlend()
            Renderer.popMatrix()
        }
    private fun updateFormatting() {
        string =
            if (formatted) ChatLib.addColor(string)
            else ChatLib.replaceFormatting(string)

        lines.clear()

        string.split("\n").forEach { line ->
            if (maxWidth > 0) {
                lines.addAll(
                    Renderer.getFontRenderer().textHandler.wrapLines(line, maxWidth, Style.EMPTY).map { it.string }
                )
            } else {
                lines.add(line)
            }
        }
    }

    override fun toString() =
        "Text{" +
            "string=$string, x=$x, y=$y, " +
            "lines=$lines, color=$color, scale=$scale, " +
            "formatted=$formatted, shadow=$shadow, " + //align=$align, " +
            "width=$width, maxWidth=$maxWidth, maxLines=$maxLines" +
            "}"

    enum class Align {
        LEFT, CENTER, RIGHT
    }
}
