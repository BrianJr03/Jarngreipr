package jr.brian.home.data.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import jr.brian.core.model.ArtSource
import jr.brian.frontend.library.art.ArtSourceStore
import jr.brian.home.data.ExperienceStore
import jr.brian.home.data.ManagerContainer
import jr.brian.home.data.ScreenAssignmentStore
import jr.brian.home.esde.model.ESDEPrefsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

/** Drives the real import path against a real [ArtSourceStore]. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ArtSourceConfigImportTest {
    private val managers = mockk<ManagerContainer>(relaxed = true)
    private lateinit var store: ArtSourceStore
    private lateinit var importExport: ImportExportManager

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { managers.ui.customAppNameManager.customNames } returns MutableStateFlow(emptyMap())
        every { managers.feature.esdePreferencesManager._state } returns MutableStateFlow(ESDEPrefsState())
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = ArtSourceStore(context)
        importExport = ImportExportManager(
            managers = managers,
            screenAssignmentStore = mockk<ScreenAssignmentStore>(relaxed = true),
            experienceStore = ExperienceStore(context),
            artSourceStore = store,
            libraryFolderBackup = mockk(relaxed = true),
            jarnLookStore = mockk(relaxed = true),
            artChoiceStore = jr.brian.frontend.library.art.GameArtChoiceStore(ApplicationProvider.getApplicationContext()),
            appPinStore = jr.brian.frontend.apps.JarnAppPinStore(ApplicationProvider.getApplicationContext()),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `schema version includes the art source field`() {
        assertEquals(47, JarngreiprConfig.CONFIG_VERSION)
    }

    @Test
    fun `every art source round trips through the manager json`() {
        ArtSource.entries.forEach { source ->
            val encoded = importExport.encodeToJson(
                JarngreiprConfig(feature = FeatureConfig(artSource = source.name)),
            )
            assertEquals(source.name, importExport.decodeFromJson(encoded).feature.artSource)
        }
    }

    @Test
    fun `older backup decodes to null`() {
        assertNull(importExport.decodeFromJson(V38_BACKUP).feature.artSource)
    }

    @Test
    fun `importing an older backup keeps the current source`() = runTest {
        ArtSource.entries.forEach { current ->
            assertImport(current, V38_BACKUP, current)
        }
    }

    @Test
    fun `importing a backup applies its source`() = runTest {
        assertImport(ArtSource.EsDe, backupWith("RetroHrai"), ArtSource.RetroHrai)
        assertImport(ArtSource.RetroHrai, backupWith("Scraped"), ArtSource.Scraped)
    }

    @Test
    fun `an older backup does not count as a choice`() = runTest {
        importExport.importFromStream(ByteArrayInputStream(V38_BACKUP.toByteArray()))

        assertNull(store.savedSource)
        assertTrue(store.saveDetected(ArtSource.RetroHrai))
    }

    @Test
    fun `a backup with a source counts as a choice and clears detection`() = runTest {
        store.saveDetected(ArtSource.RetroHrai)

        importExport.importFromStream(ByteArrayInputStream(backupWith("EsDe").toByteArray()))

        assertEquals(ArtSource.EsDe, store.savedSource)
        assertNull(store.detectedState.value)
        assertFalse(store.saveDetected(ArtSource.RetroHrai))
    }

    @Test
    fun `a config with an art source carries no scraper credentials`() {
        val scraper = buildScraperConfigForExport(
            ESDEPrefsState(
                screenScraperDevId = SECRET,
                screenScraperDevPassword = SECRET,
                screenScraperUserId = SECRET,
                screenScraperUserPassword = SECRET,
                steamGridDbApiKey = SECRET,
            ),
        )
        val exported = importExport.encodeToJson(
            JarngreiprConfig(
                feature = FeatureConfig(
                    artSource = ArtSource.Scraped.name,
                    romSearch = RomSearchConfig(scraper = scraper),
                ),
            ),
        )

        assertEquals(ArtSource.Scraped.name, importExport.decodeFromJson(exported).feature.artSource)
        assertFalse("export leaked a credential", exported.contains(SECRET))
    }

    private suspend fun assertImport(current: ArtSource, backup: String, expected: ArtSource) {
        store.setSource(current)
        val result = importExport.importFromStream(ByteArrayInputStream(backup.toByteArray()))
        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
        assertEquals(expected, store.source)
    }

    private fun backupWith(source: String) =
        """{ "version": 39, "feature": { "artSource": "$source" } }"""

    private companion object {
        const val V38_BACKUP =
            """{ "version": 38, "feature": { "experience": "Classic" } }"""
        const val SECRET = "do-not-export"
    }
}
