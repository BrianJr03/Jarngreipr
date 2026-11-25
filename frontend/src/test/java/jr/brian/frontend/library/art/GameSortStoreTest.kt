package jr.brian.frontend.library.art

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class GameSortStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `a picked sort survives a reload`() {
        GameSortStore(context).set("snes", "MostPlayed")

        assertEquals(mapOf("snes" to "MostPlayed"), GameSortStore(context).state.value)
    }

    @Test
    fun `restore replaces every system's sort and drops blank entries`() {
        val store = GameSortStore(context).apply { set("snes", "Name") }

        store.restore(mapOf("gba" to "ReleaseYear", "" to "Name", "n64" to ""))

        assertEquals(mapOf("gba" to "ReleaseYear"), GameSortStore(context).state.value)
    }

    @Test
    fun `damaged storage reads as no sorts`() {
        assertEquals(emptyMap<String, String>(), decodeGameSorts("{not json"))
        assertEquals(emptyMap<String, String>(), decodeGameSorts(null))
    }
}
