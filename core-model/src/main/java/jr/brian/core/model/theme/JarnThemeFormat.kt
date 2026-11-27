package jr.brian.core.model.theme

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

sealed interface JarnThemeDecodeResult {
    data class Decoded(val spec: JarnThemeSpec) : JarnThemeDecodeResult

    /** Written by a newer Jarn. The player has to update before this theme can load. */
    data class TooNew(val schema: Int, val supported: Int) : JarnThemeDecodeResult

    /** Not a Jarn theme, or a damaged one. [reason] is for logs, not for the player. */
    data class Invalid(val reason: String) : JarnThemeDecodeResult
}

/** Reads and writes theme files. Unknown keys and layer types are skipped; a newer schema is refused. */
object JarnThemeFormat {

    private const val SCHEMA_KEY = "schema"
    private const val FIRST_SCHEMA = 1

    val json = Json { ignoreUnknownKeys = true }

    fun encode(spec: JarnThemeSpec): String = json.encodeToString(JarnThemeSpec.serializer(), spec)

    fun decode(text: String): JarnThemeDecodeResult {
        val root = try {
            json.parseToJsonElement(text) as? JsonObject
        } catch (e: SerializationException) {
            return JarnThemeDecodeResult.Invalid(e.message ?: "Not JSON")
        } ?: return JarnThemeDecodeResult.Invalid("Not a JSON object")

        val schema = (root[SCHEMA_KEY] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
            ?: return JarnThemeDecodeResult.Invalid("Missing or non-numeric schema")
        if (schema > JarnThemeSpec.CURRENT_SCHEMA) {
            return JarnThemeDecodeResult.TooNew(schema, JarnThemeSpec.CURRENT_SCHEMA)
        }
        if (schema < FIRST_SCHEMA) return JarnThemeDecodeResult.Invalid("Unknown schema $schema")

        return try {
            JarnThemeDecodeResult.Decoded(json.decodeFromJsonElement(JarnThemeSpec.serializer(), root))
        } catch (e: IllegalArgumentException) {
            // SerializationException is an IllegalArgumentException, as are require() failures in models.
            JarnThemeDecodeResult.Invalid(e.message ?: "Malformed theme")
        }
    }
}
