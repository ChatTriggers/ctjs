package com.chattriggers.ctjs.api.message

import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.render.Renderer
import com.chattriggers.ctjs.internal.listeners.ClientListener
import com.chattriggers.ctjs.internal.mixins.ChatHudAccessor
import com.chattriggers.ctjs.internal.utils.asMixin
import net.fabricmc.fabric.impl.command.client.ClientCommandInternals
import net.minecraft.client.gui.hud.ChatHudLine
import net.minecraft.client.gui.hud.MessageIndicator
import org.mozilla.javascript.regexp.NativeRegExp
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.regex.Pattern
import kotlin.math.roundToInt

object ChatLib {
    private val chatLineIds = mutableMapOf<ChatHudLine, Int>()
    private val chatHudAccessor get() = Client.getChatGui()?.asMixin<ChatHudAccessor>()

    /**
     * Prints text in the chat.
     * The text can be a String or a [TextComponent]
     *
     * @param text the text to be printed
     */
    @JvmStatic
    fun chat(text: Any?) {
        when (text) {
            is TextComponent -> text
            is CharSequence -> TextComponent(text)
            else -> TextComponent(text.toString())
        }.chat()
    }

    /**
     * Shows text in the action bar.
     * The text can be a String or a [TextComponent]
     *
     * @param text the text to show
     */
    @JvmStatic
    fun actionBar(text: Any?) {
        when (text) {
            is TextComponent -> text
            is CharSequence -> TextComponent(text)
            else -> TextComponent(text.toString())
        }.actionBar()
    }

    /**
     * Simulates a chat message to be caught by other triggers for testing.
     * The text can be a String or a [TextComponent]
     *
     * @param text The message to simulate
     */
    @JvmStatic
    fun simulateChat(text: Any?) {
        when (text) {
            is TextComponent -> text
            is CharSequence -> TextComponent(text)
            else -> TextComponent(text.toString())
        }.withRecursive().chat()
    }


    /**
     * Replaces the easier to type '&' color codes with proper color codes in a string.
     *
     * @param message The string to add color codes to
     * @return the formatted message
     */
    @JvmStatic
    fun addColor(message: String?): String {
        return message.toString().replace("(?<!\\\\)&(?![^0-9a-fk-or]|$)".toRegex(), "\u00a7")
    }

    /**
     * Says chat message.
     * This message is actually sent to the server.
     *
     * @param text the message to be sent
     */
    @JvmStatic
    fun say(text: String) = Client.getMinecraft().networkHandler?.sendChatMessage(text)

    /**
     * Runs a command.
     *
     * @param text the command to run, without the leading slash (Ex. "help")
     * @param clientSide should the command be run as a client side command
     */
    @JvmStatic
    @JvmOverloads
    fun command(text: String, clientSide: Boolean = false) {
        if (clientSide) ClientCommandInternals.executeCommand(text)
        else Client.getMinecraft().networkHandler?.sendChatCommand(text)
    }

    /**
     * Clear all chat messages
     */
    @JvmStatic
    fun clearChat() {
        Client.getChatGui()?.clear(false)
        chatLineIds.clear()
    }

    /**
     * Get a message that will be perfectly one line of chat,
     * the separator repeated as many times as necessary.
     * The separator defaults to "-"
     *
     * @param separator the message to split chat with
     * @return the message that would split chat
     */
    @JvmStatic
    @JvmOverloads
    fun getChatBreak(separator: String = "-"): String {
        val len = Renderer.getStringWidth(separator)
        val times = getChatWidth() / len
        return separator.repeat(times)
    }

    /**
     * Gets the width of Minecraft's chat
     *
     * @return the width of chat
     */
    @JvmStatic
    fun getChatWidth(): Int {
        return Client.getChatGui()?.width ?: 0
    }

    /**
     * Remove all formatting
     *
     * @param text the string to un-format
     * @return the unformatted string
     */
    @JvmStatic
    fun removeFormatting(text: String): String {
        return text.replace("[\u00a7&][0-9a-fk-or]".toRegex(), "")
    }

