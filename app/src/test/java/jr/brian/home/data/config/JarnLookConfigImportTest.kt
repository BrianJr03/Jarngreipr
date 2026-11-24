package jr.brian.home.data.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import jr.brian.core.ui.theme.JarnSwitchCue
import jr.brian.frontend.library.art.ArtSourceStore
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.frontend.look.JarnTapControl
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
            playStatsBackup = io.mockk.mockk(relaxed = true),
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

    @Test
    fun `controller after tap round trips through a backup`() = runTest {
        val display = JarnDisplayConfig(tapControl = JarnTapControl.AlwaysBottom.id)
        val exported = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = display))
        val json = importExport.encodeToJson(exported)

        assertEquals(display, importExport.decodeFromJson(json).feature.jarnDisplay)
        importExport.importFromStream(ByteArrayInputStream(json.toByteArray()))

        assertEquals(JarnTapControl.AlwaysBottom, store.display.tapControl)
        assertEquals("bottom", store.display.toConfig().tapControl)
    }

    @Test
    fun `a null or unknown controller after tap keeps the current choice`() = runTest {
        store.updateDisplay { it.copy(tapControl = JarnTapControl.AlwaysTop) }
        listOf("null", "\"sideways\"").forEach { value ->
            val backup = """{ "version": 46, "feature": { "jarnDisplay": { "tapControl": $value } } }"""

            val result = importExport.importFromStream(ByteArrayInputStream(backup.toByteArray()))

            assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
            assertEquals(JarnTapControl.AlwaysTop, store.display.tapControl)
        }
    }

    @Test
    fun `app names default to visible`() {
        assertTrue(JarnDisplayPrefs().appNamesVisible)
        assertTrue(store.display.appNamesVisible)
    }

    @Test
    fun `hidden app names survive a store reload`() {
        store.updateDisplay { it.copy(appNamesVisible = false) }

        val reloaded = JarnLookStore(ApplicationProvider.getApplicationContext())

        assertFalse(reloaded.display.appNamesVisible)
    }

    @Test
    fun `hidden app names round trip through a backup`() = runTest {
        val display = JarnDisplayConfig(appNamesVisible = false)
        val exported = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = display))
        val json = importExport.encodeToJson(exported)

        assertEquals(display, importExport.decodeFromJson(json).feature.jarnDisplay)
        val result = importExport.importFromStream(ByteArrayInputStream(json.toByteArray()))

        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
        assertFalse(store.display.appNamesVisible)
        assertEquals(false, store.display.toConfig().appNamesVisible)
    }

    @Test
    fun `a backup can turn app names back on`() = runTest {
        store.updateDisplay { it.copy(appNamesVisible = false) }
        val backup = """{ "version": 47, "feature": { "jarnDisplay": { "appNamesVisible": true } } }"""

        val result = importExport.importFromStream(ByteArrayInputStream(backup.toByteArray()))

        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
        assertTrue(store.display.appNamesVisible)
    }

    @Test
    fun `a null or missing app names value keeps the current choice`() = runTest {
        store.updateDisplay { it.copy(appNamesVisible = false) }
        listOf("""{ "appNamesVisible": null }""", """{ "hintsVisible": true }""").forEach { display ->
            val backup = """{ "version": 46, "feature": { "jarnDisplay": $display } }"""

            val result = importExport.importFromStream(ByteArrayInputStream(backup.toByteArray()))

            assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
            assertFalse(store.display.appNamesVisible)
        }
    }

    @Test
    fun `undo countdown defaults to visible`() {
        assertTrue(JarnDisplayPrefs().undoCountdownVisible)
        assertTrue(store.display.undoCountdownVisible)
    }

    @Test
    fun `hidden undo countdown survives a store reload`() {
        store.updateDisplay { it.copy(undoCountdownVisible = false) }

        val reloaded = JarnLookStore(ApplicationProvider.getApplicationContext())

        assertFalse(reloaded.display.undoCountdownVisible)
    }

    @Test
    fun `hidden undo countdown round trips through a backup`() = runTest {
        val display = JarnDisplayConfig(undoCountdownVisible = false)
        val exported = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = display))
        val json = importExport.encodeToJson(exported)

        assertEquals(display, importExport.decodeFromJson(json).feature.jarnDisplay)
        val result = importExport.importFromStream(ByteArrayInputStream(json.toByteArray()))

        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
        assertFalse(store.display.undoCountdownVisible)
        assertEquals(false, store.display.toConfig().undoCountdownVisible)
    }

    @Test
    fun `a null or missing undo countdown keeps the current choice`() = runTest {
        store.updateDisplay { it.copy(undoCountdownVisible = false) }
        listOf("""{ "undoCountdownVisible": null }""", """{ "hintsVisible": true }""").forEach { display ->
            val backup = """{ "version": 46, "feature": { "jarnDisplay": $display } }"""

            val result = importExport.importFromStream(ByteArrayInputStream(backup.toByteArray()))

            assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
            assertFalse(store.display.undoCountdownVisible)
        }
    }

    @Test
    fun `matching defaults to off`() {
        assertFalse(store.matchCommandPanel)
    }

    @Test
    fun `matching round trips through a backup without touching the look`() = runTest {
        store.setLookId("grove")
        val exported = JarngreiprConfig(
            feature = FeatureConfig(jarnLook = "grove", jarnMatchCommandPanel = true),
        )
        val json = importExport.encodeToJson(exported)

        assertEquals(true, importExport.decodeFromJson(json).feature.jarnMatchCommandPanel)
        val result = importExport.importFromStream(ByteArrayInputStream(json.toByteArray()))

        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
        assertTrue(store.matchCommandPanel)
        assertEquals("grove", store.lookId)
    }

    @Test
    fun `a backup can turn matching off`() = runTest {
        store.setMatchCommandPanel(true)
        val backup = """{ "version": 50, "feature": { "jarnMatchCommandPanel": false } }"""

        val result = importExport.importFromStream(ByteArrayInputStream(backup.toByteArray()))

        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
        assertFalse(store.matchCommandPanel)
    }

    @Test
    fun `an older backup leaves matching alone in either state`() = runTest {
        listOf(true, false).forEach { current ->
            store.setMatchCommandPanel(current)

            val result = importExport.importFromStream(ByteArrayInputStream(V40_BACKUP.toByteArray()))

            assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
            assertEquals(current, store.matchCommandPanel)
        }
    }

    @Test
    fun `matching survives a store reload and never changes the saved look`() {
        store.setLookId("forge")
        store.setMatchCommandPanel(true)

        val reloaded = JarnLookStore(ApplicationProvider.getApplicationContext())

        assertTrue(reloaded.matchCommandPanel)
        assertEquals("forge", reloaded.lookId)
    }

    @Test
    fun `a backup never carries the palette`() {
        val json = importExport.encodeToJson(
            JarngreiprConfig(feature = FeatureConfig(jarnMatchCommandPanel = true)),
        )

        assertFalse(json.contains("palette", ignoreCase = true))
    }

    @Test
    fun `switch cue survives a backup`() = runTest {
        store.updateDisplay { it.copy(switchCue = JarnSwitchCue.TabMark) }
        val json = importExport.encodeToJson(
            JarngreiprConfig(feature = FeatureConfig(jarnDisplay = store.display.toConfig())),
        )
        store.updateDisplay { it.copy(switchCue = JarnSwitchCue.Pulse) }

        val result = importExport.importFromStream(ByteArrayInputStream(json.toByteArray()))

        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
        assertEquals(JarnSwitchCue.TabMark, store.display.switchCue)
    }

    @Test
    fun `an older backup leaves the switch cue alone`() = runTest {
        store.updateDisplay { it.copy(switchCue = JarnSwitchCue.Dim) }

        val result = importExport.importFromStream(ByteArrayInputStream(V40_BACKUP.toByteArray()))

        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
        assertEquals(JarnSwitchCue.Dim, store.display.switchCue)
    }

    @Test
    fun `an unknown switch cue falls back to pulse`() = runTest {
        store.updateDisplay { it.copy(switchCue = JarnSwitchCue.Dim) }
        val backup = """{ "version": 51, "feature": { "jarnDisplay": { "switchCue": "future" } } }"""

        val result = importExport.importFromStream(ByteArrayInputStream(backup.toByteArray()))

        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
        assertEquals(JarnSwitchCue.Pulse, store.display.switchCue)
    }

    @Test
    fun `schema version covers app names`() {
        assertEquals(51, JarngreiprConfig.CONFIG_VERSION)
    }

    private companion object {
        const val V40_BACKUP = """{ "version": 40, "feature": { "artSource": "EsDe" } }"""
    }
}
