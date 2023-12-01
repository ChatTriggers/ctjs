package com.chattriggers.ctjs.api.message

import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.client.Player
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket
import net.minecraft.text.MutableText
import java.util.concurrent.ThreadLocalRandom

class Message {
    private var chatLineId: Int = -1
    private var isRecursive: Boolean = false
    private var isFormatted: Boolean = true

    /**
     * The individual components in the message
     */
    val messageParts: MutableList<TextComponent> = mutableListOf()

    /**
     * The underlying [TextComponent]
     */
    lateinit var chatMessage: TextComponent
        private set

    val formattedText: String
        get() = chatMessage.formattedText

    val unformattedText: String
        get() = chatMessage.unformattedText

    constructor(component: TextComponent) {
        if (component.siblings.isEmpty()) {
            messageParts.add(component)
        } else {
            component.siblings
                .filterIsInstance<MutableText>()
                .map { TextComponent(it) }
                .forEach { messageParts.add(it) }
        }
        parseMessage()
    }

    constructor(vararg parts: Any) {
        parts.forEach(::addPart)
        parseMessage()
    }

    /**
     * @return the chat line ID of the message
     */
    fun getChatLineId(): Int = chatLineId

    /**
     * Sets the chat line ID of the message. Useful for updating an already sent chat message.
     */
    fun setChatLineId(id: Int) = apply { chatLineId = id }

    /**
     * @return true if the message can trip other triggers.
     */
    fun isRecursive(): Boolean = isRecursive

    /**
     * Sets whether the message can trip other triggers.
     * @param recursive true if message can trip other triggers.
     */
    fun setRecursive(recursive: Boolean) = apply { this.isRecursive = recursive }

    /**
     * @return true if the message is formatted
     */
    fun isFormatted(): Boolean = isFormatted

    /**
     * Sets if the message is to be formatted
     * @param formatted true if formatted
     */
    fun setFormatted(formatted: Boolean) = apply { this.isFormatted = formatted }

    /**
     * Sets the TextComponent or String in the Message at index.
     *
     * @param index the index of the TextComponent or String to change
     * @param component the new TextComponent or String to replace with
     * @return the Message for method chaining
     */
    fun setTextComponent(index: Int, component: Any) = apply {
        if (component is String) {
            messageParts[index] = TextComponent(component)
        } else {
            TextComponent.from(component)?.also { messageParts[index] = it }
        }
        parseMessage()
    }

    /**
     * Adds a TextComponent or String at index of the Message.
     *
     * @param index the index to insert the new TextComponent or String
     * @param component the new TextComponent or String to insert
     * @return the Message for method chaining
     */
    fun addTextComponent(index: Int, component: Any) = apply {
        if (component is String) {
            messageParts.add(index, TextComponent(component))
        } else {
            TextComponent.from(component)?.also { messageParts.add(index, it) }
        }
        parseMessage()
    }

    /**
     * Adds a TextComponent or String to the end of the Message.
     *
     * @param component the new TextComponent or String to add
     * @return the Message for method chaining
     */
    fun addTextComponent(component: Any): Message = addTextComponent(messageParts.size, component)

    /**
     * Must be called to be able to edit later
     */
    fun mutable() = apply {
        chatLineId = ThreadLocalRandom.current().nextInt()
    }

    fun edit(vararg replacements: Any) = apply {
        require(chatLineId != -1) { "This Message is not mutable" }
        messageParts.clear()
        replacements.forEach(::addPart)
        parseMessage()
        ChatLib.editChat(chatLineId, *replacements)
    }

    fun chat() = apply {
        if (Player.toMC() == null)
            return@apply

        if (chatLineId != -1) {
            ChatLib.sendMessageWithId(this)
            return@apply
        }

        if (isRecursive) {
            Client.scheduleTask {
                Client.getMinecraft().networkHandler?.onGameMessage(GameMessageS2CPacket(chatMessage, false))
            }
        } else {
            Player.toMC()?.sendMessage(chatMessage)
        }
    }

    fun actionBar() = apply {
        if (Player.toMC() == null)
            return@apply

        if (isRecursive) {
            Client.scheduleTask {
                Client.getMinecraft().networkHandler?.onGameMessage(GameMessageS2CPacket(chatMessage, true))
            }
        } else {
            Player.toMC()?.sendMessage(chatMessage, true)
        }
    }

    private fun addPart(part: Any) {
        if (part is TextComponent) {
            messageParts.add(part)
        } else TextComponent.from(part)?.also(messageParts::add)
        parseMessage()
    }

    private fun parseMessage() {
        chatMessage = TextComponent("")
        messageParts.forEach { chatMessage.appendSibling(it) }
    }
}
