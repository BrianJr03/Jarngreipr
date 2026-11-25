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
class JarnMatchFontsConfigImportTest {
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
            jarnPreviewStore = io.mockk.mockk(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `schema version covers matching fonts`() {
        assertEquals(63, JarngreiprConfig.CONFIG_VERSION)
    }

    @Test
    fun `matching fonts defaults to off`() {
        assertFalse(store.matchCommandPanelFonts)
    }

    @Test
    fun `fonts and colors toggle independently`() {
        store.setMatchCommandPanelFonts(true)
        assertFalse(store.matchCommandPanel)

        store.setMatchCommandPanel(true)
        store.setMatchCommandPanelFonts(false)
        assertTrue(store.matchCommandPanel)
        assertFalse(store.matchCommandPanelFonts)
    }

    @Test
    fun `matching fonts survives a store reload`() {
        store.setMatchCommandPanelFonts(true)

        val reloaded = JarnLookStore(ApplicationProvider.getApplicationContext())

        assertTrue(reloaded.matchCommandPanelFonts)
        assertFalse(reloaded.matchCommandPanel)
    }

    @Test
    fun `matching fonts round trips through a backup without touching colors`() = runTest {
        val exported = JarngreiprConfig(
            feature = FeatureConfig(jarnMatchCommandPanel = false, jarnMatchCommandPanelFonts = true),
        )
        val json = importExport.encodeToJson(exported)

        assertEquals(true, importExport.decodeFromJson(json).feature.jarnMatchCommandPanelFonts)
        importBackup(json)

        assertTrue(store.matchCommandPanelFonts)
        assertFalse(store.matchCommandPanel)
    }

    @Test
    fun `a backup can turn matching fonts off`() = runTest {
        store.setMatchCommandPanelFonts(true)

        importBackup("""{ "version": 62, "feature": { "jarnMatchCommandPanelFonts": false } }""")

        assertFalse(store.matchCommandPanelFonts)
    }

    @Test
    fun `an older backup leaves matching fonts alone in either state`() = runTest {
        listOf(true, false).forEach { current ->
            store.setMatchCommandPanelFonts(current)

            importBackup(V61_BACKUP)

            assertEquals(current, store.matchCommandPanelFonts)
        }
        assertNull(importExport.decodeFromJson(V61_BACKUP).feature.jarnMatchCommandPanelFonts)
    }

    private suspend fun importBackup(json: String) {
        val result = importExport.importFromStream(ByteArrayInputStream(json.toByteArray()))
        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
    }

    private companion object {
        const val V61_BACKUP = """{ "version": 61, "feature": { "jarnMatchCommandPanel": true } }"""
    }
}
