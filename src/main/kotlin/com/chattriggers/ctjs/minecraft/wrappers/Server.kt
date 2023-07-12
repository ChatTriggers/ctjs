package com.chattriggers.ctjs.minecraft.wrappers

import com.chattriggers.ctjs.minecraft.objects.TextComponent
import net.minecraft.client.network.ServerInfo

object Server : CTWrapper<ServerInfo?> {
    override val mcValue get() = Client.getMinecraft().currentServerEntry

    fun isSingleplayer(): Boolean = Client.getMinecraft().isInSingleplayer

    /**
     * Gets the current server's IP, or "localhost" if the player
     * is in a single-player world.
     *
     * @return The IP of the current server
     */
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
    fun getMOTD(): String {
        if (isSingleplayer())
            return "SinglePlayer"

        return toMC()?.label?.let(::TextComponent)?.formattedText ?: ""
    }

    // TODO(breaking): Return -1 if not in a world
    /**
     * Gets the ping to the current server, or 5 if the player
     * is in a single-player world. Returns -1 if not in a world
     *
     * @return The ping to the current server
     */
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
