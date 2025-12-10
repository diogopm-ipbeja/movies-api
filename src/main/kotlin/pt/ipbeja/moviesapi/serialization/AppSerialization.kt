package pt.ipbeja.moviesapi.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.math.BigDecimal
import java.math.RoundingMode

private val serializers = SerializersModule {
    contextual(BigDecimal::class, BigDecimalToStringSerializer)
}

val appJson = Json {
    isLenient = true
    ignoreUnknownKeys = true
    allowTrailingComma = true
    classDiscriminator = $$"$type"
    serializersModule = serializers
}





object BigDecimalToStringSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimalAsString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeString())
    }
}

object BigDecimalToFloatSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimalAsFloat", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        val multiplied = value.multiply(2.toBigDecimal())
        val rounded = multiplied.setScale(0, RoundingMode.HALF_UP)
        val result = rounded.divide(BigDecimal(2))
        encoder.encodeFloat(result.toFloat())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeDouble())
    }
}