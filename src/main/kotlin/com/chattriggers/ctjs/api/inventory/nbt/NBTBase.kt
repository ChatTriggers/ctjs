package com.chattriggers.ctjs.api.inventory.nbt

import com.chattriggers.ctjs.api.CTWrapper
import com.chattriggers.ctjs.MCNbtBase
import com.chattriggers.ctjs.MCNbtCompound
import com.chattriggers.ctjs.MCNbtList
import net.minecraft.nbt.*
import org.mozilla.javascript.NativeArray
import org.mozilla.javascript.NativeObject

open class NBTBase(override val mcValue: MCNbtBase) : CTWrapper<MCNbtBase> {
    /**
     * Gets the type byte for the tag.
     */
    val id: Byte
        get() = mcValue.type

    /**
     * Creates a clone of the tag.
     */
    fun copy() = mcValue.copy()

    /**
     * Return whether this compound has no tags.
     */
    fun hasNoTags() = when (this) {
        is NBTTagCompound -> tagMap.isEmpty()
        is NBTTagList -> mcValue.isEmpty()
        else -> false
    }

    fun hasTags() = !hasNoTags()

    override fun equals(other: Any?) = mcValue == other

    override fun hashCode() = mcValue.hashCode()

    override fun toString() = mcValue.toString()

    companion object {
        @JvmStatic
        fun fromMC(nbt: MCNbtBase): NBTBase = when (nbt) {
            is MCNbtCompound -> NBTTagCompound(nbt)
            is MCNbtList -> NBTTagList(nbt)
            else -> NBTBase(nbt)
        }

        fun MCNbtBase.toObject(): Any? {
            return when (this) {
                is NbtString -> asString()
                is NbtByte -> byteValue()
                is NbtShort -> shortValue()
                is NbtInt -> intValue()
                is NbtLong -> longValue()
                is NbtFloat -> floatValue()
                is NbtDouble -> doubleValue()
                is MCNbtCompound -> toObject()
                is MCNbtList -> toObject()
                is NbtByteArray -> NativeArray(byteArray.toTypedArray()).expose()
                is NbtIntArray -> NativeArray(intArray.toTypedArray()).expose()
                else -> error("Unknown tag type $javaClass")
            }
        }

        fun MCNbtCompound.toObject(): NativeObject {
            val o = NativeObject()
            o.expose()

            for (key in keys) {
                val value = this[key]
                if (value != null) {
                    o.put(key, o, value.toObject())
                }
            }

            return o
        }

        fun MCNbtList.toObject(): NativeArray {
            val tags = mutableListOf<Any?>()
            for (i in 0 until count()) {
                tags.add(get(i).toObject())
            }
            val array = NativeArray(tags.toTypedArray())
            array.expose()
            return array
        }

        private fun NativeArray.expose() = apply {
            // Taken from the private NativeArray#init method
            exportAsJSClass(32, this, false)
        }

        private fun NativeObject.expose() = apply {
            // Taken from the private NativeObject#init method
            exportAsJSClass(12, this, false)
        }
    }
}
