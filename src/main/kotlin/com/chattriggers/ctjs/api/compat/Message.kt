package com.chattriggers.ctjs.api.compat

import com.chattriggers.ctjs.api.message.ChatLib
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.api.triggers.ChatTrigger
import java.util.ArrayList

/**
 * Mutable facade for the CTJS 2.x Message API.
 *
 * New code should use immutable [TextComponent] instances directly.
 */
@Deprecated("Use TextComponent")
class Message private constructor(
    private var component: TextComponent,
    private var formatted: Boolean,
) {
    constructor(event: ChatTrigger.Event) : this(event.message, true)

    constructor(component: TextComponent) : this(component, true)

    constructor(messageParts: ArrayList<Any>) : this(*messageParts.toTypedArray())

    constructor(vararg components: Any) : this(
        TextComponent(*components.map {
            if (it is Message) it.component else it
        }.toTypedArray()),
        true,
    )

    fun getChatMessage(): TextComponent = component

    fun getFormattedText(): String = LegacyCompatibility.normalizeFormattedText(component.formattedText)

    fun getUnformattedText(): String = component.unformattedText

    fun getMessageParts(): List<TextComponent> = component.map { TextComponent(it) }

    fun getChatLineId(): Int = component.getChatLineId()

    fun setChatLineId(id: Int) = apply {
        component = component.withChatLineId(id)
    }

    fun isRecursive(): Boolean = component.isRecursive()

    fun setRecursive(recursive: Boolean) = apply {
        component = component.withRecursive(recursive)
    }

    fun isFormatted(): Boolean = formatted

    fun setFormatted(formatted: Boolean) = apply {
        this.formatted = formatted
    }

    fun setTextComponent(index: Int, value: Any) = apply {
        component = component.withoutTextAt(index).withTextAt(index, unwrap(value))
    }

    fun addTextComponent(value: Any) = apply {
        component = component.withText(unwrap(value))
    }

    fun addTextComponent(index: Int, value: Any) = apply {
        component = component.withTextAt(index, unwrap(value))
    }

    fun clone(): Message = copy()

    fun copy() = Message(component, formatted)

    fun edit(vararg replacements: Message) {
        ChatLib.editChat(component, *replacements.map { it.component }.toTypedArray())
    }

    fun chat() {
        component.chat()
    }

    fun actionBar() {
        component.actionBar()
    }

    override fun toString() = component.formattedText

    private fun unwrap(value: Any) = if (value is Message) value.component else value
}
