package com.chattriggers.ctjs.api.inventory.nbt

import com.chattriggers.ctjs.MCNbtBase
import com.chattriggers.ctjs.MCNbtCompound
import com.chattriggers.ctjs.MCNbtList
import com.chattriggers.ctjs.internal.utils.getOption
import net.minecraft.nbt.*
import org.mozilla.javascript.NativeArray
import org.mozilla.javascript.NativeObject

object NBT {
    /**
     * Creates a new [NBTBase] from the given [nbt]
     *
     * @param nbt the value to convert to NBT
     * @param options optional argument to allow refinement of the NBT data.
     * Possible options include:
     * - coerceNumericStrings: Boolean, default false.
     * E.g. "10b" as a byte, "20s" as a short, "30f" as a float, "40d" as a double,
     * "50l" as a long
     * - preferArraysOverLists: Boolean, default false
     * E.g. a list with all bytes or integers will be converted to an NBTTagByteArray or
     * NBTTagIntArray accordingly
     *
     * @return [NBTTagCompound] if [nbt] is an object, [NBTTagList] if [nbt]
     * is an array and preferArraysOverLists is false, or [NBTBase] otherwise.
     */
    @JvmStatic
    @JvmOverloads
    fun parse(nbt: Any, options: NativeObject? = null): NBTBase {
        return when (nbt) {
            is NativeObject -> NBTTagCompound(nbt.toNBT(options) as MCNbtCompound)
            is NativeArray -> {
                nbt.toNBT(options).let {
                    if (it is MCNbtList) {
                        NBTTagList(it)
                    } else {
                        NBTBase(it)
                    }
                }
            }
            else -> NBTBase(nbt.toNBT(options))
        }
    }

    @JvmStatic
    fun toObject(nbt: NBTTagCompound): NativeObject = nbt.toObject()

    @JvmStatic
    fun toArray(nbt: NBTTagList): NativeArray = nbt.toArray()

    private fun Any.toNBT(options: NativeObject?): MCNbtBase {
        val preferArraysOverLists = options.getOption("preferArraysOverLists", false).toBoolean()
        val coerceNumericStrings = options.getOption("coerceNumericStrings", false).toBoolean()

        return when (this) {
            is NativeObject -> MCNbtCompound().apply {
                entries.forEach { entry ->
                    put(entry.key.toString(), entry.value.toNBT(options))
                }
            }
            is NativeArray -> {
                val normalized = map { it?.toNBT(options) }

                if (!preferArraysOverLists || normalized.isEmpty()) {
                    return MCNbtList().apply { addAll(normalized) }
                }

                return when {
                    (normalized.all { it is NbtByte }) -> {
                        NbtByteArray(normalized.map { (it as NbtByte).byteValue() }.toByteArray())
                    }

                    (normalized.all { it is NbtInt }) -> {
                        NbtIntArray(normalized.map { (it as NbtInt).intValue() }.toIntArray())
                    }

                    (normalized.all { it is NbtLong }) -> {
                        NbtLongArray(normalized.map { (it as NbtLong).longValue() }.toLongArray())
                    }

                    else -> MCNbtList().apply { addAll(normalized) }
                }
            }
            is Boolean -> NbtByte.of(if (this) 1 else 0)
            is CharSequence -> parseString(this.toString(), coerceNumericStrings)
            is Byte -> NbtByte.of(this)
            is Short -> NbtShort.of(this)
            is Int -> NbtInt.of(this)
            is Long -> NbtLong.of(this)
            is Float -> NbtFloat.of(this)
            is Double -> NbtDouble.of(this)
            else -> throw IllegalArgumentException("Invalid NBT. Value provided: $this")
        }
    }

    private val numberNBTFormat = Regex("^([+-]?\\d+\\.?\\d*)([bslfd])?\$", RegexOption.IGNORE_CASE)

    private fun parseString(nbtData: String, coerceNumericStrings: Boolean): MCNbtBase {
        if (!coerceNumericStrings) {
            return NbtString.of(nbtData)
        }

        val res = numberNBTFormat.matchEntire(nbtData)?.groupValues ?: return NbtString.of(nbtData)

        val number = res[1]
        val suffix = res[2]

        return when (suffix.lowercase()) {
            "" -> {
                if (number.contains(".")) {
                    NbtDouble.of(number.toDouble())
                } else {
                    NbtInt.of(number.toInt())
                }
            }
            "b" -> NbtByte.of(number.toByte())
            "s" -> NbtShort.of(number.toShort())
            "l" -> NbtLong.of(number.toLong())
            "f" -> NbtFloat.of(number.toFloat())
            "d" -> NbtDouble.of(number.toDouble())
            else -> NbtString.of(nbtData)
        }
    }
}
