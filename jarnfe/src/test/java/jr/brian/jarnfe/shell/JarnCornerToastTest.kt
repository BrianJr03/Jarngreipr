package jr.brian.jarnfe.shell

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnCornerToastTest {
    @get:Rule
    val rule = createComposeRule()

    private fun string(id: Int) = ApplicationProvider.getApplicationContext<Application>().getString(id)

    private fun toast(title: Int, key: String? = null, durationMillis: Long = 2_500L) = JarnCornerToast(
        icon = JarnToastIcon.Res(R.drawable.ic_jarn_check),
        title = title,
        key = key,
        durationMillis = durationMillis,
    )

    private val first = toast(R.string.jarnfe_audio_toast_music_on)
    private val second = toast(R.string.jarnfe_audio_toast_music_muted)
    private val third = toast(R.string.jarnfe_audio_toast_jingles_on)

    @Before
    fun setUp() = JarnCornerToasts.clear()

    private fun host(onTopScreen: Boolean = true) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            JarnTheme { Box(Modifier.fillMaxSize()) { JarnCornerToastHost(onTopScreen) } }
        }
        rule.mainClock.advanceTimeByFrame()
    }

    @Test
    fun `different toasts wait their turn in order`() {
        JarnCornerToasts.show(first)
        JarnCornerToasts.show(second)
        JarnCornerToasts.show(third)
        assertEquals(first, JarnCornerToasts.current?.toast)
        assertEquals(listOf(second, third), JarnCornerToasts.waiting)

        JarnCornerToasts.dismiss(JarnCornerToasts.current!!)
        assertEquals(second, JarnCornerToasts.current?.toast)
        JarnCornerToasts.dismiss(JarnCornerToasts.current!!)
        assertEquals(third, JarnCornerToasts.current?.toast)
        JarnCornerToasts.dismiss(JarnCornerToasts.current!!)
        assertNull(JarnCornerToasts.current)
    }

    @Test
    fun `the same key replaces the shown toast and restarts it`() {
        val muted = toast(R.string.jarnfe_audio_toast_music_muted, key = "music")
        val on = toast(R.string.jarnfe_audio_toast_music_on, key = "music")
        JarnCornerToasts.show(muted)
        val before = JarnCornerToasts.current!!
        JarnCornerToasts.show(on)
        assertEquals(on, JarnCornerToasts.current?.toast)
        assertEquals(emptyList<JarnCornerToast>(), JarnCornerToasts.waiting)
        JarnCornerToasts.dismiss(before)
        assertEquals(on, JarnCornerToasts.current?.toast)
    }

    @Test
    fun `the same key replaces a waiting toast in its place`() {
        JarnCornerToasts.show(first)
        JarnCornerToasts.show(toast(R.string.jarnfe_audio_toast_jingles_on, key = "jingles"))
        JarnCornerToasts.show(second)
        val latest = toast(R.string.jarnfe_audio_toast_jingles_muted, key = "jingles")
        JarnCornerToasts.show(latest)
        assertEquals(listOf(latest, second), JarnCornerToasts.waiting)
    }

    @Test
    fun `past the cap the oldest waiting toast is dropped`() {
        JarnCornerToasts.show(first)
        val waiting = (0..JarnCornerToasts.MAX_QUEUED).map { toast(R.string.jarnfe_audio_toast_l3, key = "k$it") }
        waiting.forEach(JarnCornerToasts::show)
        assertEquals(waiting.drop(1), JarnCornerToasts.waiting)
        assertEquals(first, JarnCornerToasts.current?.toast)
    }

    @Test
    fun `the host shows a toast for its duration, then the next one`() {
        host()
        JarnCornerToasts.show(first)
        JarnCornerToasts.show(second)
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText(string(R.string.jarnfe_audio_toast_music_on)).assertExists()

        rule.mainClock.advanceTimeBy(2_500)
        rule.onNodeWithText(string(R.string.jarnfe_audio_toast_music_muted)).assertExists()

        rule.mainClock.advanceTimeBy(3_000)
        assertNull(JarnCornerToasts.current)
        rule.onNodeWithTag(JARN_CORNER_TOAST_TAG).assertDoesNotExist()
    }

    @Test
    fun `a replacement restarts the timer`() {
        host()
        JarnCornerToasts.show(toast(R.string.jarnfe_audio_toast_music_muted, key = "music"))
        rule.mainClock.advanceTimeBy(2_000)
        JarnCornerToasts.show(toast(R.string.jarnfe_audio_toast_music_on, key = "music"))
        rule.mainClock.advanceTimeBy(2_000)
        rule.onNodeWithText(string(R.string.jarnfe_audio_toast_music_on)).assertExists()
        rule.mainClock.advanceTimeBy(1_500)
        assertNull(JarnCornerToasts.current)
    }

    @Test
    fun `only the top screen draws toasts`() {
        host(onTopScreen = false)
        JarnCornerToasts.show(first)
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithTag(JARN_CORNER_TOAST_TAG).assertDoesNotExist()
    }

    @Test
    fun `TalkBack hears the title and subtitle politely`() {
        host()
        JarnCornerToasts.show(first.copy(subtitle = R.string.jarnfe_audio_toast_r3))
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithTag(JARN_CORNER_TOAST_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        val text = rule.onNodeWithTag(JARN_CORNER_TOAST_TAG).fetchSemanticsNode()
            .config[SemanticsProperties.Text].map { it.text }
        assertEquals(listOf(string(R.string.jarnfe_audio_toast_music_on), string(R.string.jarnfe_audio_toast_r3)), text)
    }
}
