package jr.brian.home.data.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import jr.brian.frontend.library.art.ArtSourceStore
import jr.brian.frontend.look.JarnDisplayPrefs
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

/** Drives the real import path against a real [JarnLookStore]. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class JarnLookConfigImportTest {
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
            artSourceStore = ArtSourceStore(context),
            libraryFolderBackup = mockk(relaxed = true),
            jarnLookStore = store,
            artChoiceStore = jr.brian.frontend.library.art.GameArtChoiceStore(ApplicationProvider.getApplicationContext()),
            appPinStore = jr.brian.frontend.apps.JarnAppPinStore(ApplicationProvider.getApplicationContext()),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `look fields round trip through the manager json`() {
        val exported = JarngreiprConfig(
            feature = FeatureConfig(jarnLook = "night", jarnReducedMotion = true),
        )

        val decoded = importExport.decodeFromJson(importExport.encodeToJson(exported)).feature

        assertEquals("night", decoded.jarnLook)
        assertEquals(true, decoded.jarnReducedMotion)
    }

    @Test
    fun `backup without the fields decodes to null`() {
        val decoded = importExport.decodeFromJson(V40_BACKUP).feature

        assertNull(decoded.jarnLook)
        assertNull(decoded.jarnReducedMotion)
    }

    @Test
    fun `importing an older backup keeps the current look and motion`() = runTest {
        store.setLookId("grove")
        store.setReducedMotion(true)

        val result = importExport.importFromStream(ByteArrayInputStream(V40_BACKUP.toByteArray()))

        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
        assertEquals("grove", store.lookId)
        assertTrue(store.reducedMotion)
    }

    @Test
    fun `importing a backup applies its look and motion`() = runTest {
        store.setLookId("grove")
        store.setReducedMotion(true)
        val backup = """{ "version": 41, "feature": { "jarnLook": "forge", "jarnReducedMotion": false } }"""

        val result = importExport.importFromStream(ByteArrayInputStream(backup.toByteArray()))

        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
        assertEquals("forge", store.lookId)
        assertFalse(store.reducedMotion)
    }

    @Test
    fun `display choices round trip through the manager json`() {
        val display = JarnDisplayConfig(1.25f, 0.8f, hintsVisible = false, buttonHaptics = false)
        val exported = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = display))

        val decoded = importExport.decodeFromJson(importExport.encodeToJson(exported)).feature

        assertEquals(display, decoded.jarnDisplay)
    }

    @Test
    fun `importing an older backup keeps the current display choices`() = runTest {
        val current = JarnDisplayPrefs(1.25f, 0.8f, hintsVisible = false, buttonHaptics = false)
        store.updateDisplay { current }

        val result = importExport.importFromStream(ByteArrayInputStream(V40_BACKUP.toByteArray()))

        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
        assertEquals(current, store.display)
    }

    @Test
    fun `a backup with null display fields only changes what it carries`() = runTest {
        store.updateDisplay { JarnDisplayPrefs(1.25f, 0.8f, hintsVisible = false, buttonHaptics = false) }
        val backup = """{ "version": 44, "feature": { "jarnDisplay": { "gameTileScale": 1.5, "hintsVisible": null } } }"""

        val result = importExport.importFromStream(ByteArrayInputStream(backup.toByteArray()))

        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
        assertEquals(JarnDisplayPrefs(1.25f, 1.5f, hintsVisible = false, buttonHaptics = false), store.display)
    }

    @Test
    fun `a backup with an out of range tile size is clamped`() = runTest {
        val backup = """{ "version": 44, "feature": { "jarnDisplay": { "systemTileScale": 9.0 } } }"""

        importExport.importFromStream(ByteArrayInputStream(backup.toByteArray()))

        assertEquals(JarnDisplayPrefs.MAX_TILE_SCALE, store.display.systemTileScale)
    }

    private companion object {
        const val V40_BACKUP = """{ "version": 40, "feature": { "artSource": "EsDe" } }"""
    }
}
