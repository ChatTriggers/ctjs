package com.chattriggers.ctjs.minecraft.objects

import com.chattriggers.ctjs.minecraft.libs.ChatLib
import gg.essential.universal.UPacket
import gg.essential.universal.wrappers.UPlayer
import gg.essential.universal.wrappers.message.UTextComponent
import java.util.concurrent.ThreadLocalRandom

import net.minecraft.text.MutableText

class Message {
    private lateinit var _chatMessage: UTextComponent
    val messageParts: MutableList<UTextComponent> = mutableListOf()

    val chatMessage: UTextComponent
        get() {
            parseMessage()
            return _chatMessage
        }

    val formattedText: String
        get() = chatMessage.formattedText

    val unformattedText: String
        get() = chatMessage.unformattedText

    var chatLineId: Int = -1
    var isRecursive: Boolean = false
    var isFormatted: Boolean = true

    constructor(component: UTextComponent) {
        if (component.siblings.isEmpty()) {
            messageParts.add(component)
        } else {
            component.siblings
                .filterIsInstance<MutableText>()
                .map { UTextComponent(it) }
                .forEach { messageParts.add(it) }
        }
    }

    constructor(vararg parts: Any) {
        parts.forEach(::addPart)
    }

    fun setTextComponent(index: Int, component: Any) = apply {
        if (component is String) {
            messageParts[index] = UTextComponent(component)
        } else {
            UTextComponent.from(component)?.also { messageParts[index] = it }
        }
    }

    fun addTextComponent(index: Int, component: Any) = apply {
        if (component is String) {
            messageParts.add(index, UTextComponent(component))
        } else {
            UTextComponent.from(component)?.also { messageParts.add(index, it) }
        }
    }

    fun addTextComponent(component: Any): Message = addTextComponent(messageParts.size, component)

    /**
     * Must be called to be able to edit later
     */
    fun mutable() = apply {
        chatLineId = ThreadLocalRandom.current().nextInt()
    }

    fun edit(vararg replacements: Any) {
        if (chatLineId == -1) throw IllegalStateException("This message is not mutable!")
        messageParts.clear()
        replacements.forEach(::addPart)
        chat()
    }

    fun chat() {
        parseMessage()

        if (!ChatLib.checkPlayerExists("[CHAT]: ${chatMessage.formattedText}"))
            return

        if (chatLineId != -1) {
            ChatLib.sendMessageWithId(this)
            return
        }

        if (isRecursive) {
            UPacket.sendChatMessage(_chatMessage)
        } else {
            UPlayer.sendClientSideMessage(_chatMessage)
        }
    }

    fun actionBar() {
        parseMessage()

        if (ChatLib.checkPlayerExists("[ACTION BAR]: ${chatMessage.formattedText}"))
            UPacket.sendActionBarMessage(_chatMessage)
    }

    private fun addPart(part: Any) {
        if (part is UTextComponent) {
            messageParts.add(part)
        } else UTextComponent.from(part)?.also(messageParts::add)
    }

    private fun parseMessage() {
        _chatMessage = UTextComponent("")
        messageParts.forEach { _chatMessage.appendSibling(it) }
    }
}
