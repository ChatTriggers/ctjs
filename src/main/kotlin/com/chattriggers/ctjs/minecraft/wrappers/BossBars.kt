package com.chattriggers.ctjs.minecraft.wrappers

import com.chattriggers.ctjs.minecraft.libs.ChatLib
import com.chattriggers.ctjs.mixins.BossBarHudAccessor
import com.chattriggers.ctjs.utils.asMixin
import com.chattriggers.ctjs.utils.getOption
import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.client.gui.hud.BossBarHud
import net.minecraft.client.gui.hud.ClientBossBar
import org.mozilla.javascript.NativeObject
import java.util.UUID
import net.minecraft.entity.boss.BossBar as MCBossBar

object BossBars : CTWrapper<BossBarHud> {
    override val mcValue: BossBarHud
        get() = Client.getMinecraft().inGameHud.bossBarHud

    /**
     * Gets the list of currently shown [BossBar]s
     *
     * @return the currently displayed [BossBar]s
     */
    @JvmStatic
    fun getBossBars(): List<BossBar> {
        return toMC().asMixin<BossBarHudAccessor>().bossBars.values.map(::BossBar)
    }

    /**
     * Gets all [BossBar]s with a given name
     *
     * @param name the name to match
     * @return the [BossBar]s
     */
    @JvmStatic
    fun getBossBarsByName(name: String): List<BossBar> {
        return getBossBars().filter { it.getName() == name }
    }

    /**
     * Adds a new [BossBar] to be displayed
     *
     * Takes a parameter with the following options:
     * - name: The name to appear above the BossBar. Defaults to an empty string
     * - percent: The percent full the BossBar is. Defaults to 1 (full health)
     * - color: The color of the BossBar. Can be any [Color], but defaults to white
     * - sections: The number of notches/sections to appear on the BossBar. Can be any [Style], but
     *             defaults to 1 entire section
     * - darkenSky: Whether the BossBar should darken the screen of the player. Defaults to false
     * - dragonMusic: Whether the BossBar should play dragon music while in the End. Defaults to false
     * - thickenFog: Whether the BossBar should thicken the fog around the player. Defaults to false
     *
     * @param obj An options bag
     *
     * @return the [BossBar] for further modification
     */
    @JvmStatic
    fun addBossBar(obj: NativeObject): BossBar {
        val name = obj.getOption("name", "")
        val percent = obj.getOption("percent", 1f).toFloat().coerceIn(0f..1f)
        val color = Color.from(obj.getOption("color", Color.WHITE))
        val style = Style.from(obj.getOption("sections", Style.ONE))
        val shouldDarkenSky = obj.getOption("darkenSky", false).toBoolean()
        val dragonMusic = obj.getOption("dragonMusic", false).toBoolean()
        val shouldThickenFog = obj.getOption("thickenFog", false).toBoolean()

        val uuid = UUID.randomUUID()

        val bossBar = ClientBossBar(
            uuid,
            UTextComponent(name),
            percent,
            color.toMC(),
            style.toMC(),
            shouldDarkenSky,
            dragonMusic,
            shouldThickenFog
        )

        toMC().asMixin<BossBarHudAccessor>().bossBars[uuid] = bossBar

        return BossBar(bossBar)
    }

    /**
     * Clears all [BossBar]s on screen
     */
    @JvmStatic
    fun clearBossBars() {
        toMC().clear()
    }

    /**
     * Removes all [BossBar]s with the given name
     *
     * @param name the name to match
     */
    @JvmStatic
    fun removeBossBarsByName(name: String) {
        toMC().asMixin<BossBarHudAccessor>().bossBars.values.removeIf {
            UTextComponent(it.name).formattedText == ChatLib.addColor(name)
        }
    }

    /**
     * Removes the given [BossBar]
     *
     * @param bossBar the BossBar to remove
     */
    @JvmStatic
    fun removeBossBar(bossBar: BossBar) {
        toMC().asMixin<BossBarHudAccessor>().bossBars.remove(bossBar.getUUID())
    }

    class BossBar(override val mcValue: ClientBossBar) : CTWrapper<ClientBossBar> {
        /**
         * Gets the UUID of this BossBar
         *
         * @return the uuid
         */
        fun getUUID(): UUID {
            return mcValue.uuid
        }

        /**
         * Gets the name of this BossBar
         *
         * @return the name
         */
        fun getName(): String {
            return UTextComponent(mcValue.name).formattedText
        }

        /**
         * Sets the name of this BossBar
         *
         * @param name the name to set
         */
        fun setName(name: String) = apply {
            mcValue.name = UTextComponent(name)
        }

