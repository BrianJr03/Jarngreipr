package jr.brian.home.data.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import jr.brian.home.data.database.RssFeedDao
import jr.brian.home.data.database.RssFeedEntity
import jr.brian.home.ui.theme.managers.AppearanceRestoreSignal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class PersonalizationRestoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `appearance restores and exports back the same`() {
        val backup = AppearanceBackup(context)
        val signals = AppearanceRestoreSignal.restores.value
        val wanted = AppearanceConfig(
            themeId = "custom_9",
            customThemes = listOf(CustomThemeConfig("custom_9", "Night", "#FF101010", "#FF202020", true)),
            oledMode = true,
            keyboardOledExempt = true,
            iconShape = "SQUARE",
            tabAnimation = true,
        )
        backup.restore(wanted)
        assertEquals(wanted, backup.export())
        assertEquals(signals + 1, AppearanceRestoreSignal.restores.value)
    }

    @Test
    fun `an older backup leaves the look alone`() {
        val backup = AppearanceBackup(context)
        backup.restore(AppearanceConfig(oledMode = true, iconShape = "SQUARE"))
        val before = backup.export()
        backup.restore(null)
        backup.restore(AppearanceConfig())
        assertEquals(before, backup.export())
    }

    @Test
    fun `jingle repos are added and the switch is restored`() {
        val jingles = JingleBackup(context)
        jingles.restore(enabled = null, repos = listOf("me/mine"))
        jingles.restore(enabled = false, repos = listOf("owner/new", "bad"))
        assertEquals(listOf("me/mine", "owner/new"), jingles.repos)
        assertEquals(false, jingles.enabled)
        jingles.restore(enabled = null, repos = null)
        assertEquals(false, jingles.enabled)
    }

    @Test
    fun `feeds are inserted unrefreshed and existing ones reordered`() = runBlocking {
        val dao = mockk<RssFeedDao>(relaxed = true)
        coEvery { dao.getAllFeedsSnapshot() } returns listOf(RssFeedEntity("https://old.example", "Old", "", 30, 5L))
        RssFeedBackup(dao).restore(listOf(RssFeedConfig("https://new.example", 60)))
        coVerify { dao.insertFeed(RssFeedEntity("https://new.example", "https://new.example", "", 60, 0L, 0)) }
        coVerify { dao.updateSortOrder("https://old.example", 1) }
    }

    @Test
    fun `a full config round trip carries the new areas`() = runBlocking {
        val extras = testBackupExtras()
        JingleBackup(context).restore(enabled = true, repos = listOf("me/mine"))
        val built = extras.addTo(JarngreiprConfig())
        assertTrue(built.ui.appearance != null)
        assertEquals(emptyList<RssFeedConfig>(), built.feature.rssFeeds)
        assertEquals(listOf("me/mine"), built.feature.jingles.repos)
        assertEquals(emptyMap<String, String>(), built.page.widgetProviders)
    }
}
