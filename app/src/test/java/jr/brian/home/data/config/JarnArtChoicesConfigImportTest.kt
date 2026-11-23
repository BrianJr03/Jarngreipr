package jr.brian.home.data.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import jr.brian.frontend.library.art.ArtChoice
import jr.brian.frontend.library.art.ArtScreen
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

/** Drives the real import path against a real [GameArtChoiceStore]. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class JarnArtChoicesConfigImportTest {
    private val managers = mockk<ManagerContainer>(relaxed = true)
    private lateinit var store: GameArtChoiceStore
    private lateinit var importExport: ImportExportManager

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { managers.ui.customAppNameManager.customNames } returns MutableStateFlow(emptyMap())
        every { managers.feature.esdePreferencesManager._state } returns MutableStateFlow(ESDEPrefsState())
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = GameArtChoiceStore(context)
        importExport = ImportExportManager(
            managers = managers,
            screenAssignmentStore = mockk<ScreenAssignmentStore>(relaxed = true),
            experienceStore = ExperienceStore(context),
            artSourceStore = ArtSourceStore(context),
            libraryFolderBackup = mockk(relaxed = true),
            jarnLookStore = JarnLookStore(context),
            artChoiceStore = store,
            appPinStore = jr.brian.frontend.apps.JarnAppPinStore(ApplicationProvider.getApplicationContext()),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `art choices round trip through the manager json`() {
        store.setForSystem(ArtScreen.Top, "snes", ArtChoice.Miximage)
        store.setForGame(ArtScreen.Bottom, GAME, "snes", ArtChoice.PhysicalMedia)
        val exported = JarngreiprConfig(feature = FeatureConfig(jarnArtChoices = store.choices.toConfig()))

        val decoded = importExport.decodeFromJson(importExport.encodeToJson(exported)).feature

        assertEquals(store.choices, decoded.jarnArtChoices?.toChoices())
    }

    @Test
    fun `backup without the field decodes to null`() {
        assertNull(importExport.decodeFromJson(V41_BACKUP).feature.jarnArtChoices)
    }

    @Test
    fun `importing an older backup keeps current choices`() = runTest {
        store.setForSystem(ArtScreen.Top, "snes", ArtChoice.Fanart)
        val before = store.choices

        val result = importExport.importFromStream(ByteArrayInputStream(V41_BACKUP.toByteArray()))

        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
        assertEquals(before, store.choices)
    }

    @Test
    fun `importing a backup replaces choices and skips unknown kinds`() = runTest {
        store.setForSystem(ArtScreen.Top, "gba", ArtChoice.Fanart)
        val backup = """{ "version": 42, "feature": { "jarnArtChoices": {
            "topBySystem": { "snes": "Marquee", "nes": "Hologram" },
            "bottomByGame": { "$GAME": "Screenshot" } } } }"""

        val result = importExport.importFromStream(ByteArrayInputStream(backup.toByteArray()))

        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
        assertEquals(mapOf("snes" to ArtChoice.Marquee), store.choices.topBySystem)
        assertEquals(ArtChoice.Screenshot, store.choices.effective(ArtScreen.Bottom, GAME, "snes"))
    }

    private companion object {
        const val GAME = "1:snes:Game.sfc"
        const val V41_BACKUP = """{ "version": 41, "feature": { "jarnLook": "forge" } }"""
    }
}
