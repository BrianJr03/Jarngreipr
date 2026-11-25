package jr.brian.home.esde.data

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class HiddenGamesRestoreTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }

    private fun seeded() = ESDEPreferencesManager(context).apply {
        hideGame("snes/a.sfc")
        hideGame("gba/b.gba")
    }

    @Test
    fun `a backup replaces the hidden games and the result survives a reload`() {
        seeded().restoreHiddenGames(listOf("n64/c.z64"))

        assertEquals(setOf("n64/c.z64"), ESDEPreferencesManager(context).state.value.hiddenGames)
    }

    @Test
    fun `a backup without hidden games keeps the current ones`() {
        seeded()

        ESDEPreferencesManager(context).restoreHiddenGames(null)

        assertEquals(setOf("snes/a.sfc", "gba/b.gba"), ESDEPreferencesManager(context).state.value.hiddenGames)
    }

    @Test
    fun `a backup with an empty list shows every game again`() {
        seeded().restoreHiddenGames(emptyList())

        assertEquals(emptySet<String>(), ESDEPreferencesManager(context).state.value.hiddenGames)
    }
}
