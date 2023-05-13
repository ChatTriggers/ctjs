package com.chattriggers.ctjs.minecraft.wrappers.inventory

import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.util.Nameable
import net.minecraft.inventory.Inventory as MCInventory

class Inventory {
    val inventory: MCInventory?
    val screen: HandledScreen<*>?

    constructor(inventory: MCInventory) {
        this.inventory = inventory
        this.screen = null
    }

    constructor(container: HandledScreen<*>) {
        this.inventory = null
        this.screen = container
    }

    // TODO: Verify the last part of this description
    /**
     * Gets the total size of the Inventory.
     * The player's inventory size is 36, 27 for the main inventory, plus 9 for the hotbar.
     * A single chest's size would be 63, because it also counts the player's inventory.
     *
     * @return the size of the Inventory
     */
    val size: Int get() = inventory?.size() ?: screen!!.screenHandler.slots.size

    /**
     * Gets the item in any slot, starting from 0.
     *
     * @param slot the slot index
     * @return the [Item] in that slot, or null if there is no item
     */
    fun getStackInSlot(slot: Int): Item? = inventory?.getStack(slot)?.let(::Item)
        ?: screen!!.screenHandler.getSlot(slot)?.stack?.let(::Item)

    /**
     * Returns the window identifier number of this Inventory.
     * This Inventory must be backed by a HandledScreen [isScreen]
     *
     * @return the window id
     */
    fun getWindowId(): Int = screen?.screenHandler?.syncId ?: -1

    /**
     * Checks if an item can be shift clicked into a certain slot, i.e. coal into the bottom of a furnace.
     *
     * @param slot the slot index
     * @param item the item for checking
     * @return whether it can be shift clicked in
     */
    fun isItemValidForSlot(slot: Int, item: Item) = inventory?.isValid(slot, item.stack) ?: true

    /**
     * @return a list of the [Item]s in an inventory
     */
    fun getItems() = (0 until size).map(::getStackInSlot)

    /**
     * Checks whether the inventory contains the given item.
     *
     * @param item the item to check for
     * @return whether the inventory contains the item
     */
    fun contains(item: Item) = getItems().contains(item)

    /**
     * Checks whether the inventory contains an item with ID.
     *
     * @param id the ID of the item to match
     * @return whether the inventory contains an item with ID
     */
    fun contains(id: Int) = getItems().any { it?.type?.getID() == id }

    /**
     * Gets the index of any item in the inventory, and returns the slot number.
     * Returns -1 if the inventory does not contain the item.
     *
     * @param item the item to check for
     * @return the index of the given item
     */
    fun indexOf(item: Item) = getItems().indexOf(item)

    /**
     * Gets the index of any item in the inventory with matching ID, and returns the slot number.
     * Returns -1 if the inventory does not contain the item.
     *
     * @param id the item ID to check for
     * @return the index of the given item with ID
     */
    fun indexOf(id: Int) = getItems().indexOfFirst { it?.type?.getID() == id }

    /**
     * Returns true if this Inventory wraps a [HandledScreen] object
     * rather than an [MCInventory] object
     *
     * @return if this is a container
     */
    fun isScreen(): Boolean = screen != null

    /**
     * Gets the name of the inventory, simply "container" for most chest-like blocks.
     *
     * @return the name of the inventory
     */
    fun getName(): UTextComponent {
        return if (inventory is Nameable) UTextComponent(inventory.name) else UTextComponent("container")
    }

    fun getClassName(): String = inventory?.javaClass?.simpleName ?: screen!!.javaClass.simpleName

    override fun toString(): String = "Inventory(name=${getName()}, size=$size, isScreen=${isScreen()})"
}
