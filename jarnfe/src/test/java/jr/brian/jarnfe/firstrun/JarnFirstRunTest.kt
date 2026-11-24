package jr.brian.jarnfe.firstrun

import jr.brian.core.model.ArtSource
import jr.brian.core.ui.theme.JarnSwitchCue
import jr.brian.frontend.library.art.ArtSourceStatus
import jr.brian.jarnfe.session.StageRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnFirstRunTest {
    private class FakeProgress(
        override var isDone: Boolean = false,
        override var savedStep: String? = null,
    ) : JarnFirstRunProgress {
        override fun saveStep(step: String) {
            savedStep = step
        }

        override fun markDone() {
            isDone = true
            savedStep = null
        }
    }

    @Test
    fun `every step can move forward until the tour ends`() {
        listOf(true, false).forEach { twoScreens ->
            var run: JarnFirstRunState? = JarnFirstRunState()
            val seen = mutableListOf<JarnFirstRunStep>()
            while (run != null) {
                seen += run.step
                run = run.next(twoScreens)
            }
            assertEquals(JarnFirstRunStep.available(twoScreens), seen)
        }
    }

    @Test
    fun `every step can go back and the first asks before leaving`() {
        JarnFirstRunStep.available(twoScreens = true).drop(1).forEach { step ->
            val back = JarnFirstRunState(step).back(twoScreens = true)
            assertFalse(back.confirmingSkip)
            assertEquals(step.ordinal - 1, back.step.ordinal)
        }
        val asking = JarnFirstRunState().back(twoScreens = true)
        assertTrue(asking.confirmingSkip)
        assertFalse(asking.back(twoScreens = true).confirmingSkip)
        assertEquals(JarnFirstRunStep.Welcome, asking.next(twoScreens = true)?.step)
    }

    @Test
    fun `single screen drops the steps that need a second display`() {
        val steps = JarnFirstRunStep.available(twoScreens = false)
        assertFalse(JarnFirstRunStep.PressSelect in steps)
        assertFalse(JarnFirstRunStep.PressOtherTabs in steps)
        assertTrue(JarnFirstRunStep.PressX in steps)
    }

    @Test
    fun `losing the second display moves past a step that needs it`() {
        assertEquals(
            JarnFirstRunStep.PressTabs,
            JarnFirstRunState(JarnFirstRunStep.PressSelect).settled(twoScreens = false).step,
        )
        assertEquals(
            JarnFirstRunStep.Done,
            JarnFirstRunState(JarnFirstRunStep.PressOtherTabs).settled(twoScreens = false).step,
        )
    }

    @Test
    fun `the cue step follows the look step on two screens only`() {
        val two = JarnFirstRunStep.available(twoScreens = true)
        assertEquals(two.indexOf(JarnFirstRunStep.Look) + 1, two.indexOf(JarnFirstRunStep.SwitchCue))
        assertFalse(JarnFirstRunStep.SwitchCue in JarnFirstRunStep.available(twoScreens = false))
        assertEquals(
            JarnFirstRunStep.PressA,
            JarnFirstRunState(JarnFirstRunStep.SwitchCue).settled(twoScreens = false).step,
        )
    }

    @Test
    fun `the cue step resumes by name and has no gesture for A to trigger`() {
        val progress = FakeProgress(savedStep = JarnFirstRunStep.SwitchCue.name)
        assertEquals(JarnFirstRunStep.SwitchCue, JarnFirstRunController(progress).state?.step)
        assertNull(JarnFirstRunStep.SwitchCue.gesture)
    }

    @Test
    fun `a highlighted cue previews until focus leaves it`() {
        assertEquals(JarnSwitchCue.Dim, nextPreviewCue(null, JarnSwitchCue.Dim, isFocused = true))
        assertNull(nextPreviewCue(JarnSwitchCue.Dim, JarnSwitchCue.Dim, isFocused = false))
        assertEquals(
            JarnSwitchCue.TabMark,
            nextPreviewCue(JarnSwitchCue.TabMark, JarnSwitchCue.Dim, isFocused = false),
        )
    }

    @Test
    fun `only the taught gesture counts as practice`() {
        val run = JarnFirstRunState(JarnFirstRunStep.PressX)
        assertFalse(run.practice(JarnFirstRunGesture.Back).practiced)
        assertTrue(run.practice(JarnFirstRunGesture.Context).practiced)
    }

    @Test
    fun `practice tabs stay in range`() {
        val run = JarnFirstRunState(JarnFirstRunStep.PressTabs)
        assertEquals(0, run.practice(JarnFirstRunGesture.Tabs, tabDelta = -1).practiceTab)
        val last = (1..5).fold(run) { acc, _ -> acc.practice(JarnFirstRunGesture.Tabs, tabDelta = 1) }
        assertEquals(JARN_FIRST_RUN_PRACTICE_TABS - 1, last.practiceTab)
    }

    @Test
    fun `resumes at the saved step after the process dies`() {
        val progress = FakeProgress()
        JarnFirstRunController(progress).apply {
            next(twoScreens = true)
            next(twoScreens = true)
        }
        assertEquals(JarnFirstRunStep.Media.name, progress.savedStep)
        assertEquals(JarnFirstRunStep.Media, JarnFirstRunController(progress).state?.step)
    }

    @Test
    fun `an unknown saved step starts over`() {
        val controller = JarnFirstRunController(FakeProgress(savedStep = "Removed"))
        assertEquals(JarnFirstRunStep.Welcome, controller.state?.step)
    }

    @Test
    fun `finishing shows it only once`() {
        val progress = FakeProgress(savedStep = JarnFirstRunStep.Done.name)
        val controller = JarnFirstRunController(progress)
        controller.next(twoScreens = true)
        assertNull(controller.state)
        assertTrue(progress.isDone)
        assertNull(JarnFirstRunController(progress).state)
    }

    @Test
    fun `replay never changes saved progress`() {
        val progress = FakeProgress(isDone = true)
        val controller = JarnFirstRunController(progress)
        controller.replay()
        assertNotNull(controller.state)
        controller.next(twoScreens = true)
        assertNull(progress.savedStep)
        assertNull(JarnFirstRunController(progress).state)
    }

    @Test
    fun `a restored finished tour closes a first run but not a replay`() {
        val first = JarnFirstRunController(FakeProgress())
        first.onDoneElsewhere()
        assertNull(first.state)
        val replay = JarnFirstRunController(FakeProgress(isDone = true)).apply { replay() }
        replay.onDoneElsewhere()
        assertNotNull(replay.state)
    }

    @Test
    fun `art detour uncovers settings only`() {
        val controller = JarnFirstRunController(FakeProgress(savedStep = JarnFirstRunStep.Media.name))
        controller.startArtDetour()
        assertFalse(controller.coversShell(StageRoute.Settings))
        assertTrue(controller.coversShell(StageRoute.Library))
        controller.endArtDetour()
        assertTrue(controller.coversShell(StageRoute.Settings))
        assertEquals(JarnFirstRunStep.Media, controller.state?.step)
    }

    @Test
    fun `es-de is reported as winning when both sources have media`() {
        val summary = jarnMediaSummary(
            detected = ArtSource.EsDe,
            selected = ArtSource.EsDe,
            statuses = listOf(status(ArtSource.EsDe, 10), status(ArtSource.RetroHrai, 5)),
        )
        assertEquals(JarnMediaSummary.Detected(ArtSource.EsDe, wonOverOther = true), summary)
    }

    @Test
    fun `a chosen source is never described as detected`() {
        val summary = jarnMediaSummary(
            detected = null,
            selected = ArtSource.RetroHrai,
            statuses = listOf(status(ArtSource.EsDe, 10), status(ArtSource.RetroHrai, 5)),
        )
        assertEquals(JarnMediaSummary.InUse(ArtSource.RetroHrai), summary)
    }

    @Test
    fun `media elsewhere and missing are told apart`() {
        assertEquals(
            JarnMediaSummary.Elsewhere(ArtSource.Scraped),
            jarnMediaSummary(null, ArtSource.EsDe, listOf(status(ArtSource.EsDe, 0), status(ArtSource.Scraped, 3))),
        )
        assertEquals(
            JarnMediaSummary.Missing,
            jarnMediaSummary(null, ArtSource.EsDe, listOf(status(ArtSource.EsDe, 0))),
        )
        assertEquals(JarnMediaSummary.Checking, jarnMediaSummary(null, ArtSource.EsDe, null))
    }

    private fun status(source: ArtSource, count: Int) = ArtSourceStatus(source, count > 0, count)
}
