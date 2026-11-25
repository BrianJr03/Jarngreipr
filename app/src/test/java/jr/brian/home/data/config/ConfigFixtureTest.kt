package jr.brian.home.data.config

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The fixture was written by the single-file config at v68. Splitting the config classes into
 * one file per area left the JSON byte for byte the same. Later versions only add fields.
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
    fun `a v68 backup decodes to the same config, with nothing for fields added later`() {
        val decoded = json.decodeFromString<JarngreiprConfig>(fixture)
        assertEquals(configFixture().copy(version = 68), decoded)
    }

    @Test
    fun `a v68 backup re-encodes to the same bytes apart from the version and the v69 field`() {
        val reencoded = json.encodeToString(json.decodeFromString<JarngreiprConfig>(fixture))
        val expected = fixture.replace(
            "\"playingDim\": \"30s\"",
            "\"playingDim\": \"30s\",\n            \"playingScreenOff\": null",
        )
        assertEquals(expected, reencoded)
    }
}
