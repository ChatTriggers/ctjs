package com.chattriggers.ctjs.api.render

import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.engine.printTraceToConsole
import com.chattriggers.ctjs.internal.engine.JSLoader
import com.chattriggers.ctjs.internal.utils.getOrNull
import com.chattriggers.ctjs.internal.utils.toIdentifier
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.toast.ToastManager
import net.minecraft.util.Identifier
import org.mozilla.javascript.*
import net.minecraft.client.toast.Toast

// https://github.com/Edgeburn/Toasts
/**
 * Displays a toast in the top left corner similar to the MC advancement toast
 *
 * Object properties that can be passed to the constructor:
 * - title: A TextComponent (or anything that can be passed to the TextComponent constructor)
 * - description: A TextComponent (or anything that can be passed to the TextComponent constructor)
 * - background: An Image or a String/Identifier that points to a texture. Defaults to the advancement background
 * - icon: An Image or a String/Identifier that points to a texture
 * - width: The width of the toast, defaults to 160
 * - height: The height of the toast, defaults to 32
 * - displayTime: The time in ms the toast will be displayed, defaults to 5000
 * - render: An optional function that will be called to render the toast. By default, it renders the same
 *           way that advancement toasts do. If this function is called, it will not render anything by default.
 *           It takes no parameters and is called with the Toast object as its receiver.
 */
class Toast(config: NativeObject) : Toast {
    private var titleBacker: TextComponent? = null
    var title: Any?
        get() = titleBacker
        set(value) { titleBacker = value?.let { TextComponent(it) } }

    private var descriptionBacker: TextComponent? = null
    var description: Any?
        get() = descriptionBacker
        set(value) { descriptionBacker = value?.let { TextComponent(it) } }

    private var backgroundBacker: Identifier? = Identifier("textures/gui/sprites/toast/advancement.png")
    var background: Any?
        get() = backgroundBacker
        set(value) { backgroundBacker = toIdentifier(value) }

    private var iconBacker: Identifier? = null
    var icon: Any?
        get() = iconBacker
        set(value) { iconBacker = toIdentifier(value) }

    private var toastWidth = config.getOrNull("width")?.let {
        require(it is Number) { "Toast \"width\" must be a number" }
        it.toInt()
    } ?: super.getWidth()

    private var toastHeight = config.getOrNull("height")?.let {
        require(it is Number) { "Toast \"height\" must be a number" }
        it.toInt()
    } ?: super.getHeight()

    var displayTime = config.getOrNull("displayTime")?.let {
        require(it is Number) { "Toast \"displayTime\" must be a number" }
        it.toLong()
    } ?: 5000L

    private var customRenderFunction = config.getOrNull("render")?.let {
        check(it is Callable) { "Toast \"render\" function must be undefined or callable" }
        it
    }
    private val jsReceiver = if (customRenderFunction != null) {
        Context.javaToJS(this, Context.getContext().topCallScope) as Scriptable
    } else null

    init {
        title = config.getOrNull("title")
        description = config.getOrNull("description")
        background = config.getOrDefault("background", backgroundBacker)
        icon = config.getOrNull("icon")
    }

    override fun getWidth() = toastWidth
    override fun getHeight() = toastHeight

    fun show() = apply {
        Client.getMinecraft().toastManager.add(this)
    }

    override fun draw(context: DrawContext, manager: ToastManager, startTime: Long): Toast.Visibility {
        if (customRenderFunction != null) {
            Renderer.withMatrix(context.matrices) {
                try {
                    JSLoader.invoke(customRenderFunction!!, emptyArray(), thisObj = jsReceiver!!)
                } catch (e: Throwable) {
                    e.printTraceToConsole()

                    // If the method threw, don't invoke it again
                    customRenderFunction = Callable { _, _, _, _ -> Undefined.instance }
                }
            }
        } else {
            backgroundBacker?.let {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
                context.drawTexture(it, 0, 0, 0.0f, 0.0f, width, height, width, height)
            }

            iconBacker?.let {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
                val iconSize = height - ICON_PADDING * 2
                context.drawTexture(it, ICON_PADDING, ICON_PADDING, 0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize)
            }

            val textX = if (icon == null) ICON_PADDING else height
            var textY = ICON_PADDING
            val textRenderer = Client.getMinecraft().textRenderer

            titleBacker?.let {
                context.drawText(textRenderer, it, textX, textY, 0xffffff, false)
                textY += textRenderer.fontHeight + 1
            }

            descriptionBacker?.let {
                context.drawText(textRenderer, it, textX, textY, 0xffffff, false)
            }
        }

        return if (startTime > displayTime) Toast.Visibility.HIDE else Toast.Visibility.SHOW
    }

    private companion object {
        private const val ICON_PADDING = 7

        private fun toIdentifier(value: Any?): Identifier? = when (value) {
            is Image -> value.getIdOrRegister()
            is CharSequence -> value.toString().toIdentifier()
            is Identifier -> value
            null -> null
            else -> throw IllegalArgumentException(
                "Toast \"background\" must be an Image or a string corresponding to a resource identifier"
            )
        }
    }
}
