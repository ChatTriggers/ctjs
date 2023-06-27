package com.chattriggers.ctjs.minecraft.objects.display

import com.chattriggers.ctjs.minecraft.CTEvents
import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer
import com.chattriggers.ctjs.utils.Initializer
import com.chattriggers.ctjs.utils.getOption
import gg.essential.universal.UMatrixStack
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import org.mozilla.javascript.NativeObject
import java.util.concurrent.CopyOnWriteArrayList

class Display() {
    private var lines = CopyOnWriteArrayList<DisplayLine>()

    private var renderX = 0f
    private var renderY = 0f
    private var shouldRender = true
    private var order = Order.DOWN

    private var backgroundColor: Int = 0x50000000
    private var textColor: Int = 0xffffffff.toInt()
    private var background = Background.NONE
    private var align = Align.LEFT

    private var minWidth = 0f
    private var width = 0f
    private var height = 0f

    internal var registerType = RegisterType.RENDER_OVERLAY

    init {
        @Suppress("LeakingThis")
        displays.add(this)
    }

    constructor(config: NativeObject?) : this() {
        setBackgroundColor(config.getOption("backgroundColor", 0x50000000).toInt())
        setTextColor(config.getOption("textColor", 0xffffffff).toInt())
        setBackground(config.getOption("background", Background.NONE))
        setAlign(config.getOption("align", Align.LEFT))
        setOrder(config.getOption("order", Order.DOWN))
        setRenderX(config.getOption("renderX", 0f).toFloat())
        setRenderY(config.getOption("renderY", 0f).toFloat())
        setShouldRender(config.getOption("shouldRender", true).toBoolean())
        setMinWidth(config.getOption("minWidth", 0f).toFloat())
        setRegisterType(config.getOption("registerType", RegisterType.RENDER_OVERLAY))
    }

    fun getBackgroundColor(): Int = backgroundColor

    fun setBackgroundColor(backgroundColor: Int) = apply {
        this.backgroundColor = backgroundColor
    }

    fun getTextColor(): Int = textColor

    fun setTextColor(textColor: Int) = apply {
        this.textColor = textColor
    }

    fun getBackground(): Background = background

    fun setBackground(background: Any) = apply {
        this.background = when (background) {
            is String -> Background.valueOf(background.uppercase().replace(" ", "_"))
            is Background -> background
            else -> Background.NONE
        }
    }

    fun getAlign(): Align = align

    fun setAlign(align: Any) = apply {
        this.align = when (align) {
            is String -> Align.valueOf(align.uppercase())
            is Align -> align
            else -> Align.LEFT
        }
    }

    fun getOrder(): Order = order

    fun setOrder(order: Any) = apply {
        this.order = when (order) {
            is String -> Order.valueOf(order.uppercase())
            is Order -> order
            else -> Order.DOWN
        }
    }

    fun setLine(index: Int, line: Any) = apply {
        while (lines.size - 1 < index)
            lines.add(DisplayLine(""))

        lines[index] = when (line) {
            is String -> DisplayLine(line)
            is DisplayLine -> line
            else -> DisplayLine("")
        }
    }

    fun getLine(index: Int): DisplayLine = lines[index]

    fun getLines(): List<DisplayLine> = lines

    fun setLines(lines: MutableList<DisplayLine>) = apply {
        this.lines = CopyOnWriteArrayList(lines)
    }

    @JvmOverloads
    fun addLine(index: Int = -1, line: Any) = apply {
        val toAdd = when (line) {
            is String -> DisplayLine(line)
            is DisplayLine -> line
            else -> DisplayLine("")
        }

        if (index == -1) {
            lines.add(toAdd)
        } else lines.add(index, toAdd)
    }

    fun addLines(vararg lines: Any) = apply {
        this.lines.addAll(lines.map {
            when (it) {
                is String -> DisplayLine(it)
                is DisplayLine -> it
                else -> DisplayLine("")
            }
        })
    }

    fun removeLine(index: Int) = apply {
        lines.removeAt(index)
    }

