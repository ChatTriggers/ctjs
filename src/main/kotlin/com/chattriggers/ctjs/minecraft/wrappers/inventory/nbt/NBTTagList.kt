package com.chattriggers.ctjs.minecraft.wrappers.inventory.nbt

import com.chattriggers.ctjs.utils.MCNbtBase
import com.chattriggers.ctjs.utils.MCNbtList
import net.minecraft.nbt.NbtElement
import org.mozilla.javascript.NativeArray

class NBTTagList(override val rawNBT: MCNbtList) : NBTBase(rawNBT) {
    val tagCount: Int
        get() = rawNBT.size

    fun appendTag(nbt: NBTBase) = appendTag(nbt.rawNBT)

    fun appendTag(nbt: MCNbtBase) = apply {
        rawNBT.add(nbt)
    }

    operator fun set(id: Int, nbt: NBTBase) = set(id, nbt.rawNBT)

    operator fun set(id: Int, nbt: MCNbtBase) = apply {
        rawNBT[id] = nbt
    }

    fun insertTag(index: Int, nbt: NBTBase) = insertTag(index, nbt.rawNBT)

    fun insertTag(index: Int, nbt: MCNbtBase) = apply {
        rawNBT.add(index, nbt)
    }

    fun removeTag(index: Int) = rawNBT.removeAt(index)

    fun getShortAt(index: Int) = rawNBT.getShort(index)

    fun getIntAt(index: Int) = rawNBT.getInt(index)

    fun getFloatAt(index: Int) = rawNBT.getFloat(index)

    fun getDoubleAt(index: Int) = rawNBT.getDouble(index)

    fun getStringTagAt(index: Int): String = rawNBT.getString(index)

    fun getListAt(index: Int) = NBTTagList(rawNBT.getList(index))

    fun getCompoundTagAt(index: Int) = NBTTagCompound(rawNBT.getCompound(index))

    fun getIntArrayAt(index: Int): IntArray = rawNBT.getIntArray(index)

    fun getLongArrayAt(index: Int): LongArray = rawNBT.getLongArray(index)

    operator fun get(index: Int): NbtElement = rawNBT[index]

    fun get(index: Int, type: Byte): Any? = when (type) {
        NbtElement.SHORT_TYPE -> getShortAt(index)
        NbtElement.INT_TYPE -> getIntAt(index)
        NbtElement.FLOAT_TYPE -> getFloatAt(index)
        NbtElement.DOUBLE_TYPE -> getDoubleAt(index)
        NbtElement.STRING_TYPE -> getStringTagAt(index)
        NbtElement.LIST_TYPE -> getListAt(index)
        NbtElement.COMPOUND_TYPE -> getCompoundTagAt(index)
        NbtElement.INT_ARRAY_TYPE -> getIntArrayAt(index)
        NbtElement.LONG_ARRAY_TYPE -> getLongArrayAt(index)
        else -> get(index)
    }

    fun toArray(): NativeArray = rawNBT.toObject()
}
