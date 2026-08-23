package com.chattriggers.ctjs.api.compat

import com.chattriggers.ctjs.engine.LogType
import com.chattriggers.ctjs.engine.printToConsole
import org.slf4j.LoggerFactory
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Shared helpers for the opt-in 1.8.9 compatibility surface.
 */
object LegacyCompatibility {
    private val logger = LoggerFactory.getLogger(LegacyCompatibility::class.java)
    private val emittedWarnings = ConcurrentHashMap.newKeySet<String>()

    private val legacySoundNames = mapOf(
        "gui.button.press" to "ui.button.click",
        "note.bd" to "block.note_block.basedrum",
        "note.bass" to "block.note_block.bass",
        "note.bassattack" to "block.note_block.bass",
        "note.harp" to "block.note_block.harp",
        "note.hat" to "block.note_block.hat",
        "note.pling" to "block.note_block.pling",
        "note.snare" to "block.note_block.snare",
        "random.bow" to "entity.arrow.shoot",
        "random.break" to "entity.item.break",
        "random.burp" to "entity.player.burp",
        "random.chestclosed" to "block.chest.close",
        "random.chestopen" to "block.chest.open",
        "random.click" to "ui.button.click",
        "random.drink" to "entity.generic.drink",
        "random.eat" to "entity.generic.eat",
        "random.explode" to "entity.generic.explode",
        "random.fizz" to "block.fire.extinguish",
        "random.fuse" to "entity.tnt.primed",
        "random.glass" to "block.glass.break",
        "random.levelup" to "entity.player.levelup",
        "random.orb" to "entity.experience_orb.pickup",
        "random.pop" to "entity.item.pickup",
        "random.splash" to "entity.generic.splash",
        "random.successful_hit" to "entity.arrow.hit_player",
        "fireworks.largeblast" to "entity.firework_rocket.large_blast",
        "fireworks.launch" to "entity.firework_rocket.launch",
        "fireworks.twinkle" to "entity.firework_rocket.twinkle",
    )

    @JvmStatic
    fun warnDirectJavaAccess(className: String) {
        val category = when {
            className.startsWith("net.minecraftforge.") -> "Forge"
            className.startsWith("net.minecraft.network.") -> "Minecraft packet"
            className.startsWith("net.minecraft.") -> "Minecraft"
            else -> return
        }

        warnOnce(
            "java:$className",
            "[CTJS Compatibility] Direct $category class access is not converted: $className. " +
                "This legacy module may require a manual port."
        )
    }

    @JvmStatic
    fun mapLegacySoundName(name: String): String {
        val normalized = name.trim().lowercase(Locale.ROOT)
        return legacySoundNames[normalized] ?: normalized
    }

    @JvmStatic
    fun warnOnce(key: String, message: String) {
        if (emittedWarnings.add(key)) {
            message.printToConsole(LogType.WARN)
            logger.warn(message)
        }
    }

    internal fun normalizeFormattedText(text: String): String {
        var start = 0
        while (start + 1 < text.length && text[start] == '\u00a7' && text[start + 1].equals('r', ignoreCase = true))
            start += 2

        return if (start == 0) text else text.substring(start)
    }
}
