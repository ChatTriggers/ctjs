package com.chattriggers.ctjs.api

import com.chattriggers.ctjs.CTJS
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier

object CustomKeyMapping {
    private val SAVE_FILE = CTJS.configLocation.resolve("ctjs_key_mappings.txt")
    private val KEY_MAP: HashMap<String, String> = HashMap()
    private val customKeyMappings: MutableList<KeyMapping> = mutableListOf()
    private val customCategories: MutableList<KeyMapping.Category> = mutableListOf()

    @JvmStatic
    fun register(key: String, keyCode: Int, category: KeyMapping.Category): KeyMapping {
        val customKeyMapping = customKeyMappings.find { it.name == key }
        if (customKeyMapping != null) {
            return customKeyMapping.load()
        }

        if (!customCategories.contains(category)) {
            customCategories.add(category)
        }

        val keyMapping = KeyMapping(key, InputConstants.Type.KEYSYM, keyCode, category).load()
        customKeyMappings.add(keyMapping)
        return keyMapping
    }

    @JvmStatic
    fun register(key: String, keyCode: Int, category: String): KeyMapping {
        val cat = customCategories.find { it.id.path == category }.let {
            if (it == null) {
                val parts = category.split(":", limit = 2)
                val namespace = if (parts.size > 1) parts[0] else "ctjs"
                val path = if (parts.size > 1) parts[1] else parts[0]
                KeyMapping.Category(Identifier.fromNamespaceAndPath(namespace, path))
            } else it
        }

        return register(key, keyCode, cat)
    }

    @JvmStatic
    fun find(key: String): KeyMapping? {
        val vanilla = Minecraft.getInstance().options.keyMappings.find { it.name == key }
        if (vanilla != null) return vanilla

        val custom = customKeyMappings.find { it.name == key }
        if (custom != null) return custom

        return null
    }

    @JvmStatic
    fun getKeyMappings() = customKeyMappings.toList()

    @JvmStatic
    fun getCategories() = customCategories.toList()

    fun save() {
        val builder = StringBuilder()
        for (key in customKeyMappings) {
            builder.appendLine("${key.name}:${key.saveString()}")
        }
        SAVE_FILE.writeText(builder.toString())
    }

    fun load() {
        if (!SAVE_FILE.exists()) return

        SAVE_FILE.readText().lines().forEach { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size < 2) return@forEach
            KEY_MAP[parts[0]] = parts[1]
        }
    }

    private fun KeyMapping.load() = apply {
        KEY_MAP[name]?.let {
            setKey(InputConstants.getKey(it))
            KeyMapping.resetMapping()
        }
    }
}
