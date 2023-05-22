package com.chattriggers.ctjs.minecraft.wrappers

import gg.essential.universal.wrappers.message.UTextComponent

object Server {
    /**
     * Gets the current server's IP, or "localhost" if the player
     * is in a single-player world.
     *
     * @return The IP of the current server
     */
    @JvmStatic
    fun getIP(): String {
        if (Client.getMinecraft().isInSingleplayer)
            return "localhost"

        return Client.getMinecraft().currentServerEntry?.address ?: ""
    }

    /**
     * Gets the current server's name, or "SinglePlayer" if the player
     * is in a single-player world.
     *
     * @return The name of the current server
     */
    @JvmStatic
    fun getName(): String {
        if (Client.getMinecraft().isInSingleplayer)
            return "SinglePlayer"

        return Client.getMinecraft().currentServerEntry?.name ?: ""
    }

    /**
     * Gets the current server's MOTD, or "SinglePlayer" if the player
     * is in a single-player world.
     *
     * @return The MOTD of the current server
     */
    @JvmStatic
    fun getMOTD(): String {
        if (Client.getMinecraft().isInSingleplayer)
            return "SinglePlayer"

        return Client.getMinecraft().currentServerEntry?.label?.let(::UTextComponent)?.formattedText ?: ""
    }

    /**
     * Gets the ping to the current server, or 5 if the player
     * is in a single-player world.
     *
     * @return The ping to the current server
     */
    @JvmStatic
    fun getPing(): Long {
        val player = Player.getPlayer()

        if (player == null
            || Client.getMinecraft().isInSingleplayer
            || Client.getMinecraft().currentServerEntry == null
        ) {
            return 5L
        }

        return Client.getConnection()?.getPlayerListEntry(player.uuid)?.latency?.toLong()
            ?: Client.getMinecraft().currentServerEntry?.ping ?: -1L
    }
}
