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
class JarnFirstRunConfigImportTest {
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
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `schema version covers first run`() {
        assertEquals(51, JarngreiprConfig.CONFIG_VERSION)
    }

    @Test
    fun `first run flag round trips through the manager json`() {
        val exported = JarngreiprConfig(feature = FeatureConfig(jarnFirstRunDone = true))

        val decoded = importExport.decodeFromJson(importExport.encodeToJson(exported)).feature

        assertEquals(true, decoded.jarnFirstRunDone)
    }

    @Test
    fun `older backup decodes without the flag`() {
        assertNull(importExport.decodeFromJson(V48_BACKUP).feature.jarnFirstRunDone)
    }

    @Test
    fun `older backup leaves an unfinished tour unfinished`() = runTest {
        store.saveFirstRunStep("Look")

        importBackup(V48_BACKUP)

        assertFalse(store.firstRunDone)
        assertEquals("Look", store.firstRunStep)
    }

    @Test
    fun `older backup leaves a finished tour finished`() = runTest {
        store.markFirstRunDone()

        importBackup(V48_BACKUP)

        assertTrue(store.firstRunDone)
    }

    @Test
    fun `restored user is not sent through the tour again`() = runTest {
        store.saveFirstRunStep("Look")

        importBackup("""{ "version": 49, "feature": { "jarnFirstRunDone": true } }""")

        assertTrue(store.firstRunDone)
        assertNull(store.firstRunStep)
    }

    @Test
    fun `backup from an unfinished tour never reopens a finished one`() = runTest {
        store.markFirstRunDone()

        importBackup("""{ "version": 49, "feature": { "jarnFirstRunDone": false } }""")

        assertTrue(store.firstRunDone)
    }

    private suspend fun importBackup(json: String) {
        val result = importExport.importFromStream(ByteArrayInputStream(json.toByteArray()))
        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
    }

    private companion object {
        const val V48_BACKUP = """{ "version": 48, "feature": { "artSource": "EsDe" } }"""
    }
}
