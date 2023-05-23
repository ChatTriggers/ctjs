package com.chattriggers.ctjs.minecraft.libs

import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer
import com.chattriggers.ctjs.minecraft.listeners.ClientListener
import com.chattriggers.ctjs.minecraft.objects.Message
import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.minecraft.wrappers.Player
import com.chattriggers.ctjs.mixins.ChatHudAccessor
import com.chattriggers.ctjs.utils.asMixin
import com.chattriggers.ctjs.utils.console.printToConsole
import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.client.gui.hud.ChatHudLine
import net.minecraft.client.gui.hud.MessageIndicator
import org.mozilla.javascript.regexp.NativeRegExp
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.regex.Pattern
import kotlin.math.roundToInt

object ChatLib {
    private val chatLineIds = mutableMapOf<ChatHudLine, Int>()
    private val chatHudAccessor get() = Client.getChatGUI()?.asMixin<ChatHudAccessor>()

    /**
     * Prints text in the chat.
     * The text can be a String, a [Message] or a [UTextComponent]
     *
     * @param text the text to be printed
     */
    @JvmStatic
    fun chat(text: Any) {
        when (text) {
            is String -> Message(text).chat()
            is Message -> text.chat()
            is UTextComponent -> text.chat()
            else -> Message(text.toString()).chat()
        }
    }

    /**
     * Shows text in the action bar.
     * The text can be a String, a [Message] or a [UTextComponent]
     *
     * @param text the text to show
     */
    @JvmStatic
    fun actionBar(text: Any) {
        when (text) {
            is String -> Message(text).actionBar()
            is Message -> text.actionBar()
            is UTextComponent -> text.actionBar()
            else -> Message(text.toString()).actionBar()
        }
    }

    /**
     * Simulates a chat message to be caught by other triggers for testing.
     * The text can be a String, a [Message] or a [UTextComponent]
     *
     * @param text The message to simulate
     */
    @JvmStatic
    fun simulateChat(text: Any) {
        when (text) {
            is String -> Message(text).apply { isRecursive = true }.chat()
            is Message -> text.apply { isRecursive = true }.chat()
            is UTextComponent -> Message(text).apply { isRecursive = true }.chat()
            else -> Message(text.toString()).apply { isRecursive = true }.chat()
        }
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
    @JvmOverloads
    @JvmStatic
    fun command(text: String, clientSide: Boolean = false) {
        // TODO: clientSide param? This is client side by default
        Client.getMinecraft().networkHandler?.sendChatCommand(text)
    }

    // TODO(breaking): Now only clears all chats
    /**
     * Clear all chat messages
     */
    @JvmStatic
    fun clearChat() {
        Client.getChatGUI()?.clear(false)
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
    @JvmOverloads
    @JvmStatic
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
        return Client.getChatGUI()?.width ?: 0
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
            val matcher = pattern.matcher(UTextComponent(it.content).unformattedText)
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
            removeFormatting(UTextComponent(it.content).unformattedText) == toReplace
        }
    }

    /**
     * Edits an already sent chat message by the [Message]
     *
     * @param toReplace the message to be replaced
     * @param replacements the new message(s) to be put in place of the old one
     */
    @JvmStatic
    fun editChat(toReplace: Message, vararg replacements: Any) {
        editLines(replacements) {
            toReplace.chatMessage.formattedText == UTextComponent(it.content).formattedText
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
                    val message = if (replacement !is Message) {
                        UTextComponent.from(replacement)?.let(::Message) ?: continue
                    } else replacement

                    val line = ChatHudLine(next.creationTick, message.chatMessage, null, indicator)
                    if (message.chatLineId != -1)
                        chatLineIds[line] = message.chatLineId

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
            val matcher = pattern.matcher(UTextComponent(it.content).unformattedText)
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
            removeFormatting(UTextComponent(it.content).unformattedText) == toDelete
        }
    }

    /**
     * Deletes an already sent chat message by the [Message]
     *
     * @param toDelete the message to be deleted
     */
    @JvmStatic
    fun deleteChat(toDelete: Message) {
        removeLines {
            toDelete.chatMessage.formattedText == UTextComponent(it.content).formattedText
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
    @JvmOverloads
    @JvmStatic
    fun addToSentMessageHistory(index: Int = -1, message: String) {
        if (index == -1) {
            Client.getMinecraft().inGameHud.chatHud.addToMessageHistory(message)
        } else {
            Client.getMinecraft().inGameHud.chatHud.messageHistory.add(index, message)
        }
    }

    // TODO(breaking): Remove getChatMessage()

    // helper method to make sure player exists before putting something in chat
    fun checkPlayerExists(out: String): Boolean {
        if (Player.getPlayer() == null) {
            out.printToConsole()
            return false
        }

        return true
    }

    internal fun sendMessageWithId(message: Message) {
        require(message.chatLineId != -1)

        val chatGui = Client.getChatGUI() ?: return
        val chatMessage = message.chatMessage
        chatGui.addMessage(chatMessage)
        val newChatLine: ChatHudLine = chatHudAccessor!!.messages[0]

        check(chatMessage == newChatLine.content()) {
            "Expected new chat message to be at index 0"
        }

        chatLineIds[newChatLine] = message.chatLineId
    }

    internal fun onChatHudClearChat() {
        chatLineIds.clear()
    }

    internal fun onChatHudLineRemoved(line: ChatHudLine) {
        chatLineIds.remove(line)
    }
}
