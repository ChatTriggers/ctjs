package com.chattriggers.ctjs.engine.module

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.objectweb.asm.Opcodes
import org.spongepowered.asm.mixin.injection.At.Shift

@Serializable
data class ModuleMetadata(
    val name: String? = null,
    val version: String? = null,
    var entry: String? = null,
    val asmEntry: String? = null,
    val asmExposedFunctions: Map<String, String>? = null,
    val tags: ArrayList<String>? = null,
    val pictureLink: String? = null,
    val creator: String? = null,
    val description: String? = null,
    val requires: ArrayList<String>? = null,
    val helpMessage: String? = null,
    val changelog: String? = null,
    val ignored: ArrayList<String>? = null,
    var isRequired: Boolean = false
) {
    var mixins: Map<String, ModuleMixin>? = null
}

@Serializable
class ModuleMixin(
    val className: String,
    val method: String,
    val at: At,
    val cancellable: Boolean? = null,
)

@Serializable
data class At(
    val value: String,
    val target: String? = null,
    val shift: Shift? = null,
    val by: Int? = null,
    val ordinal: Int? = null,
    val opcode: Opcode? = null,
)

@Serializable(with = OpcodeSerializer::class)
data class Opcode(val value: Int)

class OpcodeSerializer : KSerializer<Opcode> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Opcode", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Opcode {
        val name = decoder.decodeString()
        return Opcode(Opcodes::class.java.fields.first { it.name == name }.get(null) as Int)
    }

    override fun serialize(encoder: Encoder, value: Opcode) {
        val name = Opcodes::class.java.fields.first { it.get(null) == value.value }.name
        encoder.encodeString(name)
    }
}
