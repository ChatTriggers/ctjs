package com.chattriggers.ctjs.api.inventory.nbt

import com.chattriggers.ctjs.internal.utils.MCNbtBase
import com.chattriggers.ctjs.internal.utils.MCNbtList
import net.minecraft.nbt.NbtElement
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

    fun getStringTagAt(index: Int): String = mcValue.getString(index)

    fun getListAt(index: Int) = NBTTagList(mcValue.getList(index))

    fun getCompoundTagAt(index: Int) = NBTTagCompound(mcValue.getCompound(index))

    fun getIntArrayAt(index: Int): IntArray = mcValue.getIntArray(index)

    fun getLongArrayAt(index: Int): LongArray = mcValue.getLongArray(index)

    operator fun get(index: Int): NbtElement = mcValue[index]

    fun get(index: Int, type: Byte): Any = when (type) {
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

    fun toArray(): NativeArray = mcValue.toObject()
}
