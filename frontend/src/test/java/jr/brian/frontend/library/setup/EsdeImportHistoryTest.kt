package jr.brian.frontend.library.setup

import android.app.Application
import android.content.Context
import jr.brian.frontend.library.migrate.EsdeImportSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EsdeImportHistoryTest {
    private lateinit var app: Application

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        app.getSharedPreferences("jarn_esde_import", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `nothing is saved before the first import`() {
        assertNull(EsdeImportHistory(app).last.value)
    }

    @Test
    fun `the last summary survives a restart`() {
        val summary = EsdeImportSummary(
            gamelistsFound = 4,
            systemsMatched = 3,
            unrecognisedSystems = listOf("mystery", "other-nds"),
            gamesUpdated = 40,
            favoritesAdded = 6,
            playStatsUpdated = 12,
            unreadable = 1,
        )
        EsdeImportHistory(app).save(summary)

        assertEquals(summary, EsdeImportHistory(app).last.value)
    }
}
