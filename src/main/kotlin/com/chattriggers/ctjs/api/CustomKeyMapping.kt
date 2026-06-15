package com.chattriggers.ctjs.api

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier

object CustomKeyMapping {
    private val customKeyMappings: MutableList<KeyMapping> = mutableListOf()
    private val customCategories: MutableList<KeyMapping.Category> = mutableListOf()

    @JvmStatic
    fun registerKeyMapping(key: String, keyCode: Int, category: KeyMapping.Category): KeyMapping {
        val customKeyMapping = customKeyMappings.find { it.name == key }
        if (customKeyMapping != null) {
            return customKeyMapping
        }

        if (!customCategories.contains(category)) {
            customCategories.add(category)
        }

        val keyMapping = KeyMapping(key, InputConstants.Type.KEYSYM, keyCode, category)
        customKeyMappings.add(keyMapping)
        return keyMapping
    }

    @JvmStatic
    fun registerKeyMapping(key: String, keyCode: Int, category: String): KeyMapping {
        val cat = customCategories.find { it.id.path == category }.let {
            if (it == null) {
                val parts = category.split(":", limit = 2)
                val namespace = if (parts.size > 1) parts[0] else "ctjs"
                val path = if (parts.size > 1) parts[1] else parts[0]
                KeyMapping.Category(Identifier.fromNamespaceAndPath(namespace, path))
            } else it
        }

        return registerKeyMapping(key, keyCode, cat)
    }

    @JvmStatic
    fun getKeyMapping(key: String): KeyMapping? {
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

    @JvmStatic
    fun clearMappings() {
        customKeyMappings.clear()
        customCategories.clear()
    }
}