package com.chattriggers.ctjs.minecraft.wrappers

import com.chattriggers.ctjs.utils.MCDifficulty
import com.chattriggers.ctjs.utils.MCGraphicsMode
import gg.essential.universal.UMinecraft
import net.minecraft.client.option.GameOptions
import net.minecraft.client.option.GraphicsMode
import net.minecraft.client.render.entity.PlayerModelPart
import net.minecraft.sound.SoundCategory
import net.minecraft.client.option.ChatVisibility as MCChatVisibility
import net.minecraft.client.option.CloudRenderMode as MCCloudRenderMode
import net.minecraft.client.option.ParticlesMode as MCParticlesMode

class Settings : CTWrapper<GameOptions> {
    override val mcValue get() = UMinecraft.getSettings()

    @Deprecated("Use toMC", ReplaceWith("toMC()"))
    fun getSettings() = toMC()

    fun getFOV() = toMC().fov.value

    fun setFOV(fov: Int) {
        toMC().fov.value = fov
    }

    fun getDifficulty() = World.getDifficulty()

    // TODO*(breaking): Removed setDifficulty

    val skin = SkinWrapper()
    val sound = SoundWrapper()
    val chat = ChatWrapper()
    val video = VideoWrapper()

    // TODO*(breaking): Changed all of these names to indicate they involve the parts
    //                 being enabled, not returning the parts themselves
    inner class SkinWrapper {
        fun isCapeEnabled() = toMC().isPlayerModelPartEnabled(PlayerModelPart.CAPE)

        fun setCapeEnabled(toggled: Boolean) {
            toMC().togglePlayerModelPart(PlayerModelPart.CAPE, toggled)
        }

        fun isJacketEnabled() = toMC().isPlayerModelPartEnabled(PlayerModelPart.JACKET)

        fun setJacketEnabled(toggled: Boolean) {
            toMC().togglePlayerModelPart(PlayerModelPart.JACKET, toggled)
        }

        fun isLeftSleeveEnabled() = toMC().isPlayerModelPartEnabled(PlayerModelPart.LEFT_SLEEVE)

        fun setLeftSleeveEnabled(toggled: Boolean) {
            toMC().togglePlayerModelPart(PlayerModelPart.LEFT_SLEEVE, toggled)
        }

        fun isRightSleeveEnabled() = toMC().isPlayerModelPartEnabled(PlayerModelPart.RIGHT_SLEEVE)

        fun setRightSleeveEnabled(toggled: Boolean) {
            toMC().togglePlayerModelPart(PlayerModelPart.RIGHT_SLEEVE, toggled)
        }

        fun isLeftPantsLegEnabled() = toMC().isPlayerModelPartEnabled(PlayerModelPart.LEFT_PANTS_LEG)

        fun setLeftPantsLegEnabled(toggled: Boolean) {
            toMC().togglePlayerModelPart(PlayerModelPart.LEFT_PANTS_LEG, toggled)
        }

        fun isRightPantsLegEnabled() = toMC().isPlayerModelPartEnabled(PlayerModelPart.RIGHT_PANTS_LEG)

        fun setRightPantsLegEnabled(toggled: Boolean) {
            toMC().togglePlayerModelPart(PlayerModelPart.RIGHT_PANTS_LEG, toggled)
        }

        fun isHatEnabled() = toMC().isPlayerModelPartEnabled(PlayerModelPart.HAT)

        fun setHatEnabled(toggled: Boolean) {
            toMC().togglePlayerModelPart(PlayerModelPart.HAT, toggled)
        }
    }

