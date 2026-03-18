package jr.brian.home.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@Serializable
data class JingleEntry(
    val game: String,
    val file: String,
    val regex: String? = null
)

@Serializable(with = JingleIndexSerializer::class)
data class JingleIndex(
    val name: String = "",
    val entries: List<JingleEntry> = emptyList()
)

object JingleIndexSerializer : KSerializer<JingleIndex> {
    private val entryListSerializer = ListSerializer(JingleEntry.serializer())

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("JingleIndex")

    override fun deserialize(decoder: Decoder): JingleIndex {
        val jsonDecoder = decoder as JsonDecoder
        val obj = jsonDecoder.decodeJsonElement().jsonObject

        val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: ""

        val entries = if ("entries" in obj) {
            // Flat format: { "entries": [ { "game": "...", "file": "..." } ] }
            jsonDecoder.json.decodeFromJsonElement(entryListSerializer, obj["entries"]!!)
        } else {
            // Grouped format: { "n3ds": [...], "wii": [...] }
            obj.entries
                .filter { (key, value) -> key != "name" && value is JsonArray }
                .flatMap { (_, value) ->
                    jsonDecoder.json.decodeFromJsonElement(entryListSerializer, value)
                }
        }

        return JingleIndex(name = name, entries = entries)
    }

    override fun serialize(encoder: Encoder, value: JingleIndex) {
        val jsonEncoder = encoder as JsonEncoder
        jsonEncoder.encodeJsonElement(buildJsonObject {
            put("name", value.name)
            putJsonArray("entries") {
                value.entries.forEach { entry ->
                    addJsonObject {
                        put("game", entry.game)
                        put("file", entry.file)
                        entry.regex?.let { put("regex", it) }
                    }
                }
            }
        })
    }
}
