package com.chattriggers.ctjs.minecraft.objects

import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.mixins.BookScreenAccessor
import com.chattriggers.ctjs.utils.asMixin
import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.client.gui.screen.ingame.BookScreen
import net.minecraft.text.StringVisitable

// TODO*(breaking): Changes UMessage usages to UText
class Book {
    private var screen: BookScreen? = null
    private val customContents = CustomBookContents()

    /**
     * Add a page to the book.
     *
     * @param contents the entire message for what the page should be
     * @return the current book to allow method chaining
     */
    fun addPage(contents: UTextComponent) = apply {
        customContents.pages.add(contents)
    }

    /**
     * Overloaded method for adding a simple page to the book.
     *
     * @param message a simple string to make the page
     * @return the current book to allow method chaining
     */
    fun addPage(message: String) = apply {
        addPage(UTextComponent(message))
    }

    /**
     * Inserts a page at the specified index of the book
     *
     * @param pageIndex the index of the page to set
     * @param message the message to set the page to
     * @return the current book to allow method chaining
     */
    fun insertPage(pageIndex: Int, message: UTextComponent) = apply {
        require(pageIndex in customContents.pages.indices) {
            println("Invalid index $pageIndex for Book with ${customContents.pageCount} pages")
        }

        customContents.pages.add(pageIndex, message)
        screen?.asMixin<BookScreenAccessor>()?.invokeUpdatePageButtons()
    }

    fun insertPage(pageIndex: Int, message: String) = insertPage(pageIndex, UTextComponent(message))

    /**
     * Sets a page of the book to the specified message.
     *
     * @param pageIndex the index of the page to set
     * @param message the message to set the page to
     * @return the current book to allow method chaining
     */
    fun setPage(pageIndex: Int, message: UTextComponent) = apply {
        require(pageIndex in customContents.pages.indices) {
            println("Invalid index $pageIndex for Book with ${customContents.pageCount} pages")
        }

        customContents.pages[pageIndex] = message
        screen?.asMixin<BookScreenAccessor>()?.invokeUpdatePageButtons()
    }

    fun setPage(pageIndex: Int, message: String) = setPage(pageIndex, UTextComponent(message))

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

    private class CustomBookContents : BookScreen.Contents {
        val pages = mutableListOf<UTextComponent>()

        override fun getPageCount() = pages.size

        override fun getPageUnchecked(index: Int): StringVisitable = pages[index]
    }
}
