package jr.brian.home.data.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import jr.brian.frontend.look.JarnLookStore
import jr.brian.home.data.ExperienceStore
import jr.brian.home.data.ManagerContainer
import jr.brian.home.data.ScreenAssignmentStore
import jr.brian.home.data.preview.JarnPreviewStore
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
class JarnPreviewConfigImportTest {
    private val managers = mockk<ManagerContainer>(relaxed = true)
    private lateinit var store: JarnPreviewStore
    private lateinit var importExport: ImportExportManager

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { managers.ui.customAppNameManager.customNames } returns MutableStateFlow(emptyMap())
        every { managers.feature.esdePreferencesManager._state } returns MutableStateFlow(ESDEPrefsState())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val experienceStore = ExperienceStore(context)
        val lookStore = JarnLookStore(context)
        store = JarnPreviewStore(context, experienceStore, lookStore)
        importExport = ImportExportManager(
            managers = managers,
            screenAssignmentStore = mockk<ScreenAssignmentStore>(relaxed = true),
            experienceStore = experienceStore,
            artSourceStore = mockk(relaxed = true),
            libraryFolderBackup = mockk(relaxed = true),
            jarnLookStore = lookStore,
            artChoiceStore = jr.brian.frontend.library.art.GameArtChoiceStore(context),
            appPinStore = jr.brian.frontend.apps.JarnAppPinStore(context),
            playStatsBackup = mockk(relaxed = true),
            backupExtras = testBackupExtras(),
            jarnPreviewStore = store,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `schema version covers the preview`() {
        assertEquals(74, JarngreiprConfig.CONFIG_VERSION)
    }

    @Test
    fun `preview state round trips through the manager json`() {
        val preview = JarnPreviewConfig(showings = 2, lastShownVersion = "3.2.0", tried = true)
        val exported = JarngreiprConfig(feature = FeatureConfig(jarnPreview = preview))

        val decoded = importExport.decodeFromJson(importExport.encodeToJson(exported)).feature

        assertEquals(preview, decoded.jarnPreview)
    }

    @Test
    fun `export carries the current state`() {
        store.recordShown("3.2.0")
        store.markTried()

        val exported = store.previewBackup()

        assertEquals(JarnPreviewConfig(1, "3.2.0", true), exported)
    }

    @Test
    fun `older backup decodes without the preview`() {
        assertNull(importExport.decodeFromJson(V56_BACKUP).feature.jarnPreview)
    }

    @Test
    fun `older backup changes nothing`() = runTest {
        store.recordShown("3.2.0")

        importBackup(V56_BACKUP)

        assertEquals(1, store.current.showings)
        assertEquals("3.2.0", store.current.lastShownVersion)
        assertFalse(store.current.tried)
    }

    @Test
    fun `restore retires the preview for someone who tried Jarn`() = runTest {
        importBackup(backup(showings = 1, version = "3.1.0", tried = true))

        assertTrue(store.current.tried)
        assertEquals(1, store.current.showings)
    }

    @Test
    fun `restore never lowers the count or turns tried off`() = runTest {
        listOf("3.0.0", "3.1.0").forEach(store::recordShown)
        store.markTried()

        importBackup(backup(showings = 0, version = "2.9.0", tried = false))

        assertEquals(2, store.current.showings)
        assertEquals("3.1.0", store.current.lastShownVersion)
        assertTrue(store.current.tried)
    }

    @Test
    fun `a backup from a Jarn user retires it through the experience too`() = runTest {
        importBackup("""{ "version": 56, "feature": { "experience": "JarnFE" } }""")

        assertTrue(store.current.tried)
    }

    private suspend fun importBackup(json: String) {
        val result = importExport.importFromStream(ByteArrayInputStream(json.toByteArray()))
        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
    }

    private fun backup(showings: Int, version: String, tried: Boolean) =
        """{ "version": 57, "feature": { "jarnPreview": """ +
            """{ "showings": $showings, "lastShownVersion": "$version", "tried": $tried } } }"""

    private companion object {
        const val V56_BACKUP = """{ "version": 56, "feature": { "artSource": "EsDe" } }"""
    }
}