    /**
     * Replaces Minecraft formatted text with normal formatted text
     *
     * @param text the formatted string
     * @return the unformatted string
     */
    @JvmStatic
    fun replaceFormatting(text: String): String {
        return text.replace("\u00a7(?![^0-9a-fk-or]|$)".toRegex(), "&")
    }

    /**
     * Get a message that will be perfectly centered in chat.
     *
     * @param text the text to be centered
     * @return the centered message
     */
    @JvmStatic
    fun getCenteredText(text: String): String {
        val textWidth = Renderer.getStringWidth(addColor(text))
        val chatWidth = getChatWidth()

        if (textWidth >= chatWidth)
            return text

        val spaceWidth = (chatWidth - textWidth) / 2f
        val spaceBuilder = StringBuilder().apply {
            repeat((spaceWidth / Renderer.getStringWidth(" ")).roundToInt()) {
                append(' ')
            }
        }

        return spaceBuilder.append(text).toString()
    }

    /**
     * Copies the given String to the user's clipboard
     *
     * @param text the text to copy
     */
    @JvmStatic
    fun copyToClipboard(text: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }


    /**
     * Edits an already sent chat message matched by [regexp].
     *
     * @param regexp the regex object to match to the message
     * @param replacements the new message(s) to be put in replace of the old one
     */
    @JvmStatic
    fun editChat(regexp: NativeRegExp, vararg replacements: Any) {
        val global = regexp["global"] as Boolean
        val ignoreCase = regexp["ignoreCase"] as Boolean
        val multiline = regexp["multiline"] as Boolean

        val flags = (if (ignoreCase) Pattern.CASE_INSENSITIVE else 0) or if (multiline) Pattern.MULTILINE else 0
        val pattern = Pattern.compile(regexp["source"] as String, flags)

        editLines(replacements) {
            val matcher = pattern.matcher(TextComponent(it.content).unformattedText)
            if (global) matcher.find() else matcher.matches()
        }
    }

    /**
     * Edits an already sent chat message by the text of the chat
     *
     * @param toReplace the unformatted text of the message to be replaced
     * @param replacements the new message(s) to be put in place of the old one
     */
    @JvmStatic
    fun editChat(toReplace: String, vararg replacements: Any) {
        editLines(replacements) {
            removeFormatting(TextComponent(it.content).unformattedText) == toReplace
        }
    }

    /**
     * Edits an already sent chat message by the [TextComponent]
     *
     * @param toReplace the message to be replaced
     * @param replacements the new message(s) to be put in place of the old one
     */
    @JvmStatic
    fun editChat(toReplace: TextComponent, vararg replacements: Any) {
        editLines(replacements) {
            toReplace.formattedText == TextComponent(it.content).formattedText
        }
    }

    /**
     * Edits an already sent chat message by its chat line id
     *
     * @param chatLineId the chat line id of the message to be replaced
     * @param replacements the new message(s) to be put in place of the old one
     */
    @JvmStatic
    fun editChat(chatLineId: Int, vararg replacements: Any) {
        editLines(replacements) {
            chatLineIds[it] == chatLineId
        }
    }

    /**
     * Edits an already sent chat message by given a callback that receives
     * [TextComponent] instances
     *
     * @param matcher a function that accepts a [TextComponent] and returns a boolean
     * @param replacements the new message(s) to be put in place of the old one
     */
    @JvmStatic
    fun editChat(matcher: (TextComponent) -> Boolean, vararg replacements: Any) {
        editLines(replacements) { matcher(TextComponent(it.content)) }
    }

    private fun editLines(replacements: Array<out Any>, matcher: (ChatHudLine) -> Boolean) {
        val mc = Client.getMinecraft()
        val indicator = if (mc.isConnectedToLocalServer) MessageIndicator.singlePlayer() else MessageIndicator.system()
        var edited = false
        val it = chatHudAccessor?.messages?.listIterator() ?: return

        while (it.hasNext()) {
            val next = it.next()
            if (matcher(next)) {
                edited = true
                it.remove()
                chatLineIds.remove(next)
                for (replacement in replacements) {
                    val message = replacement as? TextComponent ?: TextComponent(replacement)
                    val line = ChatHudLine(next.creationTick, message, null, indicator)
                    if (message.getChatLineId() != -1)
                        chatLineIds[line] = message.getChatLineId()

                    it.add(line)
                }
            }
        }

        if (edited)
            chatHudAccessor!!.invokeRefresh()
    }

