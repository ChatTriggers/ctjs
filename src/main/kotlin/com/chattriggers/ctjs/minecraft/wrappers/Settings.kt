package com.chattriggers.ctjs.minecraft.wrappers

import gg.essential.universal.UMinecraft
import net.minecraft.client.option.GraphicsMode
import net.minecraft.client.render.entity.PlayerModelPart
import net.minecraft.sound.SoundCategory
import net.minecraft.client.option.ChatVisibility as MCChatVisibility
import net.minecraft.client.option.CloudRenderMode as MCCloudRenderMode
import net.minecraft.client.option.ParticlesMode as MCParticlesMode

class Settings {
    fun getSettings() = UMinecraft.getSettings()

    fun getFOV() = getSettings().fov.value

    fun setFOV(fov: Int) {
        getSettings().fov.value = fov
    }

    // TODO: Add Difficulty enum to this class if possible, or add a wrapper
    fun getDifficulty() = UMinecraft.getWorld()?.difficulty

    // TODO(breaking): Removed setDifficulty

    // TODO(breaking): Changed all of these names to indicate they involve the parts
    //                 being enabled, not returning the parts themselves
    val skin = object {
        fun isCapeEnabled() = getSettings().isPlayerModelPartEnabled(PlayerModelPart.CAPE)

        fun setCapeEnabled(toggled: Boolean) {
            getSettings().togglePlayerModelPart(PlayerModelPart.CAPE, toggled)
        }

        fun isJacketEnabled() = getSettings().isPlayerModelPartEnabled(PlayerModelPart.JACKET)

        fun setJacketEnabled(toggled: Boolean) {
            getSettings().togglePlayerModelPart(PlayerModelPart.JACKET, toggled)
        }

        fun isLeftSleeveEnabled() = getSettings().isPlayerModelPartEnabled(PlayerModelPart.LEFT_SLEEVE)

        fun setLeftSleeveEnabled(toggled: Boolean) {
            getSettings().togglePlayerModelPart(PlayerModelPart.LEFT_SLEEVE, toggled)
        }

        fun isRightSleeveEnabled() = getSettings().isPlayerModelPartEnabled(PlayerModelPart.RIGHT_SLEEVE)

        fun setRightSleeveEnabled(toggled: Boolean) {
            getSettings().togglePlayerModelPart(PlayerModelPart.RIGHT_SLEEVE, toggled)
        }

        fun isLeftPantsLegEnabled() = getSettings().isPlayerModelPartEnabled(PlayerModelPart.LEFT_PANTS_LEG)

        fun setLeftPantsLegEnabled(toggled: Boolean) {
            getSettings().togglePlayerModelPart(PlayerModelPart.LEFT_PANTS_LEG, toggled)
        }

        fun isRightPantsLegEnabled() = getSettings().isPlayerModelPartEnabled(PlayerModelPart.RIGHT_PANTS_LEG)

        fun setRightPantsLegEnabled(toggled: Boolean) {
            getSettings().togglePlayerModelPart(PlayerModelPart.RIGHT_PANTS_LEG, toggled)
        }

        fun isHatEnabled() = getSettings().isPlayerModelPartEnabled(PlayerModelPart.HAT)

        fun setHatEnabled(toggled: Boolean) {
            getSettings().togglePlayerModelPart(PlayerModelPart.HAT, toggled)
        }
    }

    val sound = object {
        fun getMasterVolume() = getSettings().getSoundVolumeOption(SoundCategory.MASTER).value

        fun setMasterVolume(level: Double) {
            getSettings().getSoundVolumeOption(SoundCategory.MASTER).value = level
        }

        fun getMusicVolume() = getSettings().getSoundVolumeOption(SoundCategory.MUSIC).value

        fun setMusicVolume(level: Double) {
            getSettings().getSoundVolumeOption(SoundCategory.MUSIC).value = level
        }

        fun getNoteblockVolume() = getSettings().getSoundVolumeOption(SoundCategory.RECORDS).value

        fun setNoteblockVolume(level: Double) {
            getSettings().getSoundVolumeOption(SoundCategory.RECORDS).value = level
        }

        fun getWeather() = getSettings().getSoundVolumeOption(SoundCategory.WEATHER).value

        fun setWeather(level: Double) {
            getSettings().getSoundVolumeOption(SoundCategory.WEATHER).value = level
        }

        fun getBlocks() = getSettings().getSoundVolumeOption(SoundCategory.BLOCKS).value

        fun setBlocks(level: Double) {
            getSettings().getSoundVolumeOption(SoundCategory.BLOCKS).value = level
        }

        fun getHostileCreatures() = getSettings().getSoundVolumeOption(SoundCategory.HOSTILE).value

        fun setHostileCreatures(level: Double) {
            getSettings().getSoundVolumeOption(SoundCategory.HOSTILE).value = level
        }

        fun getFriendlyCreatures() = getSettings().getSoundVolumeOption(SoundCategory.NEUTRAL).value

        fun setFriendlyCreatures(level: Double) {
            getSettings().getSoundVolumeOption(SoundCategory.NEUTRAL).value = level
        }

        fun getPlayers() = getSettings().getSoundVolumeOption(SoundCategory.PLAYERS).value

        fun setPlayers(level: Double) {
            getSettings().getSoundVolumeOption(SoundCategory.PLAYERS).value = level
        }

        fun getAmbient() = getSettings().getSoundVolumeOption(SoundCategory.AMBIENT).value

        fun setAmbient(level: Double) {
            getSettings().getSoundVolumeOption(SoundCategory.AMBIENT).value = level
        }
    }

