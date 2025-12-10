package pt.ipbeja.moviesapi.entities.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@Serializable
data class ProblemDetails(
    val type: String? = null,
    val title: String,
    val status: Int,
    val detail: String? = null,
    val instance: String? = null,
    val extensions: JsonExtensions? = null
)


@Serializable(with = ExtensionsSerializer::class)
class JsonExtensions : HashMap<String, Any?> {
    constructor(entries: Map<out String?, Any?>?) : super(entries)
}

fun extensionsOf(vararg elements: Pair<String, Any?>) = JsonExtensions(mapOf(*elements))
fun Map<String, Any?>.toExtensions() = JsonExtensions(this)


object ExtensionsSerializer : KSerializer<JsonExtensions> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ProblemDetailsExtensions")

    override fun serialize(encoder: Encoder, value: JsonExtensions) {
        require(encoder is JsonEncoder)

        val jsonObject = buildJsonObject {
            value.forEach { (k, v) ->
                val jsonVal = when (v) {
                    null -> JsonNull
                    is JsonElement -> v
                    is Boolean -> JsonPrimitive(v)
                    is Number -> JsonPrimitive(v)
                    is String -> JsonPrimitive(v)
                    else -> throw SerializationException("Unsupported extension value type for key '$k'")
                }
                put(k, jsonVal)
            }
        }

        encoder.encodeJsonElement(jsonObject)

    }

    override fun deserialize(decoder: Decoder): JsonExtensions {
        TODO("Not yet implemented")
    }

}
