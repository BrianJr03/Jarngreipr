package jr.brian.home.data.config

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The fixture was written by the single-file config at v68. Splitting the config classes into
 * one file per area must leave the JSON byte for byte the same, both ways.
 */
class ConfigFixtureTest {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val fixture: String =
        requireNotNull(javaClass.classLoader?.getResource("config/config-v68.json")).readText()

    @Test
    fun `a v68 backup decodes and re-encodes to the same bytes`() {
        val decoded = json.decodeFromString<JarngreiprConfig>(fixture)
        assertEquals(fixture, json.encodeToString(decoded))
    }

    @Test
    fun `the same config still encodes to the v68 fixture`() {
        assertEquals(fixture, json.encodeToString(configFixture()))
    }
}
