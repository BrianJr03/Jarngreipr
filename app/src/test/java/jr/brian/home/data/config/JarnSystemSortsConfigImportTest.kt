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
class JarnSystemSortsConfigImportTest {
    private val managers = mockk<ManagerContainer>(relaxed = true)
    private lateinit var sorts: jr.brian.frontend.library.art.GameSortStore
    private lateinit var importExport: ImportExportManager

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { managers.ui.customAppNameManager.customNames } returns MutableStateFlow(emptyMap())
        every { managers.feature.esdePreferencesManager._state } returns MutableStateFlow(ESDEPrefsState())
        val context = ApplicationProvider.getApplicationContext<Context>()
        sorts = jr.brian.frontend.library.art.GameSortStore(context)
        importExport = ImportExportManager(
            managers = managers,
            screenAssignmentStore = mockk<ScreenAssignmentStore>(relaxed = true),
            experienceStore = ExperienceStore(context),
            artSourceStore = mockk(relaxed = true),
            libraryFolderBackup = mockk(relaxed = true),
            jarnLookStore = JarnLookStore(context),
            artChoiceStore = jr.brian.frontend.library.art.GameArtChoiceStore(context),
            appPinStore = jr.brian.frontend.apps.JarnAppPinStore(context),
            playStatsBackup = mockk(relaxed = true),
            backupExtras = testBackupExtras(gameSorts = sorts),
            jarnPreviewStore = io.mockk.mockk(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `schema version covers game sorts`() {
        assertEquals(71, JarngreiprConfig.CONFIG_VERSION)
    }

    @Test
    fun `export carries each system's sort`() = runTest {
        sorts.set("snes", "LastPlayed")

        val exported = testBackupExtras(gameSorts = sorts).addTo(JarngreiprConfig())

        assertEquals(mapOf("snes" to "LastPlayed"), exported.feature.jarnSystemSorts)
    }

    @Test
    fun `a backup's sorts replace the current ones`() = runTest {
        sorts.set("snes", "LastPlayed")

        importBackup("""{ "version": 67, "feature": { "jarnSystemSorts": { "gba": "ReleaseYear" } } }""")

        assertEquals(mapOf("gba" to "ReleaseYear"), sorts.state.value)
    }

    @Test
    fun `an older backup keeps the current sorts`() = runTest {
        sorts.set("snes", "MostPlayed")

        importBackup("""{ "version": 66, "feature": { "jarnFontsPromptShown": true } }""")

        assertEquals(mapOf("snes" to "MostPlayed"), sorts.state.value)
    }

    private suspend fun importBackup(json: String) {
        val result = importExport.importFromStream(ByteArrayInputStream(json.toByteArray()))
        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
    }
}
