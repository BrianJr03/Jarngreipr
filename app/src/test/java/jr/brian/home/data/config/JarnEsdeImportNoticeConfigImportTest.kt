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
class JarnEsdeImportNoticeConfigImportTest {
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
    fun `schema version covers the ES-DE notice`() {
        assertEquals(77, JarngreiprConfig.CONFIG_VERSION)
    }

    @Test
    fun `the notice starts unshown and stays shown across a reload`() {
        assertFalse(store.esdeImportNoticeShown)

        store.markEsdeImportNoticeShown()

        assertTrue(JarnLookStore(ApplicationProvider.getApplicationContext()).esdeImportNoticeShown)
    }

    @Test
    fun `the notice flag round trips through a backup`() = runTest {
        val json = importExport.encodeToJson(JarngreiprConfig(feature = FeatureConfig(jarnEsdeImportNoticeShown = true)))

        assertEquals(true, importExport.decodeFromJson(json).feature.jarnEsdeImportNoticeShown)
        importBackup(json)

        assertTrue(store.esdeImportNoticeShown)
    }

    @Test
    fun `an older backup keeps the current value in either state`() = runTest {
        assertNull(importExport.decodeFromJson(V64_BACKUP).feature.jarnEsdeImportNoticeShown)
        importBackup(V64_BACKUP)
        assertFalse(store.esdeImportNoticeShown)

        store.markEsdeImportNoticeShown()
        importBackup(V64_BACKUP)
        assertTrue(store.esdeImportNoticeShown)
    }

    @Test
    fun `a backup never brings the notice back`() = runTest {
        store.markEsdeImportNoticeShown()

        importBackup("""{ "version": 65, "feature": { "jarnEsdeImportNoticeShown": false } }""")

        assertTrue(store.esdeImportNoticeShown)
    }

    private suspend fun importBackup(json: String) {
        val result = importExport.importFromStream(ByteArrayInputStream(json.toByteArray()))
        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
    }

    private companion object {
        const val V64_BACKUP = """{ "version": 64, "feature": { "jarnFontsPromptShown": true } }"""
    }
}

