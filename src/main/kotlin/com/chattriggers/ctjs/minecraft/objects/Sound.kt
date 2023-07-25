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
import com.chattriggers.ctjs.utils.MCAttenuationType
import com.chattriggers.ctjs.utils.MCSound
import com.chattriggers.ctjs.utils.asMixin
import gg.essential.universal.UMinecraft
import net.minecraft.client.sound.MovingSoundInstance
import net.minecraft.client.sound.Sound.RegistrationType
import net.minecraft.client.sound.WeightedSoundSet
import net.minecraft.resource.*
import net.minecraft.resource.metadata.ResourceMetadata
import net.minecraft.resource.metadata.ResourceMetadataReader
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.random.Random
import org.mozilla.javascript.NativeObject
import java.io.File
import java.io.InputStream
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension

/**
 * Instances a new Sound with certain properties. These properties
 * should be passed through as a normal JavaScript object.
 *
 * REQUIRED:
 * - source (String) - a namespaced-identifier (e.g. `minecraft:music_disc.cat`) for a Minecraft sound, or a filename
 *                     relative to ChatTriggers assets directory
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
    private var isCustom = false

    private var isPaused = false

    private val source = config["source"]?.toString() ?: throw IllegalArgumentException("Sound source is null.")

    // Before bootstrap, we need to store the values ourselves. Afterward, however, we should
    // derive the values from the actual sound object. This switches implementations at the
    // end of bootstrap()
    private var soundData: SoundData = InitialSoundData(config)

    private fun bootstrap() {
        if (::sound.isInitialized)
            return

        CTJS.sounds.add(this)

        val soundManagerAccessor = UMinecraft.getMinecraft().soundManager.asMixin<SoundManagerAccessor>()
        val soundFile = File(CTJS.assetsDir, source)
        if (soundFile.exists()) {
            isCustom = true
            identifier = makeIdentifier(source)
            val resource = Resource(CTResourcePack, soundFile::inputStream, ResourceMetadata::NONE)
            soundManagerAccessor.soundResources[identifier.withPrefixedPath("sounds/").withSuffixedPath(".ogg")] = resource
        } else {
            identifier = Identifier(source)
        }

        soundImpl = SoundImpl(SoundEvent.of(identifier), soundData.category.toMC(), soundData.attenuationType.toMC())
        sound = MCSound(
            identifier.toString(),
            { soundData.volume },
            { soundData.pitch },
            1,
            RegistrationType.FILE,
            soundData.stream,
            false,
            soundData.attenuation,
        )

        if (isCustom) {
            soundManagerAccessor.sounds[identifier] = WeightedSoundSet(identifier, null).apply {
                add(sound)
            }
        }

        val initialData = soundData
        soundData = BootstrappedSoundData(sound, soundImpl, initialData.volume, initialData.pitch)

        // Apply all initial values as the user may have changed them
        soundData.loop = initialData.loop
        soundData.loopDelay = initialData.loopDelay
        soundData.x = initialData.x
        soundData.y = initialData.y
        soundData.z = initialData.z
        soundData.attenuation = initialData.attenuation
        soundData.category = initialData.category
        soundData.attenuationType = initialData.attenuationType
    }

    fun destroy() {
        stop()
        if (isCustom) {
            val soundManagerAccessor = UMinecraft.getMinecraft().soundManager.asMixin<SoundManagerAccessor>()
            soundManagerAccessor.sounds.remove(identifier)
            soundManagerAccessor.soundResources.remove(identifier)
        }
    }

    /**
     * Gets the category of this sound, making it respect the Player's sound volume sliders.
     *
     * @return the category
     */
    fun getCategory() = soundData.category

    /**
     * Sets the category of this sound, making it respect the Player's sound volume sliders.
     *
     * @param category the category
     */
    fun setCategory(category: Category) = apply {
        soundData.category = category
    }

    /**
     * Gets this sound's volume.
     *
     * @return A float value (0.0f - 1.0f).
     */
    fun getVolume() = soundData.volume

    /**
     * Sets this sound's volume.
     *
     * @param volume A float value (0.0f - 1.0f).
     */
    fun setVolume(volume: Float) = apply {
        soundData.volume = volume
    }

    fun getX() = soundData.x

    fun getY() = soundData.y

    fun getZ() = soundData.z

    fun setX(x: Double) = apply {
        soundData.x = x
    }

    fun setY(y: Double) = apply {
        soundData.y = y
    }

    fun setZ(z: Double) = apply {
        soundData.z = z
    }

    fun getPosition() = Vec3d(getX(), getY(), getZ())

    fun setPosition(x: Double, y: Double, z: Double) = apply {
        soundData.x = x
        soundData.y = y
        soundData.z = z
    }

    /**
     * Gets this sound's pitch.
     *
     * @return A float value (0.5f - 2.0f).
     */
    fun getPitch() = soundData.pitch

    /**
     * Sets this sound's pitch.
     *
     * @param pitch A float value (0.5f - 2.0f).
     */
    fun setPitch(pitch: Float) = apply {
        soundData.pitch = pitch
    }

    /**
     * Gets the attenuation type (fade out over space) of the sound
     *
     * @return The type of Attenuation
     */
    fun getAttenuationType() = soundData.attenuationType

    /**
     * Sets the attenuation type (fade out over space) of the sound
     *
     * @param attenuationType The type of Attenuation
     */
    fun setAttenuationType(attenuationType: AttenuationType) = apply {
        soundData.attenuationType = attenuationType
    }
    /**
     * Gets the attenuation distance of the sound
     */
    fun getAttenuation() = soundData.attenuation

    /**
     * Sets the attenuation distance of the sound
     */
    fun setAttenuation(attenuation: Int) = apply {
        soundData.attenuation = attenuation
    }

    /**
     * Gets whether the sound should repeat after finishing
     */
    fun getLoop() = soundData.loop

    /**
     * Sets whether the sound should repeat after finishing
     */
    fun setLoop(loop: Boolean) = apply {
        soundData.loop = loop
    }

    /**
     * Gets the tick delay after finishing before looping again (if getLoop() is true)
     */
    fun getLoopDelay() = soundData.loopDelay

    /**
     * Sets the tick delay after finishing before looping again (if getLoop() is true)
     */
    fun setLoopDelay(loopDelay: Int) = apply {
        soundData.loopDelay = loopDelay
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
                    it.resume()
                }
            }
        }
    }

    /**
     * Pauses the sound, to be resumed later
     */
    fun pause() {
        bootstrap()

        Client.scheduleTask {
            isPaused = true
            soundSystem.asMixin<SoundSystemAccessor>().sources[soundImpl]?.run {
                it.pause()
            }
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
            Path(source).nameWithoutExtension.lowercase().filter { it in validIdentChars } + "_${counter++}",
        )
    }

    private interface SoundData {
        var loop: Boolean
        var loopDelay: Int
        var stream: Boolean
        var volume: Float
        var pitch: Float
        var x: Double
        var y: Double
        var z: Double
        var attenuation: Int
        var category: Category
        var attenuationType: AttenuationType
    }

    private class InitialSoundData(config: NativeObject) : SoundData {
        override var loop = config.getOrDefault("loop", false) as Boolean
        override var loopDelay = (config.getOrDefault("loopDelay", 0) as Number).toInt()
        override var stream = config.getOrDefault("stream", false) as Boolean
        override var volume = (config.getOrDefault("volume", 1f) as Number).toFloat()
        override var pitch = (config.getOrDefault("pitch", 1f) as Number).toFloat()
        override var x = (config.getOrDefault("x", Player.getX()) as Number).toDouble()
        override var y = (config.getOrDefault("y", Player.getY()) as Number).toDouble()
        override var z = (config.getOrDefault("z", Player.getZ()) as Number).toDouble()
        override var attenuation = (config.getOrDefault("attenuation", 16) as Number).toInt()
        override var category = config["category"]?.let(Sound.Category::from) ?: Sound.Category.MASTER
        override var attenuationType = config["attenuationType"]?.let(Sound.AttenuationType::from) ?: Sound.AttenuationType.LINEAR
    }

    private class BootstrappedSoundData(
        private val sound: MCSound,
        private val impl: SoundImpl,
        override var volume: Float,
        override var pitch: Float,
    ) : SoundData {
        private val mixedSound: SoundAccessor = sound.asMixin()
        private val mixedImpl: AbstractSoundInstanceAccessor = impl.asMixin()

        override var loop: Boolean
            get() = impl.isRepeatable
            set(value) { mixedImpl.setRepeat(value) }

        override var loopDelay: Int
            get() = impl.repeatDelay
            set(value) { mixedImpl.setRepeatDelay(value) }

        override var stream: Boolean
            get() = error("stream should not be accessed after bootstrap")
            set(_) = error("stream should not be accessed after bootstrap")

        override var x: Double
            get() = impl.x
            set(value) { impl.setPosition(value, y, z) }

        override var y: Double
            get() = impl.y
            set(value) { impl.setPosition(x, value, z) }

        override var z: Double
            get() = impl.z
            set(value) { impl.setPosition(x, y, value) }

        override var attenuation: Int
            get() = sound.attenuation
            set(value) { mixedSound.setAttenuation(value) }

        override var category: Category
            get() = Category.fromMC(impl.categoryOverride)
            set(value) { impl.categoryOverride = value.toMC() }

        override var attenuationType: AttenuationType
            get() = AttenuationType.fromMC(impl.attenuationType)
            set(value) { impl.attenuationType = value.toMC() }
    }

    private class SoundImpl(
        soundEvent: SoundEvent,
        soundCategory: SoundCategory,
        attenuationType: MCAttenuationType,
    ) : MovingSoundInstance(soundEvent, soundCategory, Random.create()) {
        var categoryOverride: SoundCategory = super.category

        init {
            this.attenuationType = attenuationType
        }

        override fun tick() {
            if (!World.isLoaded())
                setDone()
        }

        override fun getCategory(): SoundCategory {
            return categoryOverride
        }

        fun setPosition(x: Double, y: Double, z: Double) {
            this.x = x
            this.y = y
            this.z = z
        }

        fun setAttenuationType(attenuationType: MCAttenuationType) {
            this.attenuationType = attenuationType
        }

        fun setVolume(volume: Float) {
            this.volume = volume.coerceIn(0f, 1f)
        }

        fun setPitch(pitch: Float) {
            this.pitch = pitch.coerceIn(0.5f, 2f)
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

    private companion object {
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