    val video = object {
        // TODO: Add this to Settings object or add a wrapper for it
        // TODO(breaking): Add "mode" suffix to this method name and use the enum instead of Boolean
        fun getGraphicsMode() = getSettings().graphicsMode.value

        fun setGraphicsMode(mode: GraphicsMode) {
            getSettings().graphicsMode.value = mode
        }

        fun getRenderDistance() = getSettings().viewDistance.value

        fun setRenderDistance(distance: Int) {
            getSettings().viewDistance.value = distance
        }

        fun getSmoothLighting() = getSettings().ao.value

        fun setSmoothLighting(enabled: Boolean) {
            getSettings().ao.value = enabled
        }

        fun getMaxFrameRate() = getSettings().maxFps.value

        fun setMaxFrameRate(frameRate: Int) {
            getSettings().maxFps.value = frameRate
        }

        // TODO(breaking): remove get3dAnaglyph

        fun getBobbing() = getSettings().bobView.value

        fun setBobbing(toggled: Boolean) {
            getSettings().bobView.value = toggled
        }

        fun getGuiScale() = getSettings().guiScale.value

        fun setGuiScale(scale: Int) {
            getSettings().guiScale.value = scale
        }

        fun getBrightness() = getSettings().gamma.value

        fun setBrightness(brightness: Double) {
            getSettings().gamma.value = brightness
        }

        // TODO(breaking): Use enum instead of Int
        fun getClouds() = CloudRenderMode.fromMC(getSettings().cloudRenderMode.value)

        fun setClouds(clouds: CloudRenderMode) {
            getSettings().cloudRenderMode.value = clouds.toMC()
        }

        // TODO(breaking): Use enum instead of Int
        fun getParticles() = ParticlesMode.fromMC(getSettings().particles.value)

        fun setParticles(particles: ParticlesMode) {
            getSettings().particles.value = particles.toMC()
        }

        fun getFullscreen() = getSettings().fullscreen.value

        fun setFullscreen(toggled: Boolean) {
            getSettings().fullscreen.value = toggled
        }

        fun getVsync() = getSettings().enableVsync.value

        fun setVsync(toggled: Boolean) {
            getSettings().enableVsync.value = toggled
        }

        fun getMipmapLevels() = getSettings().mipmapLevels.value

        fun setMipmapLevels(mipmapLevels: Int) {
            getSettings().mipmapLevels.value = mipmapLevels
        }

        // TODO: Does this exist?
        // fun getVBOs() = getSettings().useVbo
        //
        // fun setVBOs(toggled: Boolean) {
        //     getSettings().useVbo = toggled
        // }

        fun getEntityShadows() = getSettings().entityShadows.value

        fun setEntityShadows(toggled: Boolean) {
            getSettings().entityShadows.value = toggled
        }
    }

    val chat = object {
        // TODO(breaking): Use enum instead of String
        fun getVisibility() = ChatVisibility.fromMC(getSettings().chatVisibility.value)

        fun setVisibility(visibility: ChatVisibility) {
            getSettings().chatVisibility.value = visibility.toMC()
        }

        fun getColors() = getSettings().chatColors.value

        fun setColors(toggled: Boolean) {
            getSettings().chatColors.value = toggled
        }

        fun getWebLinks() = getSettings().chatLinks.value

        fun setWebLinks(toggled: Boolean) {
            getSettings().chatLinks.value = toggled
        }

        fun getOpacity() = getSettings().chatOpacity.value

        fun setOpacity(opacity: Double) {
            getSettings().chatOpacity.value = opacity
        }

        fun getPromptOnWebLinks() = getSettings().chatLinksPrompt.value

        fun setPromptOnWebLinks(toggled: Boolean) {
            getSettings().chatLinksPrompt.value = toggled
        }

        fun getScale() = getSettings().chatScale.value

        fun setScale(scale: Double) {
            getSettings().chatScale.value = scale
        }

        fun getFocusedHeight() = getSettings().chatHeightFocused.value

        fun setFocusedHeight(height: Double) {
            getSettings().chatHeightFocused.value = height
        }

        fun getUnfocusedHeight() = getSettings().chatHeightUnfocused.value

        fun setUnfocusedHeight(height: Double) {
            getSettings().chatHeightUnfocused.value = height
        }

        fun getWidth() = getSettings().chatWidth.value

        fun setWidth(width: Double) {
            getSettings().chatWidth.value = width
        }

        fun getReducedDebugInfo() = getSettings().reducedDebugInfo.value

        fun setReducedDebugInfo(toggled: Boolean) {
            getSettings().reducedDebugInfo.value = toggled
        }
    }

    enum class CloudRenderMode(private val mcValue: MCCloudRenderMode) {
        OFF(MCCloudRenderMode.OFF),
        FAST(MCCloudRenderMode.FAST),
        FANCY(MCCloudRenderMode.FANCY);

        fun toMC() = mcValue

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

    enum class ParticlesMode(private val mcValue: MCParticlesMode) {
        ALL(MCParticlesMode.ALL),
        DECREASED(MCParticlesMode.DECREASED),
        MINIMAL(MCParticlesMode.MINIMAL);

        fun toMC() = mcValue

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

    enum class ChatVisibility(private val mcValue: MCChatVisibility) {
        FULL(MCChatVisibility.FULL),
        SYSTEM(MCChatVisibility.SYSTEM),
        HIDDEN(MCChatVisibility.HIDDEN);

        fun toMC() = mcValue

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
}
