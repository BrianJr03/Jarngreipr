package jr.brian.home.data.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import jr.brian.frontend.look.JarnLookStore
import jr.brian.frontend.look.JarnVideoFit
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

/** Drives the real export and import path against a real [JarnLookStore]. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class JarnVideoPreviewConfigImportTest {
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
    fun `schema version covers video previews`() {
        assertEquals(72, JarngreiprConfig.CONFIG_VERSION)
    }

    @Test
    fun `a backup carries both choices and the hint back`() = runTest {
        val backup = JarngreiprConfig(
            feature = FeatureConfig(
                jarnDisplay = JarnDisplayConfig(videoPreviews = true, videoPreviewSound = true),
                jarnVideoHintShown = true,
            ),
        )

        importBackup(importExport.encodeToJson(backup))

        assertTrue(store.display.videoPreviews)
        assertTrue(store.display.videoPreviewSound)
        assertTrue(store.videoHintShown)
    }

    @Test
    fun `an older backup keeps the current choices and hint`() = runTest {
        store.updateDisplay { it.copy(videoPreviews = true, videoPreviewSound = true) }
        store.markVideoHintShown()
        assertNull(importExport.decodeFromJson(V69_BACKUP).feature.jarnVideoHintShown)

        importBackup(V69_BACKUP)

        assertTrue(store.display.videoPreviews)
        assertTrue(store.display.videoPreviewSound)
        assertTrue(store.videoHintShown)
    }

    @Test
    fun `an older backup leaves a fresh install with everything off`() = runTest {
        importBackup(V69_BACKUP)

        assertFalse(store.display.videoPreviews)
        assertFalse(store.display.videoPreviewSound)
        assertFalse(store.videoHintShown)
    }

    @Test
    fun `crop comes back from a backup and survives a restart`() = runTest {
        importBackup("""{ "version": 71, "feature": { "jarnDisplay": { "videoFit": "crop" } } }""")
        assertEquals(JarnVideoFit.Crop, store.display.videoFit)
        assertEquals(JarnVideoFit.Crop, JarnLookStore(ApplicationProvider.getApplicationContext()).display.videoFit)
    }

    @Test
    fun `an older backup on a fresh install gives fit`() = runTest {
        importBackup(V69_BACKUP)

        assertEquals(JarnVideoFit.Fit, store.display.videoFit)
    }

    @Test
    fun `a backup never brings the hint back`() = runTest {
        store.markVideoHintShown()

        importBackup("""{ "version": 70, "feature": { "jarnVideoHintShown": false } }""")

        assertTrue(store.videoHintShown)
    }

    private suspend fun importBackup(json: String) {
        val result = importExport.importFromStream(ByteArrayInputStream(json.toByteArray()))
        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
    }

    private companion object {
        const val V69_BACKUP =
            """{ "version": 69, "feature": { "jarnDisplay": { "playingScreenOff": true } } }"""
    }
}
