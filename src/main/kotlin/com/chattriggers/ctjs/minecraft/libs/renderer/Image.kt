package com.chattriggers.ctjs.minecraft.libs.renderer

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.minecraft.CTEvents
import com.chattriggers.ctjs.utils.Initializer
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import org.lwjgl.system.MemoryUtil
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.nio.ByteBuffer
import javax.imageio.ImageIO

class Image(var image: BufferedImage?) {
    private var texture: Texture? = null
    private val textureWidth = image?.width ?: 0
    private val textureHeight = image?.height ?: 0
    private val aspectRatio = if (textureHeight != 0) textureHeight.toDouble() / textureWidth else 0.0

    init {
        CTJS.images.add(this)
    }

    // TODO(breaking): Remove deprecated files

    fun getTextureWidth(): Int = textureWidth

    fun getTextureHeight(): Int = textureHeight

    fun getTexture(): NativeImageBackedTexture {
        if (texture == null) {
            // We're trying to access the texture before initialization. Presumably, the game overlay render event
            // hasn't fired yet, so we haven't loaded the texture. Let's hope this is a rendering context!
            try {
                texture = image!!.toNativeTexture()
                image = null
            } catch (e: Exception) {
                // Unlucky. This probably wasn't a rendering context.
                println("Trying to bake texture in a non-rendering context.")

                throw e
            }
        }

        return texture!!.texture
    }

    /**
     * Clears the image from GPU memory and removes its references CT side
     * that way it can be garbage collected if not referenced in js code.
     */
    fun destroy() {
        texture?.texture?.close()
        texture?.buffer?.let(MemoryUtil::memFree)
    }

    @JvmOverloads
    fun draw(
        x: Double,
        y: Double,
        width: Double? = null,
        height: Double? = null,
    ) = apply {
        val (drawWidth, drawHeight) = when {
            width == null && height == null -> textureWidth.toDouble() to textureHeight.toDouble()
            width == null -> height!! / aspectRatio to height
            height == null -> width to width * aspectRatio
            else -> width to height
        }

        if (texture != null)
            Renderer.drawImage(this, x, y, drawWidth, drawHeight)
    }

    private data class Texture(val texture: NativeImageBackedTexture, val buffer: ByteBuffer)

    companion object : Initializer {
        override fun init() {
            CTEvents.RENDER_OVERLAY.register { _, _ ->
                for (image in CTJS.images) {
                    if (image.image != null) {
                        image.texture = image.image!!.toNativeTexture()
                        image.image = null
                    }
                }
            }
        }

        /**
         * Create an image object from a java.io.File object. Throws an exception
         * if the file cannot be found.
         */
        @JvmStatic
        fun fromFile(file: File) = Image(ImageIO.read(file))

        /**
         * Create an image object from a file path. Throws an exception
         * if the file cannot be found.
         */
        @JvmStatic
        fun fromFile(file: String) = Image(ImageIO.read(File(file)))

        /**
         * Create an image object from a file path, relative to the assets directory.
         * Throws an exception if the file cannot be found.
         */
        @JvmStatic
        fun fromAsset(name: String) = Image(ImageIO.read(File(CTJS.assetsDir, name)))

        /**
         * Creates an image object from a URL. Throws an exception if an image
         * cannot be created from the URL. Will cache the image in the assets
         */
        @JvmStatic
        @JvmOverloads
        fun fromUrl(url: String, cachedImageName: String? = null): Image {
            if (cachedImageName == null)
                return Image(getImageFromUrl(url))

            val resourceFile = File(CTJS.assetsDir, cachedImageName)

            if (resourceFile.exists())
                return Image(ImageIO.read(resourceFile))

            val image = getImageFromUrl(url)
            ImageIO.write(image, "png", resourceFile)
            return Image(image)
        }

        private fun getImageFromUrl(url: String): BufferedImage {
            val req = CTJS.makeWebRequest(url)
            if (req is HttpURLConnection) {
                req.requestMethod = "GET"
                req.doOutput = true
            }

            return ImageIO.read(req.inputStream)
        }

        private fun BufferedImage.toNativeTexture(): Texture {
            return ByteArrayOutputStream().use {
                ImageIO.write(this, "png", it)
                val buffer = MemoryUtil.memAlloc(it.size())
                buffer.put(it.toByteArray())
                buffer.rewind()
                Texture(NativeImageBackedTexture(NativeImage.read(buffer)), buffer)
            }
        }
    }
}
