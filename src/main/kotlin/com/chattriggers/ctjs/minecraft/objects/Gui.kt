package com.chattriggers.ctjs.minecraft.objects

import com.chattriggers.ctjs.engine.ILoader
import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer
import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.mixins.ClickableWidgetAccessor
import com.chattriggers.ctjs.triggers.RegularTrigger
import com.chattriggers.ctjs.triggers.TriggerType
import com.chattriggers.ctjs.utils.asMixin
import gg.essential.universal.UMatrixStack
import gg.essential.universal.wrappers.message.UTextComponent
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.util.math.MatrixStack

// TODO(breaking): Removed GuiHandler (does the same thing as Client.currentGui.set())
abstract class Gui(title: UTextComponent) : Screen(title) {
    private var onDraw: RegularTrigger? = null
    private var onClick: RegularTrigger? = null
    private var onScroll: RegularTrigger? = null
    private var onKeyTyped: RegularTrigger? = null
    private var onMouseReleased: RegularTrigger? = null
    private var onMouseDragged: RegularTrigger? = null
    private var onActionPerformed: RegularTrigger? = null
    private var onOpened: RegularTrigger? = null
    private var onClosed: RegularTrigger? = null

    private var mouseX = 0
    private var mouseY = 0

    private val buttons = mutableMapOf<Int, ButtonWidget>()
    private var nextButtonId = 0
    private var doesPauseGame = false

    init {
        ScreenMouseEvents.afterMouseScroll(this).register { _, x, y, _, dy ->
            // TODO: Pass in dx?
            onScroll?.trigger(arrayOf(x, y, dy))
        }
    }

    fun open() {
        Client.currentGui.set(this)
    }

    override fun close() {
        Client.currentGui.set(null)

    }

    fun isOpen(): Boolean = Client.getMinecraft().currentScreen === this

    fun isControlDown(): Boolean = isControlDown()

    fun isShiftDown(): Boolean = isShiftDown()

    fun isAltDown(): Boolean = isAltDown()

    /**
     * Registers a method to be run while gui is open.
     * Registered method runs on draw.
     * Arguments passed through to method:
     * - int mouseX
     * - int mouseY
     * - float partialTicks
     *
     * @param method the method to run
     * @return the trigger
     */
    fun registerDraw(method: Any) = run {
        onDraw = RegularTrigger(method, TriggerType.Other, getLoader())
        onDraw
    }

    /**
     * Registers a method to be run while gui is open.
     * Registered method runs on mouse click.
     * Arguments passed through to method:
     * - int mouseX
     * - int mouseY
     * - int button
     *
     * @param method the method to run
     * @return the trigger
     */
    fun registerClicked(method: Any) = run {
        onClick = RegularTrigger(method, TriggerType.Other, getLoader())
        onClick
    }

    /**
     * Registers a method to be run while the gui is open.
     * Registered method runs on mouse scroll.
     * Arguments passed through to method:
     * - int mouseX
     * - int mouseY
     * - int scroll direction
     */
    fun registerScrolled(method: Any) = run {
        onScroll = RegularTrigger(method, TriggerType.Other, getLoader())
        onScroll
    }

    /**
     * Registers a method to be run while gui is open.
     * Registered method runs on key input.
     * Arguments passed through to method:
     * - char typed character
     * - int key code
     *
     * @param method the method to run
     * @return the trigger
     */
    fun registerKeyTyped(method: Any) = run {
        onKeyTyped = RegularTrigger(method, TriggerType.Other, getLoader())
        onKeyTyped
    }

    /**
     * Registers a method to be run while gui is open.
     * Registered method runs on key input.
     * Arguments passed through to method:
     * - int mouseX
     * - int mouseY
     * - int clickedMouseButton
     * - long timeSinceLastClick
     *
     * @param method the method to run
     * @return the trigger
     */
    fun registerMouseDragged(method: Any) = run {
        onMouseDragged = RegularTrigger(method, TriggerType.Other, getLoader())
        onMouseDragged
    }

