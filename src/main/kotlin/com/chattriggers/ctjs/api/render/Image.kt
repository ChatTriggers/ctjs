package com.chattriggers.ctjs.api.render

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.api.client.Client
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
    private val aspectRatio = if (textureHeight != 0) textureHeight.toFloat() / textureWidth else 0f

    init {
        CTJS.images.add(this)

        Client.scheduleTask {
            texture = image!!.toNativeTexture()
        }
    }

    fun getTextureWidth(): Int = textureWidth

    fun getTextureHeight(): Int = textureHeight

    fun getTexture(): NativeImageBackedTexture {
        requireNotNull(texture) {
            "Failed to bake Image texture"
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
        texture = null
        image = null
    }

    @JvmOverloads
    fun draw(
        x: Float,
        y: Float,
        width: Float? = null,
        height: Float? = null,
    ) = apply {
        val (drawWidth, drawHeight) = when {
            width == null && height == null -> textureWidth.toFloat() to textureHeight.toFloat()
            width == null -> height!! / aspectRatio to height
            height == null -> width to width * aspectRatio
            else -> width to height
        }

        if (texture != null)
            Renderer.drawImage(this, x, y, drawWidth, drawHeight)
    }

    private data class Texture(val texture: NativeImageBackedTexture, val buffer: ByteBuffer)

    companion object {
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
