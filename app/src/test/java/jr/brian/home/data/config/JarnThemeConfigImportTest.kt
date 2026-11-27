package jr.brian.home.data.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import jr.brian.frontend.look.JarnLookStore
import jr.brian.home.data.ExperienceStore
import jr.brian.home.data.ManagerContainer
import jr.brian.home.data.ScreenAssignmentStore
import jr.brian.home.esde.model.ESDEPrefsState
import jr.brian.jarnfe.backup.JarnBackupArea
import jr.brian.jarnfe.backup.JarnBackupChange
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class JarnThemeConfigImportTest {
    private val managers = mockk<ManagerContainer>(relaxed = true)
    private lateinit var store: JarnLookStore
    private lateinit var importExport: ImportExportManager

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { managers.ui.customAppNameManager.customNames } returns MutableStateFlow(emptyMap())
        every { managers.feature.esdePreferencesManager._state } returns MutableStateFlow(ESDEPrefsState())
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = JarnLookStore(context)
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
            backupExtras = testBackupExtras(),
            jarnPreviewStore = mockk(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `schema version covers the active theme`() {
        assertEquals(78, JarngreiprConfig.CONFIG_VERSION)
    }

    @Test
    fun `the active theme round trips through a backup`() = runTest {
        store.setActiveThemeId("art-book")
        val json = importExport.encodeToJson(exported())
        assertEquals("art-book", importExport.decodeFromJson(json).feature.jarnTheme?.id)

        store.setActiveThemeId(null)
        importBackup(json)

        assertEquals("art-book", store.activeThemeId)
    }

    @Test
    fun `a backup made on the built-in theme switches back to it`() = runTest {
        val json = importExport.encodeToJson(exported())
        store.setActiveThemeId("art-book")

        importBackup(json)

        assertNull(store.activeThemeId)
    }

    @Test
    fun `a theme missing on this device is kept so it returns once added`() = runTest {
        importBackup(backupWith("""{ "id": "not-installed" }"""))

        assertEquals("not-installed", store.activeThemeId)
        assertEquals("not-installed", JarnLookStore(ApplicationProvider.getApplicationContext()).activeThemeId)
    }

    @Test
    fun `an older backup leaves the theme and Command Panel match alone`() = runTest {
        store.setActiveThemeId("art-book")
        store.setMatchCommandPanel(true)
        store.setMatchCommandPanelFonts(true)

        importBackup(V77_BACKUP)

        assertEquals("art-book", store.activeThemeId)
        assertTrue(store.matchCommandPanel)
        assertTrue(store.matchCommandPanelFonts)
        assertNull(importExport.decodeFromJson(V77_BACKUP).feature.jarnTheme)
    }

    @Test
    fun `restoring a theme never touches Command Panel match`() = runTest {
        store.setMatchCommandPanel(true)
        store.setMatchCommandPanelFonts(false)

        importBackup(backupWith("""{ "id": "art-book" }"""))

        assertTrue(store.matchCommandPanel)
        assertFalse(store.matchCommandPanelFonts)
    }

    @Test
    fun `the preview lists a theme change under Look`() {
        val now = JarngreiprConfig(feature = FeatureConfig(jarnLook = "ember", jarnTheme = JarnThemeConfig()))

        assertEquals(JarnBackupChange.Replaces, look(FeatureConfig(jarnTheme = JarnThemeConfig("art-book")), now))
        assertEquals(JarnBackupChange.Same, look(FeatureConfig(jarnLook = "ember", jarnTheme = JarnThemeConfig()), now))
        assertEquals(JarnBackupChange.Same, look(FeatureConfig(jarnLook = "ember"), now))
        assertEquals(JarnBackupChange.NotIncluded, look(FeatureConfig(), now))
    }

    private fun look(backup: FeatureConfig, now: JarngreiprConfig): JarnBackupChange =
        buildBackupPreview(JarngreiprConfig(feature = backup), now)
            .items.first { it.area == JarnBackupArea.Look }.change

    private suspend fun importBackup(json: String) {
        val result = importExport.importFromStream(ByteArrayInputStream(json.toByteArray()))
        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
    }

    private fun exported() = JarngreiprConfig(feature = FeatureConfig(jarnTheme = store.themeBackup()))

    private fun backupWith(theme: String) = """{ "version": 78, "feature": { "jarnTheme": $theme } }"""

    private companion object {
        const val V77_BACKUP = """{ "version": 77, "feature": { "jarnLook": "ember" } }"""
    }
}