        /**
         * Gets how full this BossBar is
         *
         * @return how full the BossBar is
         */
        fun getPercent(): Float = mcValue.percent

        /**
         * Sets how full this BossBar is
         *
         * @param percent how full to set this BossBar. Must be between 0 and 1
         */
        fun setPercent(percent: Float) = apply {
            mcValue.percent = percent.coerceIn(0f..1f)
        }

        /**
         * Gets the [Color] of this BossBar
         */
        fun getColor(): Color = Color.fromMC(mcValue.color)

        /**
         * Sets the [Color] of this BossBar
         *
         * @param color the color to set. Can be [Color], [MCBossBar.Color], or a string
         */
        fun setColor(color: Any) = apply {
            mcValue.color = Color.from(color).toMC()
        }

        /**
         * Gets the style of this BossBar. e.g. how many notches are displayed
         */
        fun getStyle(): Style = Style.fromMC(mcValue.style)

        /**
         * Sets the style of this BossBar
         *
         * @param style the style to set. Can be [Style], [MCBossBar.Style], a string,
         * or a number of how many notches to put
         */
        fun setStyle(style: Any) = apply {
            mcValue.style = Style.from(style).toMC()
        }

        /**
         * Gets whether this BossBar darkens the sky
         */
        fun shouldDarkenSky(): Boolean = mcValue.shouldDarkenSky()

        /**
         * Sets whether this BossBar should darken the sky
         *
         * @param darken whether to darken the sky
         */
        fun setShouldDarkenSky(darken: Boolean) = apply {
            mcValue.setDarkenSky(darken)
        }

        /**
         * Gets whether this BossBar will play dragon music.
         * This will do nothing when the player is not in the end dimension
         */
        fun hasDragonMusic(): Boolean = mcValue.hasDragonMusic()

        /**
         * Sets whether this BossBar will play dragon music
         *
         * @param music whether to play dragon music
         */
        fun setHasDragonMusic(music: Boolean) = apply {
            mcValue.setDragonMusic(music)
        }

        /**
         * Gets whether this BossBar should thicken the fog around the player
         */
        fun shouldThickenFog(): Boolean = mcValue.shouldThickenFog()

        /**
         * Sets whether this BossBar should thicken the fog around the player
         *
         * @param fog whether to thicken the fog
         */
        fun setShouldThickenFog(fog: Boolean) = apply {
            mcValue.setThickenFog(fog)
        }

        override fun toString(): String {
            return "BossBar{name=${getName()}, percent=${getPercent()}, color=${getColor()}, " +
                    "style=${getStyle()}, shouldDarkenSky=${shouldDarkenSky()}, " +
                    "hasDragonMusic=${hasDragonMusic()}, shouldThickenFog=${shouldThickenFog()}}"
        }
    }

    enum class Color(override val mcValue: MCBossBar.Color) : CTWrapper<MCBossBar.Color> {
        PINK(MCBossBar.Color.PINK),
        BLUE(MCBossBar.Color.BLUE),
        RED(MCBossBar.Color.RED),
        GREEN(MCBossBar.Color.GREEN),
        YELLOW(MCBossBar.Color.YELLOW),
        PURPLE(MCBossBar.Color.PURPLE),
        WHITE(MCBossBar.Color.WHITE);

        companion object {
            @JvmStatic
            fun fromMC(mcValue: MCBossBar.Color) = values().first { it.mcValue == mcValue }

            @JvmStatic
            fun from(value: Any) = when (value) {
                is String -> valueOf(value)
                is MCBossBar.Color -> fromMC(value)
                is Color -> value
                else -> throw IllegalArgumentException("Cannot create BossBars.Color from $value")
            }
        }
    }

    enum class Style(override val mcValue: MCBossBar.Style, val sections: Int) : CTWrapper<MCBossBar.Style> {
        ONE(MCBossBar.Style.PROGRESS, 1),
        SIX(MCBossBar.Style.NOTCHED_6, 6),
        TEN(MCBossBar.Style.NOTCHED_10, 10),
        TWELVE(MCBossBar.Style.NOTCHED_12, 12),
        TWENTY(MCBossBar.Style.NOTCHED_20, 20);

        companion object {
            @JvmStatic
            fun fromMC(mcValue: MCBossBar.Style) = values().first { it.mcValue == mcValue }

            @JvmStatic
            fun from(value: Any) = when (value) {
                is String -> valueOf(value)
                is MCBossBar.Style -> fromMC(value)
                is Style -> value
                is Int -> values().first { it.sections == value }
                else -> throw IllegalArgumentException("Cannot create BossBars.Style from $value")
            }
        }
    }
}
