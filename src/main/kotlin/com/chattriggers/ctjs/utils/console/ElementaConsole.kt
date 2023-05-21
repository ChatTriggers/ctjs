package com.chattriggers.ctjs.utils.console

import com.chattriggers.ctjs.Reference
import com.chattriggers.ctjs.engine.ILoader
import com.chattriggers.ctjs.minecraft.wrappers.Client
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.components.input.UIMultilineTextInput
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.universal.UMinecraft
import gg.essential.universal.UResolution
import org.mozilla.javascript.RhinoException
import java.awt.Color

class ConsoleEntry(input: String?, output: String?, maxWidthComponent: UIComponent) : UIBlock(Color(50, 50, 50)) {
    private val inputCursor by UIText(">>") constrain {
        x = 5.pixels()
        y = 5.pixels()
    }

    private val inputContent by UIWrappedText(input ?: "", shadow = false) constrain {
        x = SiblingConstraint(5f)
        y = 5.pixels()
        width = max(width, 10.pixels())
    }

    private val outputContent by UIWrappedText(output?.replace("\t", "    ") ?: "") constrain {
        x = 5.pixels()
        y = max(SiblingConstraint(5f), 5.pixels())
        width = max(width, 10.pixels())
    }

    init {
        require(input != null || output != null)

        constrain {
            y = SiblingConstraint(5f)
            width = max(ChildBasedSizeConstraint() + 15.pixels(), 100.percent().apply {
                constrainTo = maxWidthComponent
            })
            height = ChildBasedRangeConstraint() + 10.pixels()
        }

        if (input != null) {
            inputCursor childOf this
            inputContent childOf this
        }

        if (output != null)
            outputContent childOf this
    }
}

class ConsoleComponent(private val loader: ILoader?) : WindowScreen(ElementaVersion.V2, drawDefaultBackground = false, restoreCurrentGuiOnClose = true) {
    private val background by UIBlock(Color(30, 30, 30)) constrain {
        x = basicXConstraint {
            if (UMinecraft.getMinecraft().window == null)
                return@basicXConstraint 0f

            if (UResolution.scaledWidth < 300) 0f else (UResolution.scaledWidth * 0.05f - 5f)
        }
        y = 0.pixels()

        width = basicWidthConstraint {
            if (UMinecraft.getMinecraft().window == null)
                return@basicWidthConstraint 0f

            if (UResolution.scaledWidth < 300) {
                UResolution.scaledWidth.toFloat()
            } else UResolution.scaledWidth.toFloat() * 0.9f
        }

        height = ChildBasedSizeConstraint() + 10.pixels()
    } childOf window

    private val backgroundInnerBlock by UIBlock(Color(30, 30, 30)) constrain {
        x = 5.pixels()
        y = 5.pixels()
        width = 100.percent() - 10.pixels()
        height = ChildBasedSizeConstraint()
    } effect ScissorEffect() childOf background

    private val backgroundInner by ScrollComponent(horizontalScrollEnabled = true) constrain {
        height = height.coerceAtMost(basicHeightConstraint {
            if (UMinecraft.getMinecraft().window == null)
                return@basicHeightConstraint 0f

            UResolution.scaledHeight * 0.8f
        })
    } childOf backgroundInnerBlock

    private val textBackground by UIBlock(Color(50, 50, 50)) constrain {
        x = 5.pixels()
        y = max(SiblingConstraint(5f), 5.pixels())
        width = 100.percent() - 10.pixels()
        height = ChildBasedMaxSizeConstraint() + 10.pixels()
    }

    private val cursor by UIText(">>") constrain {
        x = 5.pixels()
        y = 5.pixels()
    } childOf textBackground

    private val textInput by UIMultilineTextInput(shadow = false).constrain {
        x = SiblingConstraint(5f)
        y = 5.pixels()
        width = FillConstraint() - 5.pixels()
    }.setMaxLines(10) childOf textBackground

    init {
        backgroundInnerBlock.hide(instantly = true)

        if (loader != null) {
            textBackground childOf background
            textBackground.onMouseClick { textInput.grabWindowFocus() }

            textInput.onActivate { input ->
                val entry = try {
                    val output = loader!!.eval(input).let { if (it == "undefined") null else it }
                    ConsoleEntry(input, output, backgroundInnerBlock)
                } catch (e: RhinoException) {
                    ConsoleEntry(input, "&c${e.details()}", backgroundInnerBlock)
                } catch (e: Throwable) {
                    makeErrorEntry(e, input)
                }

                addErrorEntry(entry)

                textInput.setText("")
                scrollToLastInput()
            }
        }

        window.onMouseClick {
            if (it.target == window)
                close()
        }

        background.animateBeforeHide {
            setYAnimation(Animations.OUT_EXP, 0.5f, 0.pixels(alignOutside = true))
        }

        background.animateAfterUnhide {
            setYAnimation(Animations.OUT_EXP, 0.5f, 0.pixels())
        }
    }

    fun clear() {
        backgroundInner.clearChildren()
        backgroundInnerBlock.hide()
        background.constrain {
            height = ChildBasedSizeConstraint() + 10.pixels()
        }
    }

    fun printStackTrace(error: Throwable) = addErrorEntry(makeErrorEntry(error))

    override fun onDisplayed() {
        textInput.grabWindowFocus()
    }

    private fun makeErrorEntry(error: Throwable, input: String? = null): ConsoleEntry {
        val errorText = "&c" + error.stackTraceToString().replace("\n", "\n&c")
        return ConsoleEntry(input, errorText, backgroundInnerBlock)
    }

    private fun addErrorEntry(entry: ConsoleEntry) {
        backgroundInnerBlock.unhide()
        background.constrain {
            height = ChildBasedSizeConstraint() + 15.pixels()
        }
        entry childOf backgroundInner
    }

    private fun scrollToLastInput() {
        val actualHeight = backgroundInner.children[0].children.sumOf { c -> c.getHeight().toDouble() }.toFloat()
        backgroundInner.scrollTo(verticalOffset = -actualHeight + backgroundInner.children.last().getHeight())
    }
}

class ElementaConsole(private val loader: ILoader?) : Console {
    private lateinit var component: ConsoleComponent

    override fun clear() {
        if (ensureInitialized())
            component.clear()
    }

    override fun println(obj: Any, logType: LogType, end: String, customColor: Color?) {
        kotlin.io.println(obj)
    }

    override fun printStackTrace(error: Throwable) {
        if (ensureInitialized())
            component.printStackTrace(error)
    }

    override fun show() {
        if (ensureInitialized())
            Client.currentGui.set(component)
    }

    override fun onConsoleSettingsChanged() {
    }

    private fun ensureInitialized(): Boolean {
        if (Client.getMinecraft().window == null)
            return false

        if (!::component.isInitialized)
            component = ConsoleComponent(loader)

        return true
    }
}
