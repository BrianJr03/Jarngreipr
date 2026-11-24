package jr.brian.home.data.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import jr.brian.core.model.Experience
import jr.brian.frontend.apps.JarnAppPinStore
import jr.brian.frontend.library.art.ArtSourceStore
import jr.brian.frontend.library.art.GameArtChoiceStore
import jr.brian.frontend.look.JarnLookStore
import jr.brian.home.data.ExperienceStore
import jr.brian.home.data.ManagerContainer
import jr.brian.home.data.ScreenAssignmentStore
import jr.brian.home.data.switchExperience
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

/** Switching changes the selector alone, and a sanitized backup restores through the real path. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ExperienceSwitchSurvivalTest {
    private val managers = mockk<ManagerContainer>(relaxed = true)
    private lateinit var context: Context
    private lateinit var experience: ExperienceStore
    private lateinit var pins: JarnAppPinStore
    private lateinit var importExport: ImportExportManager

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { managers.ui.customAppNameManager.customNames } returns MutableStateFlow(emptyMap())
        every { managers.feature.esdePreferencesManager._state } returns MutableStateFlow(ESDEPrefsState())
        context = ApplicationProvider.getApplicationContext()
        experience = ExperienceStore(context)
        pins = JarnAppPinStore(context)
        importExport = ImportExportManager(
            managers = managers,
            screenAssignmentStore = mockk<ScreenAssignmentStore>(relaxed = true),
            experienceStore = experience,
            artSourceStore = ArtSourceStore(context),
            libraryFolderBackup = mockk(relaxed = true),
            jarnLookStore = JarnLookStore(context),
            artChoiceStore = GameArtChoiceStore(context),
            appPinStore = pins,
            playStatsBackup = io.mockk.mockk(relaxed = true),
            backupExtras = testBackupExtras(),
            jarnPreviewStore = io.mockk.mockk(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `switching both ways leaves other saved data untouched`() {
        pins.setPinned("keep.me", true)
        val before = allPrefsExceptSelector()

        assertTrue(switchExperience(experience, Experience.JarnFE) { true })
        assertTrue(switchExperience(experience, Experience.Classic) { true })

        assertEquals(Experience.Classic, ExperienceStore(context).experience)
        assertEquals(before, allPrefsExceptSelector())
        assertEquals(listOf("keep.me"), JarnAppPinStore(context).pinned)
    }

    @Test
    fun `a switch that fails to open keeps the saved choice`() {
        experience.setExperience(Experience.JarnFE)

        assertFalse(switchExperience(experience, Experience.Classic) { false })
        assertFalse(switchExperience(experience, Experience.Classic) { error("no activity") })

        assertEquals(Experience.JarnFE, ExperienceStore(context).experience)
    }

    @Test
    fun `a sanitized backup restores its experience and pins end to end`() = runTest {
        val result = importExport.importFromStream(ByteArrayInputStream(SANITIZED_BACKUP.toByteArray()))

        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
        assertEquals(Experience.JarnFE, ExperienceStore(context).experience)
        assertEquals(listOf("com.example.one", "com.example.two"), JarnAppPinStore(context).pinned)
    }

    @Test
    fun `a backup from before the selector keeps the current experience and pins`() = runTest {
        experience.setExperience(Experience.JarnFE)
        pins.setPinned("keep.me", true)

        val result = importExport.importFromStream(ByteArrayInputStream(OLD_BACKUP.toByteArray()))

        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
        assertEquals(Experience.JarnFE, experience.experience)
        assertEquals(listOf("keep.me"), pins.pinned)
    }

    private fun allPrefsExceptSelector(): Map<String, Map<String, *>> =
        context.filesDir.parentFile!!.resolve("shared_prefs")
            .listFiles().orEmpty()
            .map { it.nameWithoutExtension }
            .filter { it != "experience_prefs" }
            .associateWith { context.getSharedPreferences(it, Context.MODE_PRIVATE).all }

    private companion object {
        const val OLD_BACKUP = """{ "version": 37, "feature": { "romSearch": { "frontendEnabled": true } } }"""
        const val SANITIZED_BACKUP = """
            {
              "version": 46,
              "exportedAt": "2026-01-01T00:00:00Z",
              "feature": {
                "experience": "JarnFE",
                "jarnPinnedApps": ["com.example.one", "com.example.two"],
                "romSearch": { "frontendEnabled": true }
              }
            }
        """
    }
}