    /**
     * Deletes an already sent chat message matching [regexp].
     *
     * @param regexp the regex object to match to the message
     */
    @JvmStatic
    fun deleteChat(regexp: NativeRegExp) {
        val global = regexp["global"] as Boolean
        val ignoreCase = regexp["ignoreCase"] as Boolean
        val multiline = regexp["multiline"] as Boolean

        val flags = (if (ignoreCase) Pattern.CASE_INSENSITIVE else 0) or if (multiline) Pattern.MULTILINE else 0
        val pattern = Pattern.compile(regexp["source"] as String, flags)

        removeLines {
            val matcher = pattern.matcher(TextComponent(it.content).unformattedText)
            if (global) matcher.find() else matcher.matches()
        }
    }

    /**
     * Deletes an already sent chat message by the text of the chat
     *
     * @param toDelete the unformatted text of the message to be deleted
     */
    @JvmStatic
    fun deleteChat(toDelete: String) {
        removeLines {
            removeFormatting(TextComponent(it.content).unformattedText) == toDelete
        }
    }

    /**
     * Deletes an already sent chat message by the [TextComponent]
     *
     * @param toDelete the message to be deleted
     */
    @JvmStatic
    fun deleteChat(toDelete: TextComponent) {
        removeLines {
            toDelete.formattedText == TextComponent(it.content).formattedText
        }
    }

    /**
     * Deletes an already sent chat message by its chat line id
     *
     * @param chatLineId the chat line id of the message to be deleted
     */
    @JvmStatic
    fun deleteChat(chatLineId: Int) {
        removeLines { chatLineIds[it] == chatLineId }
    }

    /**
     * Deletes an already sent chat message given a callback that receives
     * [TextComponent] instances
     *
     * @param matcher a function that accepts a [TextComponent] and returns a boolean
     */
    @JvmStatic
    fun deleteChat(matcher: (TextComponent) -> Boolean) {
        removeLines { matcher(TextComponent(it.content)) }
    }

    private fun removeLines(matcher: (ChatHudLine) -> Boolean) {
        var removed = false
        val it = chatHudAccessor?.messages?.listIterator() ?: return

        while (it.hasNext()) {
            val next = it.next()
            if (matcher(next)) {
                it.remove()
                chatLineIds.remove(next)
                removed = true
            }
        }

        if (removed)
            chatHudAccessor!!.invokeRefresh()
    }

    /**
     * Gets the previous 1000 lines of chat
     *
     * @return A list of the last 1000 chat lines
     */
    @JvmStatic
    fun getChatLines(): List<String> {
        val hist = ClientListener.chatHistory.toMutableList()
        return hist.asReversed().map { it.formattedText }
    }

    /**
     * Adds a message to the player's chat history. This allows the message to
     * show up for the player when pressing the up/down keys while in the chat gui
     *
     * @param index the index to insert the message
     * @param message the message to add to chat history
     */
    @JvmStatic
    @JvmOverloads
    fun addToSentMessageHistory(index: Int = -1, message: String) {
        if (index == -1) {
            Client.getMinecraft().inGameHud.chatHud.addToMessageHistory(message)
        } else {
            Client.getMinecraft().inGameHud.chatHud.messageHistory.add(index, message)
        }
    }

    internal fun sendMessageWithId(message: TextComponent) {
        require(message.getChatLineId() != -1)

        val chatGui = Client.getChatGui() ?: return
        chatGui.addMessage(message)
        val newChatLine = chatHudAccessor!!.messages[0]

        check(message == newChatLine.content()) {
            "Expected new chat message to be at index 0"
        }

        chatLineIds[newChatLine] = message.getChatLineId()
    }

    internal fun onChatHudClearChat() {
        chatLineIds.clear()
    }

    internal fun onChatHudLineRemoved(line: ChatHudLine) {
        chatLineIds.remove(line)
    }
}