    inner class SoundWrapper {
        fun getMasterVolume() = toMC().getSoundVolumeOption(SoundCategory.MASTER).value

        fun setMasterVolume(level: Double) {
            toMC().getSoundVolumeOption(SoundCategory.MASTER).value = level
        }

        fun getMusicVolume() = toMC().getSoundVolumeOption(SoundCategory.MUSIC).value

        fun setMusicVolume(level: Double) {
            toMC().getSoundVolumeOption(SoundCategory.MUSIC).value = level
        }

        fun getNoteblockVolume() = toMC().getSoundVolumeOption(SoundCategory.RECORDS).value

        fun setNoteblockVolume(level: Double) {
            toMC().getSoundVolumeOption(SoundCategory.RECORDS).value = level
        }

        fun getWeather() = toMC().getSoundVolumeOption(SoundCategory.WEATHER).value

        fun setWeather(level: Double) {
            toMC().getSoundVolumeOption(SoundCategory.WEATHER).value = level
        }

        fun getBlocks() = toMC().getSoundVolumeOption(SoundCategory.BLOCKS).value

        fun setBlocks(level: Double) {
            toMC().getSoundVolumeOption(SoundCategory.BLOCKS).value = level
        }

        fun getHostileCreatures() = toMC().getSoundVolumeOption(SoundCategory.HOSTILE).value

        fun setHostileCreatures(level: Double) {
            toMC().getSoundVolumeOption(SoundCategory.HOSTILE).value = level
        }

        fun getFriendlyCreatures() = toMC().getSoundVolumeOption(SoundCategory.NEUTRAL).value

        fun setFriendlyCreatures(level: Double) {
            toMC().getSoundVolumeOption(SoundCategory.NEUTRAL).value = level
        }

        fun getPlayers() = toMC().getSoundVolumeOption(SoundCategory.PLAYERS).value

        fun setPlayers(level: Double) {
            toMC().getSoundVolumeOption(SoundCategory.PLAYERS).value = level
        }

        fun getAmbient() = toMC().getSoundVolumeOption(SoundCategory.AMBIENT).value

        fun setAmbient(level: Double) {
            toMC().getSoundVolumeOption(SoundCategory.AMBIENT).value = level
        }
    }

    inner class VideoWrapper {
        // TODO*(breaking): Add "mode" suffix to this method name and use the enum instead of Boolean
        fun getGraphicsMode() = GraphicsMode.fromMC(toMC().graphicsMode.value)

        fun setGraphicsMode(mode: GraphicsMode) {
            toMC().graphicsMode.value = mode.toMC()
        }

        fun getRenderDistance() = toMC().viewDistance.value

        fun setRenderDistance(distance: Int) {
            toMC().viewDistance.value = distance
        }

        fun getSmoothLighting() = toMC().ao.value

        fun setSmoothLighting(enabled: Boolean) {
            toMC().ao.value = enabled
        }

        fun getMaxFrameRate() = toMC().maxFps.value

        fun setMaxFrameRate(frameRate: Int) {
            toMC().maxFps.value = frameRate
        }

        // TODO*(breaking): remove get3dAnaglyph

        fun getBobbing() = toMC().bobView.value

        fun setBobbing(toggled: Boolean) {
            toMC().bobView.value = toggled
        }

        fun getGuiScale() = toMC().guiScale.value

        fun setGuiScale(scale: Int) {
            toMC().guiScale.value = scale
        }

        fun getBrightness() = toMC().gamma.value

        fun setBrightness(brightness: Double) {
            toMC().gamma.value = brightness
        }

        // TODO*(breaking): Use enum instead of Int
        fun getClouds() = CloudRenderMode.fromMC(toMC().cloudRenderMode.value)

        fun setClouds(clouds: CloudRenderMode) {
            toMC().cloudRenderMode.value = clouds.toMC()
        }

        // TODO*(breaking): Use enum instead of Int
        fun getParticles() = ParticlesMode.fromMC(toMC().particles.value)

        fun setParticles(particles: ParticlesMode) {
            toMC().particles.value = particles.toMC()
        }

        fun getFullscreen() = toMC().fullscreen.value

        fun setFullscreen(toggled: Boolean) {
            toMC().fullscreen.value = toggled
        }

        fun getVsync() = toMC().enableVsync.value

        fun setVsync(toggled: Boolean) {
            toMC().enableVsync.value = toggled
        }

        fun getMipmapLevels() = toMC().mipmapLevels.value

        fun setMipmapLevels(mipmapLevels: Int) {
            toMC().mipmapLevels.value = mipmapLevels
        }

        fun getEntityShadows() = toMC().entityShadows.value

        fun setEntityShadows(toggled: Boolean) {
            toMC().entityShadows.value = toggled
        }
    }

