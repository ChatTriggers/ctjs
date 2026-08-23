package com.chattriggers.ctjs.api.render

import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.engine.printTraceToConsole
import com.chattriggers.ctjs.internal.engine.JSLoader
import com.chattriggers.ctjs.internal.lifecycle.GenerationGuard
import com.chattriggers.ctjs.internal.lifecycle.OwnedKind
import com.chattriggers.ctjs.internal.utils.getOrNull
import com.chattriggers.ctjs.internal.utils.toIdentifier
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.toasts.ToastManager
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import org.mozilla.javascript.Callable
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.Undefined
import net.minecraft.client.gui.components.toasts.Toast as MCToast

// https://github.com/Edgeburn/Toasts
class Toast(config: NativeObject) : MCToast {
    private var titleBacker: TextComponent? = null
    var title: Any?
        get() = titleBacker
        set(value) { titleBacker = value?.let { TextComponent(it) } }

    private var descriptionBacker: TextComponent? = null
    var description: Any?
        get() = descriptionBacker
        set(value) { descriptionBacker = value?.let { TextComponent(it) } }

    private var backgroundBacker: Identifier? = DEFAULT_BACKGROUND
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
    } ?: MCToast.DEFAULT_WIDTH

    private var toastHeight = config.getOrNull("height")?.let {
        require(it is Number) { "Toast \"height\" must be a number" }
        it.toInt()
    } ?: MCToast.SLOT_HEIGHT

    var displayTime = config.getOrNull("displayTime")?.let {
        require(it is Number) { "Toast \"displayTime\" must be a number" }
        it.toLong()
    } ?: 5000L

    private var customRenderFunction = config.getOrNull("render")?.let {
        check(it is Callable) { "Toast \"render\" function must be undefined or callable" }
        it
    }
    private var jsReceiver = if (customRenderFunction != null) {
        Context.javaToJS(this, Context.getContext().topCallScope) as Scriptable
    } else null
    private var wantedVisibility = MCToast.Visibility.SHOW
    private val generation = GenerationGuard(OwnedKind.UI, onInvalidate = ::invalidateGeneration)

    init {
        title = config.getOrNull("title")
        description = config.getOrNull("description")
        background = config.getOrNull("background") ?: backgroundBacker
        icon = config.getOrNull("icon")
        generation.register()
    }

    override fun width() = toastWidth
    override fun height() = toastHeight
    override fun getWantedVisibility() = wantedVisibility

    override fun update(manager: ToastManager, fullyVisibleForMs: Long) {
        if (fullyVisibleForMs >= displayTime * manager.notificationDisplayTimeMultiplier) {
            wantedVisibility = MCToast.Visibility.HIDE
        } else {
            wantedVisibility = if (generation.isActive()) MCToast.Visibility.SHOW else MCToast.Visibility.HIDE
        }
    }

    fun show() = apply {
        if (generation.isActive()) {
            wantedVisibility = MCToast.Visibility.SHOW
            Client.getMinecraft().toastManager.addToast(this)
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, font: Font, fullyVisibleForMs: Long) {
        if (customRenderFunction != null) {
            val invoked = generation.execute {
                Renderer.withGuiGraphics(graphics, Renderer.partialTicks) {
                    try {
                        JSLoader.invoke(customRenderFunction!!, emptyArray(), thisObj = jsReceiver!!)
                    } catch (e: Throwable) {
                        e.printTraceToConsole()
                        customRenderFunction = Callable { _, _, _, _ -> Undefined.instance }
                    }
                }
            }
            if (!invoked)
                wantedVisibility = MCToast.Visibility.HIDE
            return
        }

        backgroundBacker?.let {
            if (it == DEFAULT_BACKGROUND) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, it, 0, 0, width(), height())
            } else {
                graphics.blit(RenderPipelines.GUI_TEXTURED, it, 0, 0, 0f, 0f, width(), height(), width(), height())
            }
        }

        iconBacker?.let {
            val iconSize = height() - ICON_PADDING * 2
            graphics.blit(RenderPipelines.GUI_TEXTURED, it, ICON_PADDING, ICON_PADDING, 0f, 0f, iconSize, iconSize, iconSize, iconSize)
        }

        val textX = if (iconBacker == null) ICON_PADDING else height()
        var textY = ICON_PADDING
        titleBacker?.let {
            graphics.text(font, it, textX, textY, 0xffffff, false)
            textY += font.lineHeight + 1
        }
        descriptionBacker?.let {
            graphics.text(font, it, textX, textY, 0xffffff, false)
        }
    }

    private fun invalidateGeneration() {
        customRenderFunction = null
        jsReceiver = null
        wantedVisibility = MCToast.Visibility.HIDE
    }

    private companion object {
        private const val ICON_PADDING = 7
        private val DEFAULT_BACKGROUND = Identifier.withDefaultNamespace("toast/advancement")

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
