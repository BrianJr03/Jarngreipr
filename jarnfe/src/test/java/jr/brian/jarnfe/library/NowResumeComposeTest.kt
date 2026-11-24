package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.launch.JarnGameFileStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NowResumeComposeTest {
    @get:Rule
    val rule = createComposeRule()

    private val played = mutableListOf<String>()
    private val game = JarnLibraryGame(
        id = "snes/saltglass",
        systemId = "snes",
        title = "Saltglass Run (USA)",
        metadata = JarnGameMetadata(
            playtimeMinutes = 125,
            lastPlayedAtMs = System.currentTimeMillis(),
            playCount = 3,
        ),
    )

    private fun string(id: Int, vararg args: Any) =
        ApplicationProvider.getApplicationContext<Application>().getString(id, *args)

    @After
    fun tearDown() {
        JarnLibrary.onPlay = null
        JarnResumeNotice.dismiss()
    }

    private fun showPoster(status: JarnGameFileStatus?) {
        JarnLibrary.onPlay = { played += it.id }
        val focus = FocusRequester()
        rule.setContent {
            JarnTheme {
                Column {
                    NowResumeFacts(game, status)
                    NowResumeButton(game, status, focus)
                    ResumeMissingDialog()
                }
            }
        }
    }

    @Test
    fun `a present game shows its play stats and resumes on A`() {
        showPoster(JarnGameFileStatus.Present)
        rule.onNodeWithText(string(R.string.jarnfe_resume_file_missing)).assertDoesNotExist()
        rule.onNodeWithText(string(R.string.jarnfe_library_last_played, string(R.string.jarnfe_resume_today)), substring = true)
            .assertIsDisplayed()
        rule.onNodeWithText(string(R.string.jarnfe_hint_resume)).performClick()
        assertEquals(listOf(game.id), played)
        assertNull(JarnResumeNotice.game)
    }

    @Test
    fun `a missing file says so, never launches, and offers a refresh`() {
        showPoster(JarnGameFileStatus.Missing)
        rule.onNodeWithText(string(R.string.jarnfe_resume_file_missing)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.jarnfe_library_last_played, string(R.string.jarnfe_resume_today)), substring = true)
            .assertDoesNotExist()
        rule.onNodeWithText(string(R.string.jarnfe_hint_resume)).performClick()
        rule.waitForIdle()
        assertTrue(played.isEmpty())
        assertEquals(game, JarnResumeNotice.game)
        rule.onNodeWithText(string(R.string.jarnfe_resume_missing_title)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.jarnfe_resume_missing_message, "Saltglass Run")).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.jarnfe_settings_refresh)).performClick()
        rule.waitForIdle()
        assertNull(JarnResumeNotice.game)
        assertTrue(played.isEmpty())
    }
}
