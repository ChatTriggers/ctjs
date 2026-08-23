package com.chattriggers.ctjs.internal.commands

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.SharedSuggestionProvider
import kotlin.test.Test
import kotlin.test.assertEquals

class CommandLifecycleTest {
    @Test
    fun `register unregister reconnect and reload stay exactly once`() {
        val collection = TestCollection()
        val client = CommandDispatcher<SharedSuggestionProvider>()
        val network = CommandDispatcher<SharedSuggestionProvider>()
        collection.attachClientDispatcher(client)
        collection.attachNetworkDispatcher(network)
        val command = TestCommand("one")

        collection.register(command)
        collection.register(command)
        assertEquals(listOf(client, network), command.registered)
        assertEquals(1, collection.registeredCount())

        collection.unregister(command)
        assertEquals(listOf(client, network), command.unregistered)
        assertEquals(0, collection.registeredCount())

        collection.register(command)
        collection.detachDispatchers()
        val reconnectClient = CommandDispatcher<SharedSuggestionProvider>()
        val reconnectNetwork = CommandDispatcher<SharedSuggestionProvider>()
        collection.attachClientDispatcher(reconnectClient)
        collection.attachNetworkDispatcher(reconnectNetwork)
        assertEquals(1, command.registered.count { it === reconnectClient })
        assertEquals(1, command.registered.count { it === reconnectNetwork })

        collection.unregisterAll()
        assertEquals(0, collection.registeredCount())
        assertEquals(1, command.unregistered.count { it === reconnectClient })
        assertEquals(1, command.unregistered.count { it === reconnectNetwork })
    }

    @Test
    fun `conflict is dispatcher local and override remains explicit`() {
        val collection = TestCollection()
        val conflict = CommandDispatcher<SharedSuggestionProvider>()
        conflict.register(literal("same"))
        val free = CommandDispatcher<SharedSuggestionProvider>()
        collection.attachClientDispatcher(conflict)
        collection.attachNetworkDispatcher(free)

        val normal = TestCommand("same")
        collection.register(normal)
        assertEquals(listOf("same"), collection.warnings)
        assertEquals(listOf(free), normal.registered)

        val override = TestCommand("same", overrideExisting = true)
        collection.register(override)
        assertEquals(listOf(conflict, free), override.registered)
    }

    private class TestCollection : CommandCollection() {
        val warnings = mutableListOf<String>()
        override fun warnConflict(name: String) {
            warnings += name
        }
    }

    private class TestCommand(
        override val name: String,
        override val overrideExisting: Boolean = false,
    ) : Command {
        val registered = mutableListOf<CommandDispatcher<SharedSuggestionProvider>>()
        val unregistered = mutableListOf<CommandDispatcher<SharedSuggestionProvider>>()

        override fun registerImpl(dispatcher: CommandDispatcher<SharedSuggestionProvider>) {
            registered += dispatcher
        }

        override fun unregisterImpl(dispatcher: CommandDispatcher<SharedSuggestionProvider>) {
            unregistered += dispatcher
        }
    }
}
