package com.chattriggers.ctjs.api.triggers

import com.chattriggers.ctjs.internal.commands.Command
import com.chattriggers.ctjs.internal.commands.StaticCommand

class CommandTrigger(method: Any) : Trigger(method, TriggerType.COMMAND) {
    private lateinit var commandName: String
    private var overrideExisting: Boolean = false
    private val staticCompletions = mutableListOf<String>()
    private val aliases = mutableSetOf<String>()
    private var command: Command? = null
    private var dynamicCompletions: ((List<String>) -> List<String>)? = null

    override fun trigger(args: Array<out Any?>) {
        callMethod(args)
    }

    /**
     * Sets the tab completion options for the command.
     * This method must be used before setting the command name, otherwise, the tab completions will not be set.
     *
     * @param args all the tab completion options.
     */
    fun setTabCompletions(vararg args: String) = apply {
        staticCompletions.clear()
        staticCompletions.addAll(args)
    }

    /**
     * This sets the possible tab completions for the command.
     * This method must be used before setting the command name, otherwise, the tab completions will not be set.
     *
     * @param callback the callback that returns the tab completion options.
     *
     * For example:
     * ```js
     * register("command", () => {
     *
     * }).setTabCompletions((args) => {
     *      return ["option1", "option2"];
     * }).setName("test");
     *```
     * The `args` parameter of the callback are the arguments currently passed to the command.
     * For instance, if you want to not show the options after the user tabs the first time, just add a check
     * for the length of the arguments and return an empty array.
     *
     * The return value of the callback **must be an array of strings**, and in this case will always return the 2
     * options in the array.
     */
    fun setTabCompletions(callback: (List<String>) -> List<String>) = apply {
        this.dynamicCompletions = callback
    }

    /**
     * Sets the aliases for the command.
     *
     * @param args all the aliases.
     */
    fun setAliases(vararg args: String) = apply {
        check(::commandName.isInitialized) { "Command name must be set before aliases!" }

        aliases.clear()
        aliases.addAll(args)
        reInstance()
    }

    /**
     * Sets the command name.
     * Example:
     * setCommandName("test")
     * would result in the command being /test
     *
     * @param commandName The command name
     * @param overrideExisting Whether existing commands with the same name should be overridden
     * @return the trigger for additional modification
     */
    @JvmOverloads
    fun setCommandName(commandName: String, overrideExisting: Boolean = false) = apply {
        this.commandName = commandName
        this.overrideExisting = overrideExisting
        reInstance()
    }

    /**
     * Alias for [setCommandName]
     *
     * @param commandName The command name
     * @param overrideExisting Whether existing commands with the same name should be overridden
     * @return the trigger for additional modification
     */
    @JvmOverloads
    fun setName(commandName: String, overrideExisting: Boolean = false) = setCommandName(commandName, overrideExisting)

    private fun reInstance() {
        command?.let(StaticCommand::unregister)
        command = StaticCommand(this, commandName, aliases, overrideExisting, staticCompletions, dynamicCompletions)
        StaticCommand.register(command!!)
    }
}
