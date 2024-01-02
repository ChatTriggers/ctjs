package com.chattriggers.ctjs.api.render

import com.chattriggers.ctjs.internal.utils.getOption
import org.mozilla.javascript.NativeObject
import java.util.concurrent.CopyOnWriteArrayList

class Display() {
    private var lines = CopyOnWriteArrayList<Text>()

    private var x = 0f
    private var y = 0f
    private var order = Order.NORMAL

    private var backgroundColor: Long = 0x50000000
    private var textColor: Long = 0xffffffff
    private var background = Background.NONE
    private var align = Text.Align.LEFT

    private var minWidth = 0f
    private var width = 0f
    private var height = 0f

    constructor(config: NativeObject?) : this() {
        setBackgroundColor(config.getOption("backgroundColor", 0x50000000).toLong())
        setTextColor(config.getOption("textColor", 0xffffffff).toLong())
        setBackground(config.getOption("background", Background.NONE))
        setAlign(config.getOption("align", Text.Align.LEFT))
        setOrder(config.getOption("order", Order.NORMAL))
        setX(config.getOption("x", 0f).toFloat())
        setY(config.getOption("y", 0f).toFloat())
        setMinWidth(config.getOption("minWidth", 0f).toFloat())
    }

    fun getTextColor(): Long = textColor

    /**
     * Sets the color of the texts
     *
     * Overrides the color of the individual texts
     */
    fun setTextColor(textColor: Long) = apply {
        this.textColor = textColor
    }

    fun getAlign(): Text.Align = align

    /**
     * Set the alignment of the texts in the display
     *
     * Overrides alignment of the individual texts
     */
    fun setAlign(align: Any) = apply {
        this.align = when (align) {
            is CharSequence -> Text.Align.valueOf(align.toString().uppercase())
            is Text.Align -> align
            else -> Text.Align.LEFT
        }
    }

    fun getOrder(): Order = order

    fun setOrder(order: Any) = apply {
        this.order = when (order) {
            is CharSequence -> Order.valueOf(order.toString().uppercase())
            is Order -> order
            else -> Order.NORMAL
        }
    }

    fun getBackground(): Background = background

    fun setBackground(background: Any) = apply {
        this.background = when (background) {
            is CharSequence -> Background.valueOf(background.toString().uppercase().replace(" ", "_"))
            is Background -> background
            else -> Background.NONE
        }
    }

    fun getBackgroundColor(): Long = backgroundColor

    fun setBackgroundColor(backgroundColor: Long) = apply {
        this.backgroundColor = backgroundColor
    }

    fun setLine(index: Int, line: Any) = apply {
        while (lines.size - 1 < index)
            lines.add(Text(""))

        when (line) {
            is CharSequence -> lines[index].setString(line.toString())
            is Text -> lines[index] = line
            else -> lines[index] = Text("")
        }
    }

    fun getLine(index: Int): Text = lines[index]

    fun getLines(): List<Text> = lines

    fun setLines(lines: MutableList<Text>) = apply {
        this.lines = CopyOnWriteArrayList(lines)
    }

    fun addLine(line: Any) = apply {
        setLine(this.lines.size, line)
    }

    fun addLines(vararg lines: Any) = apply {
        lines.forEach { addLine(it) }
    }

    fun removeLine(index: Int) = apply {
        lines.removeAt(index)
    }

    fun clearLines() = apply {
        lines.clear()
    }

    fun getX(): Float = x

    fun setX(x: Float) = apply { this.x = x }

    fun getY(): Float = y

    fun setY(y: Float) = apply { this.y = y }

    fun getWidth(): Float = width

    fun getHeight(): Float = height

    fun getMinWidth(): Float = minWidth

    fun setMinWidth(minWidth: Float) = apply {
        this.minWidth = minWidth
    }

    fun draw() {
        width = lines.maxOfOrNull { it.getWidth() }?.coerceAtLeast(minWidth) ?: minWidth

        val textBackgroundWidth = when (background) {
            Background.FULL -> width
            Background.PER_LINE -> width
            Background.NONE -> null
        }

        var currentHeight = 0f

        val linesX = when (align) {
            Text.Align.CENTER -> x + width / 2
            Text.Align.RIGHT -> x + width
            else -> x
        }

        val linesToDraw = when (order) {
            Order.NORMAL -> lines
            Order.REVERSED -> lines.asReversed()
        }

        linesToDraw.forEach {
            if (background === Background.FULL)
                it
                    .setBackground(true)
                    .setBackgroundColor(backgroundColor)

            it
                .setColor(textColor)
                .setAlign(align)
                .draw(linesX, y + currentHeight, x, textBackgroundWidth)

            currentHeight += it.getHeight()
        }

        height = currentHeight
    }

    override fun toString() =
        "Display{" +
            "renderX=$x, renderY=$y, " +
            "background=$background, backgroundColor=$backgroundColor, " +
            "textColor=$textColor, align=$align, order=$order, " +
            "minWidth=$minWidth, width=$width, height=$height, " +
            "lines=$lines" +
            "}"

    enum class Background {
        NONE, FULL, PER_LINE
    }

    enum class Order {
        REVERSED, NORMAL
    }
}
