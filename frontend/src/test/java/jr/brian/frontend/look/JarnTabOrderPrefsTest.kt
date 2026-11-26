package jr.brian.frontend.look

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnTabOrderPrefsTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `no order is saved until the player makes one`() {
        val display = JarnLookStore(context).display
        assertEquals(emptyList<String>(), display.librarySectionOrder)
        assertEquals(emptyList<String>(), display.deckTabOrder)
    }

    @Test
    fun `both orders survive a restart in the same order`() {
        JarnLookStore(context).updateDisplay {
            it.copy(librarySectionOrder = listOf("Recent", "Systems"), deckTabOrder = listOf("Canvas", "Now", "Apps"))
        }
        val reloaded = JarnLookStore(context).display
        assertEquals(listOf("Recent", "Systems"), reloaded.librarySectionOrder)
        assertEquals(listOf("Canvas", "Now", "Apps"), reloaded.deckTabOrder)
    }
}
