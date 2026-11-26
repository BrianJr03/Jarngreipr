package jr.brian.home.data.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import jr.brian.frontend.apps.JarnAppPinStore
import jr.brian.frontend.library.art.ArtSourceStore
import jr.brian.frontend.library.art.GameArtChoiceStore
import jr.brian.frontend.look.JARN_LIVE_WALLPAPER_OFF
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

/** Drives the real import path against a real [JarnLookStore]. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class JarnLiveWallpaperConfigImportTest {
    private val managers = mockk<ManagerContainer>(relaxed = true)
    private lateinit var context: Context
    private lateinit var store: JarnLookStore
    private lateinit var importExport: ImportExportManager

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { managers.ui.customAppNameManager.customNames } returns MutableStateFlow(emptyMap())
        every { managers.feature.esdePreferencesManager._state } returns MutableStateFlow(ESDEPrefsState())
        context = ApplicationProvider.getApplicationContext()
        store = JarnLookStore(context)
        importExport = ImportExportManager(
            managers = managers,
            screenAssignmentStore = mockk<ScreenAssignmentStore>(relaxed = true),
            experienceStore = ExperienceStore(context),
            artSourceStore = ArtSourceStore(context),
            libraryFolderBackup = mockk(relaxed = true),
            jarnLookStore = store,
            artChoiceStore = GameArtChoiceStore(context),
            appPinStore = JarnAppPinStore(context),
            playStatsBackup = io.mockk.mockk(relaxed = true),
            backupExtras = testBackupExtras(),
            jarnPreviewStore = io.mockk.mockk(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun restore(json: String) {
        val result = importExport.importFromStream(ByteArrayInputStream(json.toByteArray()))
        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
    }

    private fun backupWith(id: String) = """{ "version": 45, "feature": { "jarnLiveWallpaper": "$id" } }"""

    @Test
    fun `the field round trips through the manager json`() {
        val exported = JarngreiprConfig(feature = FeatureConfig(jarnLiveWallpaper = "starstave"))

        val decoded = importExport.decodeFromJson(importExport.encodeToJson(exported)).feature

        assertEquals("starstave", decoded.jarnLiveWallpaper)
    }

    @Test
    fun `export writes off when nothing is chosen and the id when one is`() {
        assertEquals(JARN_LIVE_WALLPAPER_OFF, store.liveWallpaperBackup())

        store.setLiveWallpaperId("ironweave")

        assertEquals("ironweave", store.liveWallpaperBackup())
    }

    @Test
    fun `an older backup keeps the current choice`() = runTest {
        store.setLiveWallpaperId("hearthrise")

        restore("""{ "version": 44, "feature": { "artSource": "EsDe" } }""")

        assertEquals("hearthrise", store.liveWallpaperId)
    }

    @Test
    fun `an explicit off clears the current choice`() = runTest {
        store.setLiveWallpaperId("hearthrise")

        restore(backupWith(JARN_LIVE_WALLPAPER_OFF))

        assertNull(store.liveWallpaperId)
        assertNull(JarnLookStore(context).liveWallpaperId)
    }

    @Test
    fun `a backed up Tidemark choice restores`() = runTest {
        restore(backupWith("tidemark"))

        assertEquals("tidemark", store.liveWallpaperId)
    }

    @Test
    fun `an unknown id is kept as written and survives a restart`() = runTest {
        restore(backupWith("from-a-newer-build"))

        assertEquals("from-a-newer-build", store.liveWallpaperId)
        assertEquals("from-a-newer-build", JarnLookStore(context).liveWallpaperId)
    }
}
