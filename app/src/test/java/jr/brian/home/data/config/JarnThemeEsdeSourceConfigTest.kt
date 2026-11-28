package jr.brian.home.data.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import jr.brian.frontend.look.JarnLookStore
import jr.brian.frontend.look.themes.EsdeImportRecord
import jr.brian.frontend.look.themes.EsdeRestoreOffer
import jr.brian.frontend.look.themes.EsdeThemeStamp
import jr.brian.frontend.look.themes.esdeRestoreOfferFile
import jr.brian.frontend.look.themes.readEsdeRestoreOffer
import jr.brian.frontend.look.themes.writeEsdeImportRecord
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class JarnThemeEsdeSourceConfigTest {
    private val managers = mockk<ManagerContainer>(relaxed = true)
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val themesRoot get() = File(context.filesDir, "themes")
    private val offerFile get() = esdeRestoreOfferFile(context.filesDir)
    private lateinit var store: JarnLookStore
    private lateinit var extras: SettingsBackupExtras
    private lateinit var importExport: ImportExportManager

    private val source = "/storage/1234-ABCD/ES-DE/themes/linear"

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { managers.ui.customAppNameManager.customNames } returns MutableStateFlow(emptyMap())
        every { managers.feature.esdePreferencesManager._state } returns MutableStateFlow(ESDEPrefsState())
        store = JarnLookStore(context)
        extras = testBackupExtras()
        importExport = ImportExportManager(
            managers = managers,
            screenAssignmentStore = mockk<ScreenAssignmentStore>(relaxed = true),
            experienceStore = ExperienceStore(context),
            artSourceStore = mockk(relaxed = true),
            libraryFolderBackup = mockk(relaxed = true),
            jarnLookStore = store,
            artChoiceStore = jr.brian.frontend.library.art.GameArtChoiceStore(context),
            appPinStore = jr.brian.frontend.apps.JarnAppPinStore(context),
            playStatsBackup = mockk(relaxed = true),
            backupExtras = extras,
            jarnPreviewStore = mockk(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        themesRoot.deleteRecursively()
        offerFile.delete()
    }

    private fun installImport(id: String) {
        val folder = File(themesRoot, id).apply { mkdirs() }
        writeEsdeImportRecord(
            folder,
            EsdeImportRecord(source, variant = "textlist", colorScheme = "dark", aspectRatio = "16:9", stamp = EsdeThemeStamp(1L)),
        )
    }

    private suspend fun importBackup(json: String) {
        val result = importExport.importFromStream(ByteArrayInputStream(json.toByteArray()))
        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
    }

    private fun backupWith(theme: String) = """{ "version": 79, "feature": { "jarnTheme": $theme } }"""

    @Test
    fun `schema version covers the ES-DE source`() {
        assertEquals(79, JarngreiprConfig.CONFIG_VERSION)
    }

    @Test
    fun `an imported ES-DE theme exports where it came from and round trips`() = runTest {
        installImport("linear")
        val exported = extras.addTo(JarngreiprConfig(feature = FeatureConfig(jarnTheme = JarnThemeConfig("linear"))))
        val esde = exported.feature.jarnTheme?.esde
        assertEquals(JarnThemeEsdeSourceConfig(source, "textlist", "dark", "16:9"), esde)

        val decoded = importExport.decodeFromJson(importExport.encodeToJson(exported))
        assertEquals(exported.feature.jarnTheme, decoded.feature.jarnTheme)
    }

    @Test
    fun `other themes export no ES-DE source`() = runTest {
        File(themesRoot, "art-book").mkdirs()
        listOf(JarnThemeConfig("art-book"), JarnThemeConfig(null), JarnThemeConfig("../escape")).forEach { theme ->
            val exported = extras.addTo(JarngreiprConfig(feature = FeatureConfig(jarnTheme = theme)))
            assertNull(exported.feature.jarnTheme?.esde)
        }
    }

    @Test
    fun `restoring an ES-DE theme this device lacks leaves an offer and keeps the id`() = runTest {
        importBackup(backupWith("""{ "id": "linear", "esde": { "sourceFolder": "$source", "variant": "textlist" } }"""))

        assertEquals("linear", store.activeThemeId)
        assertEquals(EsdeRestoreOffer("linear", source, variant = "textlist"), readEsdeRestoreOffer(offerFile))
    }

    @Test
    fun `restoring onto a device that has the theme makes no offer`() = runTest {
        installImport("linear")
        importBackup(backupWith("""{ "id": "linear", "esde": { "sourceFolder": "$source" } }"""))
        assertNull(readEsdeRestoreOffer(offerFile))
    }

    @Test
    fun `older and v78 backups change nothing about offers`() = runTest {
        val existing = EsdeRestoreOffer("old", source)
        jr.brian.frontend.look.themes.writeEsdeRestoreOffer(offerFile, existing)
        store.setActiveThemeId("kept")

        importBackup("""{ "version": 77, "feature": { "jarnLook": "ember" } }""")
        assertEquals("kept", store.activeThemeId)
        assertEquals(existing, readEsdeRestoreOffer(offerFile))

        importBackup("""{ "version": 78, "feature": { "jarnTheme": { "id": "art-book" } } }""")
        assertEquals("art-book", store.activeThemeId)
        assertEquals(existing, readEsdeRestoreOffer(offerFile))
        assertNull(importExport.decodeFromJson("""{ "version": 78, "feature": { "jarnTheme": { "id": "x" } } }""").feature.jarnTheme?.esde)
    }
}
