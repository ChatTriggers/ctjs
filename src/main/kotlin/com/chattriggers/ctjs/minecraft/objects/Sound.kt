package com.chattriggers.ctjs.minecraft.objects

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.minecraft.wrappers.CTWrapper
import com.chattriggers.ctjs.minecraft.wrappers.Client
import com.chattriggers.ctjs.minecraft.wrappers.Player
import com.chattriggers.ctjs.minecraft.wrappers.World
import com.chattriggers.ctjs.mixins.AbstractSoundInstanceAccessor
import com.chattriggers.ctjs.mixins.sound.SoundAccessor
import com.chattriggers.ctjs.mixins.sound.SoundManagerAccessor
import com.chattriggers.ctjs.mixins.sound.SoundSystemAccessor
import com.chattriggers.ctjs.utils.MCSound
import com.chattriggers.ctjs.utils.asMixin
import gg.essential.universal.UMinecraft
import net.minecraft.client.sound.MovingSoundInstance
import net.minecraft.client.sound.Sound.RegistrationType
import net.minecraft.client.sound.SoundInstance.AttenuationType as MCAttenuationType
import net.minecraft.client.sound.WeightedSoundSet
import net.minecraft.resource.*
import net.minecraft.resource.metadata.ResourceMetadata
import net.minecraft.resource.metadata.ResourceMetadataReader
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier
import net.minecraft.util.math.random.Random
import org.mozilla.javascript.NativeObject
import java.io.File
import java.io.InputStream

// TODO(breaking): Changed a bunch of fields on this class
/**
 * Instances a new Sound with certain properties. These properties
 * should be passed through as a normal JavaScript object.
 *
 * REQUIRED:
 * - source (String) - filename, relative to ChatTriggers assets directory
 *
 * OPTIONAL:
 * - stream (boolean) - whether to stream this sound rather than preload it (should be true for large files), defaults to false
 *
 * CONFIGURABLE (can be set in config object, or changed later):
 * - category (SoundCategory) - which category this sound should be a part of, see [setCategory].
 * - volume (float) - volume of the sound, see [setVolume]
 * - pitch (float) - pitch of the sound, see [setPitch]
 * - x, y, z (float) - location of the sound, see [setPosition]. Defaults to the players position.
 * - attenuationType (AttenuationType) - fade out type of the sound, see [setAttenuationType]
 * - attenuation (int) - The attenuation distance, see [setAttenuation]
 * - loop (boolean) - whether to loop this sound over and over, defaults to false
 * - loopDelay (int) - Ticks to delay between looping this sound
 *
 * @param config the JavaScript config object
 */
class Sound(private val config: NativeObject) {
    private lateinit var identifier: Identifier
    private lateinit var soundImpl: SoundImpl
    private lateinit var sound: MCSound

    private var volume = 1f
    private var pitch = 1f
    private var isPaused = false

    private fun bootstrap() {
        if (::sound.isInitialized)
            return

        CTJS.sounds.add(this)

        val soundManagerAccessor = UMinecraft.getMinecraft().soundManager.asMixin<SoundManagerAccessor>()

        val source = config["source"]?.toString() ?: throw IllegalArgumentException("Sound source is null.")
        val loop = config.getOrDefault("loop", false) as Boolean
        val loopDelay = (config.getOrDefault("loopDelay", 0) as Number).toInt()
        val stream = config.getOrDefault("stream", false) as Boolean
        val x = (config.getOrDefault("x", Player.getX()) as Number).toDouble()
        val y = (config.getOrDefault("y", Player.getY()) as Number).toDouble()
        val z = (config.getOrDefault("z", Player.getZ()) as Number).toDouble()
        val attenuation = (config.getOrDefault("attenuation", 16) as Number).toInt()
        val category = config["category"]?.let(Category::from) ?: Category.MASTER
        val attenuationType = config["attenuationType"]?.let(AttenuationType::from) ?: AttenuationType.LINEAR

        identifier = makeIdentifier(source)
        val soundFile = File(CTJS.assetsDir, source)
        require(soundFile.exists()) { "Cannot find sound resource \"$source\"" }

        val resource = Resource(CTResourcePack, soundFile::inputStream, ResourceMetadata::NONE)
        soundManagerAccessor.soundResources[identifier.withPrefixedPath("sounds/").withSuffixedPath(".ogg")] = resource

        soundImpl = SoundImpl(SoundEvent.of(identifier), category.toMC(), attenuationType.toMC())
        sound = MCSound(
            identifier.toString(),
            { this.volume },
            { this.pitch },
            1,
            RegistrationType.FILE,
            stream,
            false,
            attenuation,
        )

        soundManagerAccessor.sounds[identifier] = WeightedSoundSet(identifier, null).apply {
            add(sound)
        }

        setPosition(x, y, z)
        setLoop(loop)
        setLoopDelay(loopDelay)

        if (config["volume"] != null) {
            setVolume((config["volume"] as Number).toFloat())
        }

        if (config["pitch"] != null) {
            setPitch((config["pitch"] as Number).toFloat())
        }
    }

