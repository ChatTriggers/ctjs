package com.chattriggers.ctjs.minecraft.wrappers.inventory.nbt

import com.chattriggers.ctjs.mixins.NbtCompoundAccessor
import com.chattriggers.ctjs.utils.MCNbtBase
import com.chattriggers.ctjs.utils.MCNbtCompound
import com.chattriggers.ctjs.utils.MCNbtList
import com.chattriggers.ctjs.utils.asMixin
import net.minecraft.nbt.NbtByteArray
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtInt
import net.minecraft.nbt.NbtIntArray
import net.minecraft.nbt.NbtLongArray
import org.mozilla.javascript.NativeObject

class NBTTagCompound(override val rawNBT: MCNbtCompound) : NBTBase(rawNBT) {
    val tagMap: Map<String, MCNbtBase>
        get() = rawNBT.asMixin<NbtCompoundAccessor>().entries

    val keySet: Set<String>
        get() = rawNBT.keys

    enum class NBTDataType {
        BYTE,
        SHORT,
        INTEGER,
        LONG,
        FLOAT,
        DOUBLE,
        STRING,
        BYTE_ARRAY,
        INT_ARRAY,
        LONG_ARRAY,
        BOOLEAN,
        COMPOUND_TAG,
        TAG_LIST,
    }

    // TODO(breaking): Wrap MCNbtLists to NBTTagLists
    fun getTag(key: String): NBTBase? = rawNBT.get(key)?.let(::fromMC)

    fun getTagId(key: String) = rawNBT.getType(key)

    fun getByte(key: String) = rawNBT.getByte(key)

    fun getShort(key: String) = rawNBT.getShort(key)

    fun getInteger(key: String) = rawNBT.getInt(key)

    fun getLong(key: String) = rawNBT.getLong(key)

    fun getFloat(key: String) = rawNBT.getFloat(key)

    fun getDouble(key: String) = rawNBT.getDouble(key)

    fun getString(key: String) = rawNBT.getString(key)

    fun getByteArray(key: String) = rawNBT.getByteArray(key)

    fun getIntArray(key: String) = rawNBT.getIntArray(key)

    fun getBoolean(key: String) = rawNBT.getBoolean(key)

    fun getCompoundTag(key: String) = NBTTagCompound(rawNBT.getCompound(key))

    // TODO(breaking): Return wrapped NBTTagList
    fun getTagList(key: String, type: Int) = NBTTagList(rawNBT.getList(key, type))

    fun get(key: String, type: NBTDataType, tagType: Int?): Any? {
        return when (type) {
            NBTDataType.BYTE -> getByte(key)
            NBTDataType.SHORT -> getShort(key)
            NBTDataType.INTEGER -> getInteger(key)
            NBTDataType.LONG -> getLong(key)
            NBTDataType.FLOAT -> getFloat(key)
            NBTDataType.DOUBLE -> getDouble(key)
            NBTDataType.STRING -> {
                if (rawNBT.contains(key, NbtElement.STRING_TYPE.toInt()))
                    tagMap[key]?.let { NBTBase(it).toString() }
                else null
            }
            NBTDataType.BYTE_ARRAY -> {
                if (rawNBT.contains(key, NbtElement.BYTE_TYPE.toInt()))
                    (tagMap[key] as NbtByteArray).byteArray
                else null
            }
            NBTDataType.INT_ARRAY -> {
                if (rawNBT.contains(key, NbtElement.INT_ARRAY_TYPE.toInt()))
                    (tagMap[key] as NbtIntArray).intArray
                else null
            }
            NBTDataType.LONG_ARRAY -> {
                if (rawNBT.contains(key, NbtElement.LONG_ARRAY_TYPE.toInt()))
                    (tagMap[key] as NbtLongArray).longArray
                else null
            }
            NBTDataType.BOOLEAN -> getBoolean(key)
            NBTDataType.COMPOUND_TAG -> getCompoundTag(key)
            NBTDataType.TAG_LIST -> getTagList(
                key,
                tagType
                    ?: throw IllegalArgumentException("For accessing a tag list you need to provide the tagType argument")
            )
        }
    }

    operator fun get(key: String): NBTBase? = getTag(key)

    fun setNBTBase(key: String, value: NBTBase) = setNBTBase(key, value.rawNBT)

    fun setNBTBase(key: String, value: MCNbtBase) = apply {
        rawNBT.put(key, value)
    }

    fun setBoolean(key: String, value: Boolean) = apply {
        rawNBT.putBoolean(key, value)
    }

    fun setByte(key: String, value: Byte) = apply {
        rawNBT.putByte(key, value)
    }

    fun setShort(key: String, value: Short) = apply {
        rawNBT.putShort(key, value)
    }

    fun setInteger(key: String, value: Int) = apply {
        rawNBT.putInt(key, value)
    }

    fun setLong(key: String, value: Long) = apply {
        rawNBT.putLong(key, value)
    }

    fun setFloat(key: String, value: Float) = apply {
        rawNBT.putFloat(key, value)
    }

    fun setDouble(key: String, value: Double) = apply {
        rawNBT.putDouble(key, value)
    }

    fun setString(key: String, value: String) = apply {
        rawNBT.putString(key, value)
    }

    fun setByteArray(key: String, value: ByteArray) = apply {
        rawNBT.putByteArray(key, value)
    }

    fun setIntArray(key: String, value: IntArray) = apply {
        rawNBT.putIntArray(key, value)
    }

    fun putLongArray(key: String, value: LongArray) = apply {
        rawNBT.putLongArray(key, value)
    }

    operator fun set(key: String, value: Any) = apply {
        when (value) {
            is NBTBase -> setNBTBase(key, value)
            is MCNbtBase -> setNBTBase(key, value)
            is Byte -> setByte(key, value)
            is Short -> setShort(key, value)
            is Int -> setInteger(key, value)
            is Long -> setLong(key, value)
            is Float -> setFloat(key, value)
            is Double -> setDouble(key, value)
            is String -> setString(key, value)
            is ByteArray -> setByteArray(key, value)
            is IntArray -> setIntArray(key, value)
            is Boolean -> setBoolean(key, value)
            else -> throw IllegalArgumentException("Unsupported NBT type: ${value.javaClass.simpleName}")
        }
    }

    fun removeTag(key: String) = apply {
        rawNBT.remove(key)
    }

    fun toObject(): NativeObject = rawNBT.toObject()
}
