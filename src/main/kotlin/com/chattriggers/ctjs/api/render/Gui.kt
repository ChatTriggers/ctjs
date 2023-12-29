package com.chattriggers.ctjs.api.render

import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.api.triggers.RegularTrigger
import com.chattriggers.ctjs.api.triggers.TriggerType
import com.chattriggers.ctjs.internal.mixins.ClickableWidgetAccessor
import com.chattriggers.ctjs.internal.utils.asMixin
import gg.essential.universal.UKeyboard
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UScreen
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ButtonWidget

class Gui @JvmOverloads constructor(
    title: TextComponent = TextComponent(""),
) : UScreen(unlocalizedName = title.formattedText) {
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

    fun open() {
        Client.currentGui.set(this)
    }

    override fun close() {
        Client.currentGui.set(null)

    }

    fun isOpen(): Boolean = Client.getMinecraft().currentScreen === this

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
    fun registerDraw(method: Any) = apply {
        onDraw = RegularTrigger(method, TriggerType.OTHER)
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
    fun registerClicked(method: Any) = apply {
        onClick = RegularTrigger(method, TriggerType.OTHER)
    }

    /**
     * Registers a method to be run while the gui is open.
     * Registered method runs on mouse scroll.
     * Arguments passed through to method:
     * - int mouseX
     * - int mouseY
     * - int scroll direction
     */
    fun registerScrolled(method: Any) = apply {
        onScroll = RegularTrigger(method, TriggerType.OTHER)
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
    fun registerKeyTyped(method: Any) = apply {
        onKeyTyped = RegularTrigger(method, TriggerType.OTHER)
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
    fun registerMouseDragged(method: Any) = apply {
        onMouseDragged = RegularTrigger(method, TriggerType.OTHER)
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
    fun registerMouseReleased(method: Any) = apply {
        onMouseReleased = RegularTrigger(method, TriggerType.OTHER)
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
    fun registerActionPerformed(method: Any) = apply {
        onActionPerformed = RegularTrigger(method, TriggerType.OTHER)
    }

    /**
     * Registers a method to be run when the gui is opened.
     * Arguments passed through to method:
     * - the gui that is opened
     *
     * @param method the method to run
     * @return the trigger
     */
    fun registerOpened(method: Any) = apply {
        onOpened = RegularTrigger(method, TriggerType.OTHER)
    }

    /**
     * Registers a method to be run when the gui is closed.
     * Arguments passed through to method:
     * - the gui that is closed
     *
     * @param method the method to run
     * @return the trigger
     */
    fun registerClosed(method: Any) = apply {
        onClosed = RegularTrigger(method, TriggerType.OTHER)
    }

    fun unregisterDraw() = apply {
        onDraw?.unregister()
        onDraw = null
    }

    fun unregisterClicked() = apply {
        onClick?.unregister()
        onClick = null
    }

    fun unregisterScrolled() = apply {
        onScroll?.unregister()
        onScroll = null
    }

    fun unregisterKeyTyped() = apply {
        onKeyTyped?.unregister()
        onKeyTyped = null
    }

    fun unregisterMouseDragged() = apply {
        onMouseDragged?.unregister()
        onMouseDragged = null
    }

    fun unregisterMouseReleased() = apply {
        onMouseReleased?.unregister()
        onMouseReleased = null
    }

    fun unregisterActionPerformed() = apply {
        onActionPerformed?.unregister()
        onActionPerformed = null
    }

    fun unregisterOpened() = apply {
        onOpened?.unregister()
        onOpened = null
    }

    fun unregisterClosed() = apply {
        onClosed?.unregister()
        onClosed = null
    }

    override fun initScreen(width: Int, height: Int) {
        super.initScreen(width, height)

        ScreenMouseEvents.afterMouseScroll(this).register { _, x, y, _, dy ->
            onScroll?.trigger(arrayOf(x, y, dy))
        }

        buttons.values.forEach(::addDrawableChild)
        onOpened?.trigger(arrayOf(this))
    }

    /**
     * Internal method to run trigger. Not meant for public use
     */
    override fun onScreenClose() {
        super.onScreenClose()
        onClosed?.trigger(arrayOf(this))
    }

    /**
     * Internal method to run trigger. Not meant for public use
     */
    override fun onMouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int) {
        super.onMouseClicked(mouseX, mouseY, mouseButton)
        onClick?.trigger(arrayOf(mouseX, mouseY, mouseButton))
    }

    /**
     * Internal method to run trigger. Not meant for public use
     *
     * Note: [state] is actually the mouse button, no clue why it's named that
     */
    override fun onMouseReleased(mouseX: Double, mouseY: Double, state: Int) {
        super.onMouseReleased(mouseX, mouseY, state)
        onMouseReleased?.trigger(arrayOf(mouseX, mouseY, state))
    }

    /**
     * Internal method to run trigger. Not meant for public use
     */
    override fun onMouseDragged(
        x: Double,
        y: Double,
        clickedButton: Int,
        timeSinceLastClick: Long,
    ) {
        super.onMouseDragged(x, y, clickedButton, timeSinceLastClick)
        onMouseDragged?.trigger(arrayOf(mouseX, mouseY, clickedButton))
    }

    /**
     * Internal method to run trigger. Not meant for public use
     */
    override fun onDrawScreen(
        matrixStack: UMatrixStack,
        mouseX: Int,
        mouseY: Int,
        partialTicks: Float,
    ) {
        super.onDrawScreen(matrixStack, mouseX, mouseY, partialTicks)

        @Suppress("UNCHECKED_CAST")
        val drawContexts = drawContextsField.get(this) as List<DrawContext>
        Renderer.pushMatrix(UMatrixStack(drawContexts.last().matrices))

        Renderer.partialTicks = partialTicks

        this.mouseX = mouseX
        this.mouseY = mouseY
        onDraw?.trigger(arrayOf(mouseX, mouseY, partialTicks))

        Renderer.popMatrix()
    }

    /**
     * Internal method to run trigger. Not meant for public use
     */
    override fun onKeyPressed(keyCode: Int, typedChar: Char, modifiers: UKeyboard.Modifiers?) {
        super.onKeyPressed(keyCode, typedChar, modifiers)

        if (keyCode != 0) {
            var char = keyCode.toChar()
            if (modifiers?.isShift != true)
                char = char.lowercaseChar()
            onKeyTyped?.trigger(arrayOf(char, keyCode))
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
    fun addButton(button: ButtonWidget): Int {
        val id = nextButtonId++
        buttons[id] = button
        addDrawableChild(button)
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
    @JvmOverloads
    fun addButton(
        x: Int,
        y: Int,
        width: Int = 200,
        height: Int = 20,
        buttonText: TextComponent,
    ): Int {
        val id = nextButtonId++
        val button = ButtonWidget.builder(buttonText) {
            onActionPerformed?.trigger(arrayOf(id))
        }.dimensions(x, y, width, height).build()
        buttons[id] = button
        addDrawableChild(button)
        return id
    }

    fun addButton(x: Int, y: Int, width: Int = 200, height: Int = 20, buttonText: String) =
        addButton(x, y, width, height, TextComponent(buttonText))

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

    fun getButtonVisibility(buttonId: Int): Boolean = buttons[buttonId]?.visible ?: false

    /**
     * Sets the visibility of a button
     *
     * @param buttonId the id of the button to change
     * @param visible the new visibility of the button
     * @return the Gui for method chaining
     */
    fun setButtonVisibility(buttonId: Int, visible: Boolean) = apply {
        buttons[buttonId]?.visible = visible
    }

    fun getButtonEnabled(buttonId: Int): Boolean = buttons[buttonId]?.active ?: false

    /**
     * Sets the enabled state of a button
     *
     * @param buttonId the id of the button to set
     * @param enabled the enabled state of the button
     * @return the Gui for method chaining
     */
    fun setButtonEnabled(buttonId: Int, enabled: Boolean) = apply {
        buttons[buttonId]?.active = enabled
    }

    fun getButtonWidth(buttonId: Int): Int = buttons[buttonId]?.width ?: 0

    /**
     * Sets the button's width. Button textures break if the width is greater than 200
     *
     * @param buttonId id of the button
     * @param width the new width
     * @return the Gui for method chaining
     */
    fun setButtonWidth(buttonId: Int, width: Int) = apply {
        buttons[buttonId]?.width = width
    }

    fun getButtonHeight(buttonId: Int): Int = buttons[buttonId]?.height ?: 0

    /**
     * Sets the button's height. Button textures break if the height is not 20
     *
     * @param buttonId id of the button
     * @param height the new height
     * @return the Gui for method chaining
     */
    fun setButtonHeight(buttonId: Int, height: Int) = apply {
        buttons[buttonId]?.asMixin<ClickableWidgetAccessor>()?.setHeight(height)
    }

    fun getButtonX(buttonId: Int): Int = buttons[buttonId]?.x ?: 0

    /**
     * Sets the button's x position
     *
     * @param buttonId id of the button
     * @param x the new x position
     * @return the Gui for method chaining
     */
    fun setButtonX(buttonId: Int, x: Int) = apply {
        buttons[buttonId]?.x = x
    }

    fun getButtonY(buttonId: Int): Int = buttons[buttonId]?.y ?: 0

    /**
     * Sets the button's y position
     *
     * @param buttonId id of the button
     * @param y the new y position
     * @return the Gui for method chaining
     */
    fun setButtonY(buttonId: Int, y: Int) = apply {
        buttons[buttonId]?.y = y
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
        buttons[buttonId]?.apply {
            this.x = x
            this.y = y
        }
    }

    /**
     * Sets the button's text
     *
     * @param buttonId id of the button
     * @param text the new text
     */
    fun setButtonText(buttonId: Int, text: TextComponent) = apply {
        buttons[buttonId]?.message = text
    }

    /**
     * Sets the button's text
     *
     * @param buttonId id of the button
     * @param text the new text
     */
    fun setButtonText(buttonId: Int, text: String) = setButtonText(buttonId, TextComponent(text))

    /**
     * Sets the gui's tooltip, this will be visible on top of the cursor
     * when the gui is open.
     *
     * @param text the contents of the tooltip
     */
    fun setTooltip(text: TextComponent) = apply {
        setTooltip(Tooltip.wrapLines(Client.getMinecraft(), text))
    }

    /**
     * Sets the gui's tooltip, this will be visible on top of the cursor
     * when the gui is open.
     *
     * @param text the contents of the tooltip
     */
    fun setTooltip(text: String) = setTooltip(TextComponent(text))

    private companion object {
        private val drawContextsField = UScreen::class.java.getDeclaredField("drawContexts").also {
            it.isAccessible = true
        }
    }
}
