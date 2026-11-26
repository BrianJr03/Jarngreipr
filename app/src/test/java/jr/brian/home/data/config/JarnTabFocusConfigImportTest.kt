package jr.brian.home.data.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import jr.brian.frontend.look.JarnLookStore
import jr.brian.frontend.look.JarnTabFocus
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

/** Drives the real export and import path against a real [JarnLookStore]. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class JarnTabFocusConfigImportTest {
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
    fun `an exported choice comes back from the backup and survives a restart`() = runTest {
        store.updateDisplay { it.copy(tabFocus = JarnTabFocus.Dpad) }
        val exported = importExport.encodeToJson(
            JarngreiprConfig(feature = FeatureConfig(jarnDisplay = store.display.toConfig())),
        )
        assertEquals("dpad", importExport.decodeFromJson(exported).feature.jarnDisplay?.tabFocus)
        store.updateDisplay { it.copy(tabFocus = JarnTabFocus.Both) }

        importBackup(exported)

        assertEquals(JarnTabFocus.Dpad, store.display.tabFocus)
        assertEquals(JarnTabFocus.Dpad, JarnLookStore(ApplicationProvider.getApplicationContext()).display.tabFocus)
    }

    @Test
    fun `an older backup on a fresh install gives both`() = runTest {
        importBackup(V72_BACKUP)

        assertEquals(JarnTabFocus.Both, store.display.tabFocus)
    }

    @Test
    fun `an older backup keeps the current choice`() = runTest {
        store.updateDisplay { it.copy(tabFocus = JarnTabFocus.Neither) }

        importBackup(V72_BACKUP)

        assertEquals(JarnTabFocus.Neither, store.display.tabFocus)
    }

    private suspend fun importBackup(json: String) {
        val result = importExport.importFromStream(ByteArrayInputStream(json.toByteArray()))
        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
    }

    private companion object {
        const val V72_BACKUP =
            """{ "version": 72, "feature": { "jarnDisplay": { "videoFit": "crop" } } }"""
    }
}
