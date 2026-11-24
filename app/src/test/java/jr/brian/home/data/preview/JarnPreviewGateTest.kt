package jr.brian.home.data.preview

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import jr.brian.core.model.Experience
import jr.brian.frontend.look.JarnLookStore
import jr.brian.home.data.ExperienceStore
import jr.brian.home.data.openChosenJarn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class JarnPreviewGateTest {
    private lateinit var context: Context
    private lateinit var experienceStore: ExperienceStore
    private lateinit var lookStore: JarnLookStore
    private lateinit var store: JarnPreviewStore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        experienceStore = ExperienceStore(context)
        lookStore = JarnLookStore(context)
        store = JarnPreviewStore(context, experienceStore, lookStore)
    }

    @Test
    fun `a fresh state allows a showing`() {
        assertTrue(JarnPreviewState().allowsShowing("3.2.0"))
    }

    @Test
    fun `never twice in one version`() {
        store.recordShown("3.2.0")
        store.recordShown("3.2.0")

        assertEquals(1, store.current.showings)
        assertFalse(store.current.allowsShowing("3.2.0"))
        assertTrue(store.current.allowsShowing("3.3.0"))
    }

    @Test
    fun `three showings retire it`() {
        listOf("1", "2", "3").forEach(store::recordShown)

        assertEquals(3, store.current.showings)
        assertFalse(store.current.allowsShowing("4"))
    }

    @Test
    fun `pressing Try Jarn retires it`() {
        store.markTried()

        assertTrue(store.current.tried)
        assertFalse(store.current.allowsShowing("3.2.0"))
    }

    @Test
    fun `saving Jarn once retires it even after returning to Classic`() = runTest {
        experienceStore.setExperience(Experience.JarnFE)
        experienceStore.setExperience(Experience.Classic)

        assertTrue(store.current.tried)
        assertTrue(store.state.first().tried)
        assertTrue(ExperienceStore(context).everJarnFe)
    }

    @Test
    fun `a finished Jarn tour retires it`() = runTest {
        lookStore.markFirstRunDone()

        assertTrue(store.state.first().tried)
    }

    @Test
    fun `state survives a new store`() {
        store.recordShown("3.2.0")
        store.markTried()

        val reloaded = JarnPreviewStore(context, experienceStore, lookStore).current
        assertEquals(1, reloaded.showings)
        assertEquals("3.2.0", reloaded.lastShownVersion)
        assertTrue(reloaded.tried)
    }

    @Test
    fun `a debug build that offers Jarn shows it once per version`() {
        assertTrue(store.shouldShow("3.2.0"))

        store.recordShown("3.2.0")

        assertFalse(store.shouldShow("3.2.0"))
        assertTrue(store.shouldShow("3.3.0"))
    }

    @Test
    fun `a fresh install's home press opens classic, so meet jarn shows`() {
        var jarnOpened = false
        val openedJarn = openChosenJarn(
            jarnAvailable = true,
            store = { experienceStore },
            forceClassic = false,
            isLoopFree = { true },
        ) {
            jarnOpened = true
            true
        }

        assertFalse(openedJarn)
        assertFalse(jarnOpened)
        assertEquals(null, experienceStore.storedExperience)
        assertFalse(experienceStore.everJarnFe)
        assertTrue(store.shouldShow("4.0.0"))
    }

    @Test
    fun `a saved Jarn experience never shows it`() {
        experienceStore.setExperience(Experience.JarnFE)

        assertFalse(store.shouldShow("3.2.0"))
    }

    @Test
    fun `install kind tells an update from a fresh install`() {
        assertEquals(JarnInstallKind.FreshInstall, installKindOf(null, 10L, 10L))
        assertEquals(JarnInstallKind.FreshInstall, installKindOf("", 10L, 10L))
        assertEquals(JarnInstallKind.Update, installKindOf("3.1.0", 10L, 10L))
        assertEquals(JarnInstallKind.Update, installKindOf(null, 10L, 20L))
    }

    @Test
    fun `restore only raises the count and only turns tried on`() {
        val now = JarnPreviewSaved(showings = 2, lastShownVersion = "3.2.0", tried = true)

        val merged = now.mergedWith(showings = 1, lastShownVersion = "3.0.0", tried = false)

        assertEquals(now, merged)
    }

    @Test
    fun `restore takes the newer count and its version`() {
        val merged = JarnPreviewSaved(1, "3.1.0", false).mergedWith(3, "3.2.0", true)

        assertEquals(JarnPreviewSaved(3, "3.2.0", true), merged)
    }

    @Test
    fun `restore fills a missing version without lowering anything`() {
        val merged = JarnPreviewSaved().mergedWith(null, "3.1.0", null)

        assertEquals(JarnPreviewSaved(0, "3.1.0", false), merged)
    }
}
