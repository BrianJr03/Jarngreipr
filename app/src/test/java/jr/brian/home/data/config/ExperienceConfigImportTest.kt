package jr.brian.home.data.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import jr.brian.core.model.Experience
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

/**
 * Drives the real [ImportExportManager] decode and apply path against a real
 * [ExperienceStore], because [JarngreiprConfigRoundTripTest] only exercises
 * the serializer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ExperienceConfigImportTest {
    private val managers = mockk<ManagerContainer>(relaxed = true)
    private lateinit var experienceStore: ExperienceStore
    private lateinit var importExport: ImportExportManager

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { managers.ui.customAppNameManager.customNames } returns MutableStateFlow(emptyMap())
        every { managers.feature.esdePreferencesManager._state } returns MutableStateFlow(ESDEPrefsState())
        experienceStore = ExperienceStore(ApplicationProvider.getApplicationContext<Context>())
        importExport = ImportExportManager(
            managers = managers,
            screenAssignmentStore = mockk<ScreenAssignmentStore>(relaxed = true),
            experienceStore = experienceStore,
            artSourceStore = mockk(relaxed = true),
            libraryFolderBackup = mockk(relaxed = true),
            jarnLookStore = mockk(relaxed = true),
            artChoiceStore = jr.brian.frontend.library.art.GameArtChoiceStore(ApplicationProvider.getApplicationContext()),
            appPinStore = jr.brian.frontend.apps.JarnAppPinStore(ApplicationProvider.getApplicationContext()),
            playStatsBackup = io.mockk.mockk(relaxed = true),
            backupExtras = testBackupExtras(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `schema version includes the experience field`() {
        assertEquals(54, JarngreiprConfig.CONFIG_VERSION)
    }

    @Test
    fun `exported experience values round trip through the manager json`() {
        Experience.entries.forEach { experience ->
            val exported = JarngreiprConfig(feature = FeatureConfig(experience = experience.name))

            val encoded = importExport.encodeToJson(exported)

            assertTrue(encoded.contains("\"experience\": \"${experience.name}\""))
            assertEquals(experience.name, importExport.decodeFromJson(encoded).feature.experience)
        }
    }

    @Test
    fun `backup without the field decodes to null`() {
        assertNull(importExport.decodeFromJson(V37_BACKUP).feature.experience)
    }

    @Test
    fun `importing a v37 backup keeps classic`() = runTest {
        assertImportResult(Experience.Classic, V37_BACKUP, Experience.Classic)
    }

    @Test
    fun `importing a v37 backup keeps jarn fe`() = runTest {
        assertImportResult(Experience.JarnFE, V37_BACKUP, Experience.JarnFE)
    }

    @Test
    fun `importing a jarn fe backup applies jarn fe`() = runTest {
        assertImportResult(Experience.Classic, backupWith("JarnFE"), Experience.JarnFE)
    }

    @Test
    fun `importing a classic backup applies classic`() = runTest {
        assertImportResult(Experience.JarnFE, backupWith("Classic"), Experience.Classic)
    }

    @Test
    fun `importing an unknown experience applies classic`() = runTest {
        assertImportResult(Experience.JarnFE, backupWith("Future"), Experience.Classic)
    }

    private suspend fun assertImportResult(
        current: Experience,
        backup: String,
        expected: Experience,
    ) {
        experienceStore.setExperience(current)

        val result = importExport.importFromStream(ByteArrayInputStream(backup.toByteArray()))

        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
        assertEquals(expected, experienceStore.experience)
    }

    private fun backupWith(experience: String) =
        """{ "version": 38, "feature": { "experience": "$experience" } }"""

    private companion object {
        const val V37_BACKUP =
            """{ "version": 37, "feature": { "romSearch": { "frontendEnabled": true } } }"""
    }
}
