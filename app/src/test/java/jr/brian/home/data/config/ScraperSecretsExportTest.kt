package jr.brian.home.data.config

import android.app.Application
import android.content.Context
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.setScraperConcurrencyOverride
import jr.brian.home.esde.data.setScreenScraperDevId
import jr.brian.home.esde.data.setScreenScraperDevPassword
import jr.brian.home.esde.data.setScreenScraperUserId
import jr.brian.home.esde.data.setScreenScraperUserPassword
import jr.brian.home.esde.data.setSteamGridDbApiKey
import jr.brian.home.esde.model.DEFAULT_SCREENSCRAPER_MEDIA_TYPES
import jr.brian.home.esde.model.DEFAULT_STEAMGRIDDB_MEDIA_TYPES
import jr.brian.home.esde.model.ESDEPrefsState
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Phase 0 §5: configs shared between users must not leak scraper
 * credentials. These tests pin:
 *  1. the export side strips every secret to the empty string;
 *  2. the import side treats an empty-string field as "do not clobber my
 *     stored secret", so round-tripping a config through buildConfig +
 *     applyConfig leaves prior credentials intact.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ScraperSecretsExportTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        // Each test starts from a clean prefs blob so credentials from a
        // prior test can't bleed through.
        app.getSharedPreferences("esde_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `buildScraperConfigForExport strips every credential`() {
        val prefsWithSecrets = ESDEPrefsState(
            screenScraperEnabled = true,
            screenScraperDevId = "my-dev-id",
            screenScraperDevPassword = "my-dev-password",
            screenScraperUserId = "my-user-id",
            screenScraperUserPassword = "my-user-password",
            steamGridDbEnabled = true,
            steamGridDbApiKey = "my-steamgriddb-key",
            scraperConcurrencyOverride = 4,
        )

        val exported = buildScraperConfigForExport(prefsWithSecrets)

        assertEquals("", exported.screenScraperDevId)
        assertEquals("", exported.screenScraperDevPassword)
        assertEquals("", exported.screenScraperUserId)
        assertEquals("", exported.screenScraperUserPassword)
        assertEquals("", exported.steamGridDbApiKey)
        // Non-secret fields must still ride through — this is NOT a wipe.
        assertEquals(true, exported.screenScraperEnabled)
        assertEquals(true, exported.steamGridDbEnabled)
        assertEquals(4, exported.concurrencyOverride)
    }

    @Test
    fun `applyScraperConfigImport with empty creds preserves stored secrets`() {
        val prefs = ESDEPreferencesManager(app).apply {
            setScreenScraperDevId("existing-dev-id")
            setScreenScraperDevPassword("existing-dev-password")
            setScreenScraperUserId("existing-user-id")
            setScreenScraperUserPassword("existing-user-password")
            setSteamGridDbApiKey("existing-steamgriddb-key")
        }
        val importedWithoutSecrets = ScraperConfig(
            screenScraperEnabled = true,
            screenScraperDevId = "",
            screenScraperDevPassword = "",
            screenScraperUserId = "",
            screenScraperUserPassword = "",
            screenScraperMediaTypes = DEFAULT_SCREENSCRAPER_MEDIA_TYPES,
            steamGridDbEnabled = true,
            steamGridDbApiKey = "",
            steamGridDbMediaTypes = DEFAULT_STEAMGRIDDB_MEDIA_TYPES,
            concurrencyOverride = 0,
        )

        applyScraperConfigImport(importedWithoutSecrets, prefs)

        val state = prefs.state.value
        assertEquals("existing-dev-id", state.screenScraperDevId)
        assertEquals("existing-dev-password", state.screenScraperDevPassword)
        assertEquals("existing-user-id", state.screenScraperUserId)
        assertEquals("existing-user-password", state.screenScraperUserPassword)
        assertEquals("existing-steamgriddb-key", state.steamGridDbApiKey)
    }

    @Test
    fun `applyScraperConfigImport with filled creds overwrites, for pre-split configs`() {
        val prefs = ESDEPreferencesManager(app).apply {
            setScreenScraperDevId("old-dev-id")
            setScreenScraperUserId("preserved-user-id")
            setSteamGridDbApiKey("old-steamgriddb-key")
        }
        val legacyImport = ScraperConfig(
            screenScraperEnabled = true,
            screenScraperDevId = "legacy-config-dev-id",
            screenScraperDevPassword = "legacy-config-dev-password",
            screenScraperUserId = "",
            screenScraperUserPassword = "",
            screenScraperMediaTypes = DEFAULT_SCREENSCRAPER_MEDIA_TYPES,
            steamGridDbEnabled = true,
            steamGridDbApiKey = "legacy-config-steamgriddb-key",
            steamGridDbMediaTypes = DEFAULT_STEAMGRIDDB_MEDIA_TYPES,
            concurrencyOverride = 0,
        )

        applyScraperConfigImport(legacyImport, prefs)

        val state = prefs.state.value
        assertEquals("legacy-config-dev-id", state.screenScraperDevId)
        assertEquals("legacy-config-dev-password", state.screenScraperDevPassword)
        // Legacy config carried an empty user id, so the stored value must
        // stay rather than get wiped to "".
        assertEquals("preserved-user-id", state.screenScraperUserId)
        assertEquals("legacy-config-steamgriddb-key", state.steamGridDbApiKey)
    }
}
