package com.chattriggers.ctjs.api.world

import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.client.Player
import com.chattriggers.ctjs.api.message.TextComponent

object Server {
    @JvmStatic
    fun toMC() = Client.getMinecraft().currentServerEntry

    @JvmStatic
    fun isSingleplayer(): Boolean = Client.getMinecraft().isInSingleplayer

    /**
     * Gets the current server's IP, or "localhost" if the player
     * is in a single-player world.
     *
     * @return The IP of the current server
     */
    @JvmStatic
    fun getIP(): String {
        if (isSingleplayer())
            return "localhost"

        return toMC()?.address ?: ""
    }

    /**
     * Gets the current server's name, or "SinglePlayer" if the player
     * is in a single-player world.
     *
     * @return The name of the current server
     */
    @JvmStatic
    fun getName(): String {
        if (isSingleplayer())
            return "SinglePlayer"

        return toMC()?.name ?: ""
    }

    /**
     * Gets the current server's MOTD, or "SinglePlayer" if the player
     * is in a single-player world.
     *
     * @return The MOTD of the current server
     */
    @JvmStatic
    fun getMOTD(): String {
        if (isSingleplayer())
            return "SinglePlayer"

        return toMC()?.label?.let { TextComponent(it) }?.formattedText ?: ""
    }

    /**
     * Gets the ping to the current server, or 5 if the player
     * is in a single-player world. Returns -1 if not in a world
     *
     * @return The ping to the current server
     */
    @JvmStatic
    fun getPing(): Long {
        if (isSingleplayer()) {
            return 5L
        }

        val player = Player.toMC() ?: return -1L

        return Client.getConnection()?.getPlayerListEntry(player.uuid)?.latency?.toLong()
            ?: toMC()?.ping
            ?: -1L
    }
}
