package com.chattriggers.ctjs.minecraft.wrappers.inventory.nbt

import com.chattriggers.ctjs.utils.MCNbtBase
import com.chattriggers.ctjs.utils.MCNbtCompound
import com.chattriggers.ctjs.utils.MCNbtList
import net.minecraft.nbt.*
import org.mozilla.javascript.NativeArray
import org.mozilla.javascript.NativeObject

open class NBTBase(open val rawNBT: MCNbtBase) {
    /**
     * Gets the type byte for the tag.
     */
    val id: Byte
        get() = rawNBT.type

    /**
     * Creates a clone of the tag.
     */
    fun copy() = rawNBT.copy()

    /**
     * Return whether this compound has no tags.
     */
    fun hasNoTags() = when (this) {
        is NBTTagCompound -> this.tagMap.isEmpty()
        is NBTTagList -> this.rawNBT.isEmpty()
        else -> false
    }

    fun hasTags() = !hasNoTags()

    override fun equals(other: Any?) = rawNBT == other

    override fun hashCode() = rawNBT.hashCode()

    override fun toString() = rawNBT.toString()

    companion object {
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
