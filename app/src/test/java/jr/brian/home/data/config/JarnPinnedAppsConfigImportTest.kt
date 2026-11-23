package jr.brian.home.data.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import jr.brian.frontend.apps.JarnAppPinStore
import jr.brian.frontend.library.art.ArtSourceStore
import jr.brian.frontend.library.art.GameArtChoiceStore
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

/** Drives the real import path against a real [JarnAppPinStore]. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class JarnPinnedAppsConfigImportTest {
    private val managers = mockk<ManagerContainer>(relaxed = true)
    private lateinit var context: Context
    private lateinit var store: JarnAppPinStore
    private lateinit var importExport: ImportExportManager

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { managers.ui.customAppNameManager.customNames } returns MutableStateFlow(emptyMap())
        every { managers.feature.esdePreferencesManager._state } returns MutableStateFlow(ESDEPrefsState())
        context = ApplicationProvider.getApplicationContext()
        store = JarnAppPinStore(context)
        importExport = ImportExportManager(
            managers = managers,
            screenAssignmentStore = mockk<ScreenAssignmentStore>(relaxed = true),
            experienceStore = ExperienceStore(context),
            artSourceStore = ArtSourceStore(context),
            libraryFolderBackup = mockk(relaxed = true),
            jarnLookStore = JarnLookStore(context),
            artChoiceStore = GameArtChoiceStore(context),
            appPinStore = store,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun import(json: String) {
        val result = importExport.importFromStream(ByteArrayInputStream(json.toByteArray()))
        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
    }

    @Test
    fun `pins round trip through the manager json in order`() {
        val exported = JarngreiprConfig(feature = FeatureConfig(jarnPinnedApps = listOf("b", "a")))
        val decoded = importExport.decodeFromJson(importExport.encodeToJson(exported))
        assertEquals(listOf("b", "a"), decoded.feature.jarnPinnedApps)
    }

    @Test
    fun `a backup without pins decodes to null`() {
        assertNull(importExport.decodeFromJson(V42_BACKUP).feature.jarnPinnedApps)
    }

    @Test
    fun `importing an older backup keeps current pins`() = runTest {
        store.setPinned("keep.me", true)
        import(V42_BACKUP)
        assertEquals(listOf("keep.me"), store.pinned)
    }

    @Test
    fun `importing a backup applies its pins in order and survives a restart`() = runTest {
        store.setPinned("old", true)
        import("""{ "version": 43, "feature": { "jarnPinnedApps": ["z.app", "a.app", "z.app"] } }""")
        assertEquals(listOf("z.app", "a.app"), store.pinned)
        assertEquals(listOf("z.app", "a.app"), JarnAppPinStore(context).pinned)
    }

    @Test
    fun `an empty pin list in a backup clears pins`() = runTest {
        store.setPinned("old", true)
        import("""{ "version": 43, "feature": { "jarnPinnedApps": [] } }""")
        assertTrue(store.pinned.isEmpty())
    }

    private companion object {
        const val V42_BACKUP = """{ "version": 42, "feature": { "artSource": "EsDe" } }"""
    }
}
