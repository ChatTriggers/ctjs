package com.chattriggers.ctjs.minecraft.wrappers.inventory.nbt

import com.chattriggers.ctjs.mixins.NbtCompoundAccessor
import com.chattriggers.ctjs.utils.MCNbtBase
import com.chattriggers.ctjs.utils.MCNbtCompound
import com.chattriggers.ctjs.utils.asMixin
import net.minecraft.nbt.NbtByteArray
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtIntArray
import net.minecraft.nbt.NbtLongArray
import org.mozilla.javascript.NativeObject

class NBTTagCompound(override val mcValue: MCNbtCompound) : NBTBase(mcValue) {
    val tagMap: Map<String, MCNbtBase>
        get() = mcValue.asMixin<NbtCompoundAccessor>().entries

    val keySet: Set<String>
        get() = mcValue.keys

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

    fun getTag(key: String): NBTBase? = mcValue.get(key)?.let(::fromMC)

    fun getTagId(key: String) = mcValue.getType(key)

    fun getByte(key: String) = mcValue.getByte(key)

    fun getShort(key: String) = mcValue.getShort(key)

    fun getInteger(key: String) = mcValue.getInt(key)

    fun getLong(key: String) = mcValue.getLong(key)

    fun getFloat(key: String) = mcValue.getFloat(key)

    fun getDouble(key: String) = mcValue.getDouble(key)

    fun getString(key: String) = mcValue.getString(key)

    fun getByteArray(key: String) = mcValue.getByteArray(key)

    fun getIntArray(key: String) = mcValue.getIntArray(key)

    fun getBoolean(key: String) = mcValue.getBoolean(key)

    fun getCompoundTag(key: String) = NBTTagCompound(mcValue.getCompound(key))

    fun getTagList(key: String, type: Int) = NBTTagList(mcValue.getList(key, type))

    fun get(key: String, type: NBTDataType, tagType: Int?): Any? {
        return when (type) {
            NBTDataType.BYTE -> getByte(key)
            NBTDataType.SHORT -> getShort(key)
            NBTDataType.INTEGER -> getInteger(key)
            NBTDataType.LONG -> getLong(key)
            NBTDataType.FLOAT -> getFloat(key)
            NBTDataType.DOUBLE -> getDouble(key)
            NBTDataType.STRING -> {
                if (mcValue.contains(key, NbtElement.STRING_TYPE.toInt()))
                    tagMap[key]?.let { NBTBase(it).toString() }
                else null
            }
            NBTDataType.BYTE_ARRAY -> {
                if (mcValue.contains(key, NbtElement.BYTE_TYPE.toInt()))
                    (tagMap[key] as NbtByteArray).byteArray
                else null
            }
            NBTDataType.INT_ARRAY -> {
                if (mcValue.contains(key, NbtElement.INT_ARRAY_TYPE.toInt()))
                    (tagMap[key] as NbtIntArray).intArray
                else null
            }
            NBTDataType.LONG_ARRAY -> {
                if (mcValue.contains(key, NbtElement.LONG_ARRAY_TYPE.toInt()))
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

    fun setNBTBase(key: String, value: NBTBase) = setNBTBase(key, value.toMC())

    fun setNBTBase(key: String, value: MCNbtBase) = apply {
        mcValue.put(key, value)
    }

    fun setBoolean(key: String, value: Boolean) = apply {
        mcValue.putBoolean(key, value)
    }

    fun setByte(key: String, value: Byte) = apply {
        mcValue.putByte(key, value)
    }

    fun setShort(key: String, value: Short) = apply {
        mcValue.putShort(key, value)
    }

    fun setInteger(key: String, value: Int) = apply {
        mcValue.putInt(key, value)
    }

    fun setLong(key: String, value: Long) = apply {
        mcValue.putLong(key, value)
    }

    fun setFloat(key: String, value: Float) = apply {
        mcValue.putFloat(key, value)
    }

    fun setDouble(key: String, value: Double) = apply {
        mcValue.putDouble(key, value)
    }

    fun setString(key: String, value: String) = apply {
        mcValue.putString(key, value)
    }

    fun setByteArray(key: String, value: ByteArray) = apply {
        mcValue.putByteArray(key, value)
    }

    fun setIntArray(key: String, value: IntArray) = apply {
        mcValue.putIntArray(key, value)
    }

    fun putLongArray(key: String, value: LongArray) = apply {
        mcValue.putLongArray(key, value)
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
        mcValue.remove(key)
    }

    fun toObject(): NativeObject = mcValue.toObject()
}