    fun clearLines() = apply {
        lines.clear()
    }

    fun getRenderX(): Float = renderX

    fun setRenderX(renderX: Float) = apply {
        this.renderX = renderX
    }

    fun getRenderY(): Float = renderY

    fun setRenderY(renderY: Float) = apply {
        this.renderY = renderY
    }

    fun setRenderLoc(renderX: Float, renderY: Float) = apply {
        this.renderX = renderX
        this.renderY = renderY
    }

    fun getShouldRender(): Boolean = shouldRender

    fun setShouldRender(shouldRender: Boolean) = apply {
        this.shouldRender = shouldRender
        lines.forEach { it.shouldRender = shouldRender }
    }

    fun show() = apply {
        setShouldRender(true)
    }

    fun hide() = apply {
        setShouldRender(false)
    }

    fun getWidth(): Float = width

    fun getHeight(): Float = height

    fun getMinWidth(): Float = minWidth

    fun setMinWidth(minWidth: Float) = apply {
        this.minWidth = minWidth
    }

    /**
     * Gets the type of register the display will render under.
     *
     * The returned value will be a RegisterType
     *      renderOverlayEvent: render overlay
     *      postGuiRenderEvent: post gui render
     *
     * @return the register type
     */
    fun getRegisterType(): RegisterType = registerType

    /**
     * Sets the type of register the display will render under.
     *
     * Possible input values are:.
     *      renderOverlayEvent: render overlay
     *      postGuiRenderEvent: post gui render
     */
    fun setRegisterType(registerType: Any) = apply {
        this.registerType = when (registerType) {
            is String -> RegisterType.valueOf(registerType.uppercase().replace(" ", "_"))
            is RegisterType -> registerType
            else -> RegisterType.RENDER_OVERLAY
        }
    }

    fun render() {
        if (!shouldRender)
            return

        width = lines.maxOfOrNull { it.getTextWidth() }?.coerceAtLeast(minWidth) ?: minWidth

        var i = 0f
        lines.forEach {
            it.draw(renderX, renderY + i, width, background, backgroundColor, textColor, align)

            when (order) {
                Order.DOWN -> i += it.getText().getHeight()
                Order.UP -> i -= it.getText().getHeight()
            }
        }

        height = i
    }

    override fun toString() =
        "Display{" +
            "shouldRender=$shouldRender, registerType=$registerType, " +
            "renderX=$renderX, renderY=$renderY, " +
            "background=$background, backgroundColor=$backgroundColor, " +
            "textColor=$textColor, align=$align, order=$order, " +
            "minWidth=$minWidth, width=$width, height=$height, " +
            "lines=$lines" +
            "}"

    enum class RegisterType {
        RENDER_OVERLAY, POST_GUI_RENDER
    }

    enum class Background {
        NONE, FULL, PER_LINE
    }

    enum class Align {
        LEFT, CENTER, RIGHT
    }

    enum class Order {
        UP, DOWN
    }

    companion object : Initializer {
        private val displays = CopyOnWriteArrayList<Display>()

        fun clearDisplays() {
            displays.clear()
        }

        override fun init() {
            CTEvents.RENDER_OVERLAY.register { stack, partialTicks ->
                Renderer.matrixStack = UMatrixStack(stack)
                Renderer.partialTicks = partialTicks
                drawAll(RegisterType.RENDER_OVERLAY)

            }

            ScreenEvents.BEFORE_INIT.register { _, screen, _, _ ->
                ScreenEvents.afterRender(screen).register { _, stack, _, _, partialTicks ->
                    Renderer.matrixStack = UMatrixStack(stack)
                    Renderer.partialTicks = partialTicks
                    drawAll(RegisterType.POST_GUI_RENDER)
                }
            }
        }

        private fun drawAll(type: RegisterType) {
            Renderer.pushMatrix()
            displays.forEach {
                if (it.registerType == type)
                    it.render()
            }
            Renderer.popMatrix()
        }
    }
}