    inner class ChatWrapper {
        // TODO*(breaking): Use enum instead of String
        fun getVisibility() = ChatVisibility.fromMC(toMC().chatVisibility.value)

        fun setVisibility(visibility: ChatVisibility) {
            toMC().chatVisibility.value = visibility.toMC()
        }

        fun getColors() = toMC().chatColors.value

        fun setColors(toggled: Boolean) {
            toMC().chatColors.value = toggled
        }

        fun getWebLinks() = toMC().chatLinks.value

        fun setWebLinks(toggled: Boolean) {
            toMC().chatLinks.value = toggled
        }

        fun getOpacity() = toMC().chatOpacity.value

        fun setOpacity(opacity: Double) {
            toMC().chatOpacity.value = opacity
        }

        fun getPromptOnWebLinks() = toMC().chatLinksPrompt.value

        fun setPromptOnWebLinks(toggled: Boolean) {
            toMC().chatLinksPrompt.value = toggled
        }

        fun getScale() = toMC().chatScale.value

        fun setScale(scale: Double) {
            toMC().chatScale.value = scale
        }

        fun getFocusedHeight() = toMC().chatHeightFocused.value

        fun setFocusedHeight(height: Double) {
            toMC().chatHeightFocused.value = height
        }

        fun getUnfocusedHeight() = toMC().chatHeightUnfocused.value

        fun setUnfocusedHeight(height: Double) {
            toMC().chatHeightUnfocused.value = height
        }

        fun getWidth() = toMC().chatWidth.value

        fun setWidth(width: Double) {
            toMC().chatWidth.value = width
        }

        fun getReducedDebugInfo() = toMC().reducedDebugInfo.value

        fun setReducedDebugInfo(toggled: Boolean) {
            toMC().reducedDebugInfo.value = toggled
        }
    }

    enum class CloudRenderMode(override val mcValue: MCCloudRenderMode) : CTWrapper<MCCloudRenderMode> {
        OFF(MCCloudRenderMode.OFF),
        FAST(MCCloudRenderMode.FAST),
        FANCY(MCCloudRenderMode.FANCY);

        companion object {
            @JvmStatic
            fun fromMC(mcValue: MCCloudRenderMode) = values().first { it.mcValue == mcValue }

            @JvmStatic
            fun from(value: Any) = when (value) {
                is String -> CloudRenderMode.valueOf(value)
                is MCCloudRenderMode -> fromMC(value)
                is CloudRenderMode -> value
                else -> throw IllegalArgumentException("Cannot create CloudRenderMode from $value")
            }
        }
    }

    enum class ParticlesMode(override val mcValue: MCParticlesMode) : CTWrapper<MCParticlesMode> {
        ALL(MCParticlesMode.ALL),
        DECREASED(MCParticlesMode.DECREASED),
        MINIMAL(MCParticlesMode.MINIMAL);

        companion object {
            @JvmStatic
            fun fromMC(mcValue: MCParticlesMode) = values().first { it.mcValue == mcValue }

            @JvmStatic
            fun from(value: Any) = when (value) {
                is String -> ParticlesMode.valueOf(value)
                is MCParticlesMode -> fromMC(value)
                is ParticlesMode -> value
                else -> throw IllegalArgumentException("Cannot create ParticlesMode from $value")
            }
        }
    }

    enum class ChatVisibility(override val mcValue: MCChatVisibility) : CTWrapper<MCChatVisibility> {
        FULL(MCChatVisibility.FULL),
        SYSTEM(MCChatVisibility.SYSTEM),
        HIDDEN(MCChatVisibility.HIDDEN);

        companion object {
            @JvmStatic
            fun fromMC(mcValue: MCChatVisibility) = values().first { it.mcValue == mcValue }

            @JvmStatic
            fun from(value: Any) = when (value) {
                is String -> ChatVisibility.valueOf(value)
                is MCChatVisibility -> fromMC(value)
                is ChatVisibility -> value
                else -> throw IllegalArgumentException("Cannot create ChatVisibility from $value")
            }
        }
    }

    enum class Difficulty(override val mcValue: MCDifficulty) : CTWrapper<MCDifficulty> {
        PEACEFUL(MCDifficulty.PEACEFUL),
        EASY(MCDifficulty.EASY),
        NORMAL(MCDifficulty.NORMAL),
        HARD(MCDifficulty.HARD);

        companion object {
            @JvmStatic
            fun fromMC(mcValue: MCDifficulty) = values().first { it.mcValue == mcValue }
        }
    }

    enum class GraphicsMode(override val mcValue: MCGraphicsMode) : CTWrapper<MCGraphicsMode> {
        FAST(MCGraphicsMode.FAST),
        FANCY(MCGraphicsMode.FANCY),
        FABULOUS(MCGraphicsMode.FABULOUS);

        companion object {
            @JvmStatic
            fun fromMC(mcValue: MCGraphicsMode) = values().first { it.mcValue == mcValue }
        }
    }
}
