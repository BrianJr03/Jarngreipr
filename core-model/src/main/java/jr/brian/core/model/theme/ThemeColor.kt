package jr.brian.core.model.theme

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private const val OPAQUE_ALPHA = 0xFF
private const val RGB_DIGITS = 6
private const val ARGB_DIGITS = 8
private const val HEX_RADIX = 16

/** A packed ARGB color, written as `#RRGGBB` when opaque and `#AARRGGBB` otherwise. */
@Serializable(with = ThemeColorSerializer::class)
@JvmInline
value class ThemeColor(val argb: Int) {
    val alpha: Int get() = argb ushr 24

    override fun toString(): String = if (alpha == OPAQUE_ALPHA) {
        "#" + (argb and 0xFFFFFF).toString(HEX_RADIX).padStart(RGB_DIGITS, '0').uppercase()
    } else {
        "#" + argb.toUInt().toString(HEX_RADIX).padStart(ARGB_DIGITS, '0').uppercase()
    }

    companion object {
        /** Reads `#RRGGBB` or `#AARRGGBB`; null for anything else. */
        fun parse(text: String): ThemeColor? {
            val digits = text.removePrefix("#")
            if (!text.startsWith("#") || (digits.length != RGB_DIGITS && digits.length != ARGB_DIGITS)) return null
            if (!digits.all { it.digitToIntOrNull(HEX_RADIX) != null }) return null
            val value = digits.toUIntOrNull(HEX_RADIX)?.toInt() ?: return null
            return ThemeColor(if (digits.length == RGB_DIGITS) value or (OPAQUE_ALPHA shl 24) else value)
        }
    }
}

object ThemeColorSerializer : KSerializer<ThemeColor> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("jr.brian.core.model.theme.ThemeColor", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ThemeColor) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): ThemeColor {
        val text = decoder.decodeString()
        return ThemeColor.parse(text) ?: throw SerializationException("Not a #RRGGBB or #AARRGGBB color: $text")
    }
}