    /**
     * Registers a method to be run while gui is open.
     * Registered method runs on mouse release.
     * Arguments passed through to method:
     * - int mouseX
     * - int mouseY
     * - int button
     *
     * @param method the method to run
     * @return the trigger
     */
    fun registerMouseReleased(method: Any) = run {
        onMouseReleased = RegularTrigger(method, TriggerType.Other, getLoader())
        onMouseReleased
    }

    /**
     * Registers a method to be run while gui is open.
     * Registered method runs when an action is performed (clicking a button)
     * Arguments passed through to method:
     * - the button that is clicked
     *
     * @param method the method to run
     * @return the trigger
     */
    fun registerActionPerformed(method: Any) = run {
        onActionPerformed = RegularTrigger(method, TriggerType.Other, getLoader())
        onActionPerformed
    }

    /**
     * Registers a method to be run when the gui is opened.
     * Arguments passed through to method:
     * - the gui that is opened
     *
     * @param method the method to run
     * @return the trigger
     */
    fun registerOpened(method: Any) = run {
        onOpened = RegularTrigger(method, TriggerType.Other, getLoader())
        onOpened
    }

    /**
     * Registers a method to be run when the gui is closed.
     * Arguments passed through to method:
     * - the gui that is closed
     *
     * @param method the method to run
     * @return the trigger
     */
    fun registerClosed(method: Any) = run {
        onClosed = RegularTrigger(method, TriggerType.Other, getLoader())
        onClosed
    }

    override fun init() {
        super.init()
        buttons.values.forEach(::addDrawable)
        onOpened?.trigger(arrayOf(this))
    }

    /**
     * Internal method to run trigger. Not meant for public use
     */
    override fun removed() {
        super.removed()
        onClosed?.trigger(arrayOf(this))
    }

