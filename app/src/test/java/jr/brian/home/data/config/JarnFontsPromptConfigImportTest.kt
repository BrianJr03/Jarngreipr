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
class JarnFontsPromptConfigImportTest {
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
    fun `schema version covers the fonts prompt`() {
        assertEquals(74, JarngreiprConfig.CONFIG_VERSION)
    }

    @Test
    fun `the prompt starts unanswered and stays answered across a reload`() {
        assertFalse(store.fontsPromptShown)

        store.markFontsPromptShown()

        assertTrue(JarnLookStore(ApplicationProvider.getApplicationContext()).fontsPromptShown)
    }

    @Test
    fun `the prompt flag round trips through a backup`() = runTest {
        val json = importExport.encodeToJson(JarngreiprConfig(feature = FeatureConfig(jarnFontsPromptShown = true)))

        assertEquals(true, importExport.decodeFromJson(json).feature.jarnFontsPromptShown)
        importBackup(json)

        assertTrue(store.fontsPromptShown)
        assertFalse(store.matchCommandPanelFonts)
    }

    @Test
    fun `an older backup keeps the current value in either state`() = runTest {
        assertNull(importExport.decodeFromJson(V62_BACKUP).feature.jarnFontsPromptShown)
        importBackup(V62_BACKUP)
        assertFalse(store.fontsPromptShown)

        store.markFontsPromptShown()
        importBackup(V62_BACKUP)
        assertTrue(store.fontsPromptShown)
    }

    @Test
    fun `a backup from before the prompt never brings it back`() = runTest {
        store.markFontsPromptShown()

        importBackup("""{ "version": 63, "feature": { "jarnFontsPromptShown": false } }""")

        assertTrue(store.fontsPromptShown)
    }

    private suspend fun importBackup(json: String) {
        val result = importExport.importFromStream(ByteArrayInputStream(json.toByteArray()))
        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
    }

    private companion object {
        const val V62_BACKUP = """{ "version": 62, "feature": { "jarnMatchCommandPanelFonts": true } }"""
    }
}

