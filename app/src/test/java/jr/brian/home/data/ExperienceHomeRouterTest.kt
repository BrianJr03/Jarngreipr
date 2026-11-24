package jr.brian.home.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import jr.brian.core.model.Experience
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ExperienceHomeRouterTest {
    private lateinit var store: ExperienceStore
    private var opens = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("experience_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        store = ExperienceStore(context)
    }

    private fun route(
        jarnAvailable: Boolean = true,
        forceClassic: Boolean = false,
        isLoopFree: Boolean = true,
        opened: Boolean = true,
        storeProvider: () -> ExperienceStore = { store },
    ) = openChosenJarn(jarnAvailable, storeProvider, forceClassic, { isLoopFree }) {
        opens++
        opened
    }

    @Test
    fun `a build without jarn never reads the store or opens it`() {
        val result = route(jarnAvailable = false, storeProvider = { error("store was read") })
        assertFalse(result)
        assertEquals(0, opens)
    }

    @Test
    fun `classic stays on classic`() {
        store.setExperience(Experience.Classic)
        assertFalse(route())
        assertEquals(0, opens)
    }

    @Test
    fun `jarn opens jarn`() {
        store.setExperience(Experience.JarnFE)
        assertTrue(route())
        assertEquals(1, opens)
    }

    @Test
    fun `a failed launch falls back without saving a choice`() {
        assertFalse(route(opened = false))
        assertNull(store.storedExperience)
    }

    @Test
    fun `an opened default is saved`() {
        assertTrue(route())
        assertEquals(Experience.JarnFE, store.storedExperience)
    }

    @Test
    fun `the classic override keeps the saved jarn choice`() {
        store.setExperience(Experience.JarnFE)
        assertFalse(route(forceClassic = true))
        assertEquals(0, opens)
        assertEquals(Experience.JarnFE, store.storedExperience)
    }

    @Test
    fun `a detected loop goes to classic without opening jarn`() {
        store.setExperience(Experience.JarnFE)
        assertFalse(route(isLoopFree = false))
        assertEquals(0, opens)
    }
}