    /**
     * Internal method to run trigger. Not meant for public use
     */
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return super.mouseClicked(mouseX, mouseY, button).also {
            onClick?.trigger(arrayOf(mouseX, mouseY, button))
        }
    }

    /**
     * Internal method to run trigger. Not meant for public use
     */
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return super.mouseReleased(mouseX, mouseY, button).also {
            onMouseReleased?.trigger(arrayOf(mouseX, mouseY, button))
        }
    }

    /**
     * Internal method to run trigger. Not meant for public use
     */
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY).also {
            // TODO(breaking): Removed timeSinceLastClick (could be readded)
            onMouseDragged?.trigger(arrayOf(mouseX, mouseY, button))
        }
    }

    /**
     * Internal method to run trigger. Not meant for public use
     */
    override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(matrices, mouseX, mouseY, delta)

        Renderer.matrixStack = UMatrixStack(matrices)
        Renderer.partialTicks = delta
        Renderer.pushMatrix()

        this.mouseX = mouseX
        this.mouseY = mouseY
        onDraw?.trigger(arrayOf(mouseX, mouseY, delta))

        Renderer.popMatrix()
    }

    /**
     * Internal method to run trigger. Not meant for public use
     */
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return super.keyPressed(keyCode, scanCode, modifiers).also {
            onKeyTyped?.trigger(arrayOf(keyCode.toChar(), scanCode))
        }
    }

    /**
     * Internal method to run trigger. Not meant for public use
     */

    override fun shouldPause() = doesPauseGame

    fun setDoesPauseGame(doesPauseGame: Boolean) = apply { this.doesPauseGame = doesPauseGame }

    /**
     * Add a base Minecraft button to the gui
     *
     * @param button the button to add
     * @return the button ID for use in actionPerformed
     */
    // TODO(breaking): Return ID instead of this
    fun addButton(button: ButtonWidget): Int {
        val id = nextButtonId++
        buttons[id] = button
        addDrawable(button)
        return id
    }

    /**
     * Add a base Minecraft button to the gui
     *
     * @param x the x position of the button
     * @param y the y position of the button
     * @param width the width of the button
     * @param height the height of the button
     * @param buttonText the label of the button
     * @return the button ID for use in actionPerformed
     */
    // TODO(breaking): Return ID instead of this and doesn't accept an ID
    @JvmOverloads
    fun addButton(x: Int, y: Int, width: Int = 200, height: Int = 20, buttonText: UTextComponent): Int {
        val id = nextButtonId++
        val button = ButtonWidget.builder(buttonText) {
            onActionPerformed?.trigger(arrayOf(id))
        }.dimensions(x, y, width, height).build()
        buttons[id] = button
        addDrawable(button)
        return id
    }

    fun addButton(x: Int, y: Int, width: Int = 200, height: Int = 20, buttonText: String) =
        addButton(x, y, width, height, UTextComponent(buttonText))

    /**
     * Removes a button from the gui with the given id
     *
     * @param buttonId the id of the button to remove
     * @return the Gui for method chaining
     */
    fun removeButton(buttonId: Int) = apply {
        remove(buttons[buttonId] ?: return@apply)
        buttons.remove(buttonId)
    }

    fun clearButtons() = apply {
        buttons.values.forEach(::remove)
        buttons.clear()
    }

    // TODO: Should we even have this? Maybe add a wrapper?
    fun getButton(buttonId: Int): ButtonWidget? = buttons[buttonId]

    fun getButtonVisibility(buttonId: Int): Boolean = getButton(buttonId)?.visible ?: false

    /**
     * Sets the visibility of a button
     *
     * @param buttonId the id of the button to change
     * @param visible the new visibility of the button
     * @return the Gui for method chaining
     */
    fun setButtonVisibility(buttonId: Int, visible: Boolean) = apply {
        getButton(buttonId)?.visible = visible
    }

    fun getButtonEnabled(buttonId: Int): Boolean = getButton(buttonId)?.active ?: false

    /**
     * Sets the enabled state of a button
     *
     * @param buttonId the id of the button to set
     * @param enabled the enabled state of the button
     * @return the Gui for method chaining
     */
    fun setButtonEnabled(buttonId: Int, enabled: Boolean) = apply {
        getButton(buttonId)?.active = enabled
    }

    fun getButtonWidth(buttonId: Int): Int = getButton(buttonId)?.width ?: 0

    /**
     * Sets the button's width. Button textures break if the width is greater than 200
     *
     * @param buttonId id of the button
     * @param width the new width
     * @return the Gui for method chaining
     */
    fun setButtonWidth(buttonId: Int, width: Int) = apply {
        getButton(buttonId)?.width = width
    }

    fun getButtonHeight(buttonId: Int): Int = getButton(buttonId)?.height ?: 0

    /**
     * Sets the button's height. Button textures break if the height is not 20
     *
     * @param buttonId id of the button
     * @param height the new height
     * @return the Gui for method chaining
     */
    fun setButtonHeight(buttonId: Int, height: Int) = apply {
        getButton(buttonId)?.asMixin<ClickableWidgetAccessor>()?.setHeight(height)
    }

    fun getButtonX(buttonId: Int): Int = getButton(buttonId)?.x ?: 0

    /**
     * Sets the button's x position
     *
     * @param buttonId id of the button
     * @param x the new x position
     * @return the Gui for method chaining
     */
    fun setButtonX(buttonId: Int, x: Int) = apply {
        getButton(buttonId)?.x = x
    }

    fun getButtonY(buttonId: Int): Int = getButton(buttonId)?.y ?: 0

    /**
     * Sets the button's y position
     *
     * @param buttonId id of the button
     * @param y the new y position
     * @return the Gui for method chaining
     */
    fun setButtonY(buttonId: Int, y: Int) = apply {
        getButton(buttonId)?.y = y
    }

    /**
     * Sets the button's position
     *
     * @param buttonId id of the button
     * @param x the new x position
     * @param y the new y position
     * @return the Gui for method chaining
     */
    fun setButtonLoc(buttonId: Int, x: Int, y: Int) = apply {
        getButton(buttonId)?.apply {
            this.x = x
            this.y = y
        }
    }

    // TODO(breaking): Deleted completely pointless drawing methods that had no reason to be here

    internal abstract fun getLoader(): ILoader
}
