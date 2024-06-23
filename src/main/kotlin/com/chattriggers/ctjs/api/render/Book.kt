package com.chattriggers.ctjs.api.render

import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.message.TextComponent
import com.chattriggers.ctjs.internal.mixins.BookScreenAccessor
import com.chattriggers.ctjs.internal.utils.asMixin
import net.minecraft.client.gui.screen.ingame.BookScreen
import net.minecraft.text.StringVisitable

class Book {
    private var screen: BookScreen? = null
    private val customContents = BookScreen.Contents(emptyList())

    /**
     * Add a page to the book.
     *
     * @param contents the entire message for what the page should be
     * @return the current book to allow method chaining
     */
    fun addPage(contents: TextComponent) = apply {
        customContents.pages.add(contents)
    }

    /**
     * Overloaded method for adding a simple page to the book.
     *
     * @param message a simple string to make the page
     * @return the current book to allow method chaining
     */
    fun addPage(message: String) = apply {
        addPage(TextComponent(message))
    }

    /**
     * Inserts a page at the specified index of the book
     *
     * @param pageIndex the index of the page to set
     * @param message the message to set the page to
     * @return the current book to allow method chaining
     */
    fun insertPage(pageIndex: Int, message: TextComponent) = apply {
        require(pageIndex in customContents.pages.indices) {
            println("Invalid index $pageIndex for Book with ${customContents.pageCount} pages")
        }

        customContents.pages.add(pageIndex, message)
        screen?.asMixin<BookScreenAccessor>()?.invokeUpdatePageButtons()
    }

    fun insertPage(pageIndex: Int, message: String) = insertPage(pageIndex, TextComponent(message))

    /**
     * Sets a page of the book to the specified message.
     *
     * @param pageIndex the index of the page to set
     * @param message the message to set the page to
     * @return the current book to allow method chaining
     */
    fun setPage(pageIndex: Int, message: TextComponent) = apply {
        require(pageIndex in customContents.pages.indices) {
            println("Invalid index $pageIndex for Book with ${customContents.pageCount} pages")
        }

        customContents.pages[pageIndex] = message
        screen?.asMixin<BookScreenAccessor>()?.invokeUpdatePageButtons()
    }

    fun setPage(pageIndex: Int, message: String) = setPage(pageIndex, TextComponent(message))

    @JvmOverloads
    fun display(pageIndex: Int = 0) {
        screen = BookScreen(customContents)
        Client.scheduleTask {
            Client.getMinecraft().setScreen(screen)
            screen!!.setPage(pageIndex)
        }
    }

    fun isOpen(): Boolean {
        return Client.currentGui.get() === screen
    }

    fun getCurrentPage(): Int {
        return if (!isOpen() || screen == null) -1 else screen!!.asMixin<BookScreenAccessor>().pageIndex
    }
}
