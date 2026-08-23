package com.chattriggers.ctjs.api.inventory.nbt

import com.chattriggers.ctjs.MCNbtBase
import com.chattriggers.ctjs.MCNbtList
import net.minecraft.nbt.Tag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import org.mozilla.javascript.NativeArray

class NBTTagList(override val mcValue: MCNbtList) : NBTBase(mcValue) {
    val tagCount: Int
        get() = mcValue.size

    fun appendTag(nbt: NBTBase) = appendTag(nbt.toMC())

    fun appendTag(nbt: MCNbtBase) = apply {
        mcValue.add(nbt)
    }

    operator fun set(id: Int, nbt: NBTBase) = set(id, nbt.toMC())

    operator fun set(id: Int, nbt: MCNbtBase) = apply {
        mcValue[id] = nbt
    }

    fun insertTag(index: Int, nbt: NBTBase) = insertTag(index, nbt.toMC())

    fun insertTag(index: Int, nbt: MCNbtBase) = apply {
        mcValue.add(index, nbt)
    }

    fun removeTag(index: Int) = fromMC(mcValue.removeAt(index))

    fun getShortAt(index: Int) = mcValue.getShort(index)

    fun getIntAt(index: Int) = mcValue.getInt(index)

    fun getFloatAt(index: Int) = mcValue.getFloat(index)

    fun getDoubleAt(index: Int) = mcValue.getDouble(index)

    fun getStringTagAt(index: Int): String = mcValue.getString(index).orElse("")

    fun getListAt(index: Int) = NBTTagList(mcValue.getList(index).orElseGet(::ListTag))

    fun getCompoundTagAt(index: Int) = NBTTagCompound(mcValue.getCompound(index).orElseGet(::CompoundTag))

    fun getIntArrayAt(index: Int): IntArray = mcValue.getIntArray(index).orElseGet { IntArray(0) }

    fun getLongArrayAt(index: Int): LongArray = mcValue.getLongArray(index).orElseGet { LongArray(0) }

    operator fun get(index: Int): Tag = mcValue[index]

    fun get(index: Int, type: Byte): Any = when (type) {
        Tag.TAG_SHORT -> getShortAt(index)
        Tag.TAG_INT -> getIntAt(index)
        Tag.TAG_FLOAT -> getFloatAt(index)
        Tag.TAG_DOUBLE -> getDoubleAt(index)
        Tag.TAG_STRING -> getStringTagAt(index)
        Tag.TAG_LIST -> getListAt(index)
        Tag.TAG_COMPOUND -> getCompoundTagAt(index)
        Tag.TAG_INT_ARRAY -> getIntArrayAt(index)
        Tag.TAG_LONG_ARRAY -> getLongArrayAt(index)
        else -> get(index)
    }

    fun toArray(): NativeArray = mcValue.toObject()
}