    fun destroy() {
        stop()
        val soundManagerAccessor = UMinecraft.getMinecraft().soundManager.asMixin<SoundManagerAccessor>()
        soundManagerAccessor.sounds.remove(identifier)
        soundManagerAccessor.soundResources.remove(identifier)
    }

    /**
     * Sets the category of this sound, making it respect the Player's sound volume sliders.
     * Options are: master, music, record, weather, block, hostile, neutral, player, and ambient
     *
     * @param category the category
     */
    // TODO(breaking): Changed from string to enum
    fun setCategory(category: Category) = apply {
        soundImpl.categoryOverride = category.toMC()
    }

    /**
     * Sets this sound's volume.
     * Will override the category if called after [setCategory], but not if called before.
     *
     * @param volume New volume, float value ( 0.0f - 1.0f ).
     */
    fun setVolume(volume: Float) = apply { this.volume = volume }

    fun getVolume() = volume

    /**
     * Updates the position of this sound
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     */
    fun setPosition(x: Double, y: Double, z: Double) = apply {
        soundImpl.setPosition(x, y, z)
    }

    /**
     * Sets this sound's pitch.
     *
     * @param pitch A float value ( 0.5f - 2.0f ).
     */
    fun setPitch(pitch: Float) = apply { this.pitch = pitch }

    fun getPitch() = pitch

    /**
     * Sets the attenuation type (fade out over space) of the sound
     *
     * @param attenuationType the type of Attenuation
     */
    // TODO(breaking): Use enum instead of Int and changed name
    fun setAttenuationType(attenuationType: AttenuationType) = apply {
        soundImpl.attenuationType = attenuationType.toMC()
    }

    /**
     * Sets the attenuation distance of the sound
     */
    fun setAttenuation(attenuation: Int) = apply {
        sound.asMixin<SoundAccessor>().setAttenuation(attenuation)
    }

    fun setLoop(loop: Boolean) = apply {
        soundImpl.asMixin<AbstractSoundInstanceAccessor>().setRepeat(loop)
    }

    fun setLoopDelay(loopDelay: Int) = apply {
        soundImpl.asMixin<AbstractSoundInstanceAccessor>().setRepeatDelay(loopDelay)
    }

    /**
     * Plays/resumes the sound
     */
    @JvmOverloads
    fun play(delay: Int = 0) {
        bootstrap()

        // soundSystem.play() does a lot of setup and, most importantly, creates a new
        // source for the sound. If we have previously paused, we avoid all that setup
        // and instead directly invoke the play method from OpenAL via Source.play
        if (!isPaused) {
            soundSystem.play(soundImpl, delay)
        } else {
            Client.scheduleTask(delay) {
                isPaused = false
                soundSystem.asMixin<SoundSystemAccessor>().sources[soundImpl]?.run {
                    it.play()
                }
            }
        }
    }

    /**
     * Pauses the sound, to be resumed later
     */
    fun pause() {
        bootstrap()
        isPaused = true
        soundSystem.asMixin<SoundSystemAccessor>().sources[soundImpl]?.run {
            it.pause()
        }
    }

    /**
     * Completely stops the sound
     */
    fun stop() {
        bootstrap()
        soundSystem.stop(soundImpl)
        isPaused = false
    }

    /**
     * Immediately restarts the sound
     */
    fun rewind() {
        stop()
        play()
    }

    private fun makeIdentifier(source: String): Identifier {
        return Identifier(
            "ctjs",
            source.replace(".ogg", "").lowercase().filter { it in validIdentChars } + "_${counter++}",
        )
    }

    private class SoundImpl(
        soundEvent: SoundEvent,
        soundCategory: SoundCategory,
        attenuationType: MCAttenuationType,
    ) : MovingSoundInstance(soundEvent, soundCategory, Random.create()) {
        var categoryOverride: SoundCategory? = null

        init {
            this.attenuationType = attenuationType
        }

        override fun tick() {
            if (!World.isLoaded())
                setDone()
        }

        override fun getCategory(): SoundCategory {
            return categoryOverride ?: super.getCategory()
        }

        fun setPosition(x: Double, y: Double, z: Double) {
            this.x = x
            this.y = y
            this.z = z
        }

        fun setAttenuationType(attenuationType: MCAttenuationType) {
            this.attenuationType = attenuationType
        }
    }

    enum class Category(override val mcValue: SoundCategory) : CTWrapper<SoundCategory> {
        MASTER(SoundCategory.MASTER),
        MUSIC(SoundCategory.MUSIC),
        RECORDS(SoundCategory.RECORDS),
        WEATHER(SoundCategory.WEATHER),
        BLOCKS(SoundCategory.BLOCKS),
        HOSTILE(SoundCategory.HOSTILE),
        NEUTRAL(SoundCategory.NEUTRAL),
        PLAYERS(SoundCategory.PLAYERS),
        AMBIENT(SoundCategory.AMBIENT),
        VOICE(SoundCategory.VOICE);

        companion object {
            @JvmStatic
            fun fromMC(mcValue: SoundCategory) = values().first { it.mcValue == mcValue }

            @JvmStatic
            fun from(value: Any) = when (value) {
                is String -> valueOf(value)
                is SoundCategory -> fromMC(value)
                is Category -> value
                else -> throw IllegalArgumentException("Cannot create Sound.Category from $value")
            }
        }
    }

    enum class AttenuationType(override val mcValue: MCAttenuationType) : CTWrapper<MCAttenuationType> {
        NONE(MCAttenuationType.NONE),
        LINEAR(MCAttenuationType.LINEAR);

        companion object {
            @JvmStatic
            fun fromMC(mcValue: MCAttenuationType) = values().first { it.mcValue == mcValue }

            @JvmStatic
            fun from(value: Any) = when (value) {
                is String -> valueOf(value)
                is MCAttenuationType -> fromMC(value)
                is AttenuationType -> value
                else -> throw IllegalArgumentException("Cannot create Sound.Category from $value")
            }
        }
    }

    private object CTResourcePack : ResourcePack {
        override fun close() {
            throw NotImplementedError()
        }

        override fun openRoot(vararg segments: String): InputSupplier<InputStream>? {
            val file = segments.fold(CTJS.assetsDir, ::File)
            if (file.exists())
                return InputSupplier { file.inputStream() }
            return null
        }

        override fun open(type: ResourceType?, id: Identifier?): InputSupplier<InputStream>? {
            throw NotImplementedError()
        }

        override fun findResources(
            type: ResourceType?,
            namespace: String?,
            prefix: String?,
            consumer: ResourcePack.ResultConsumer?
        ) {
            throw NotImplementedError()
        }

        override fun getNamespaces(type: ResourceType?): MutableSet<String> {
            throw NotImplementedError()
        }

        override fun <T : Any?> parseMetadata(metaReader: ResourceMetadataReader<T>?): T? {
            throw NotImplementedError()
        }

        override fun getName(): String {
            return "CTJS_Sounds"
        }
    }

    companion object {
        private val soundSystem by lazy {
            Client.getMinecraft().soundManager.asMixin<SoundManagerAccessor>().soundSystem
        }

        private val validIdentChars = setOf(
            *('a'..'z').toList().toTypedArray(),
            *('0'..'9').toList().toTypedArray(),
            '_', '.', '-', '/',
        )
        private var counter = 0
    }
}
