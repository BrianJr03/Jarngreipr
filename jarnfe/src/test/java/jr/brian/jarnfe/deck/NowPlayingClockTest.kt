package jr.brian.jarnfe.deck

import jr.brian.frontend.look.JarnPlayingDim
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.settings.next
import jr.brian.jarnfe.shell.JarnDeviceStatus
import jr.brian.jarnfe.shell.isBatteryLow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class NowPlayingClockTest {
    private val minute = 60_000L

    @Test
    fun `session time reads as hours and minutes`() {
        assertEquals("00:00", nowPlayingSessionText(59_999L))
        assertEquals("00:42", nowPlayingSessionText(42 * minute))
        assertEquals("01:05", nowPlayingSessionText(65 * minute))
        assertEquals("00:00", nowPlayingSessionText(-5L))
    }

    @Test
    fun `the session clock ticks on its own minute`() {
        assertEquals(minute, nextMinuteDelayMs(anchorMs = 10_000L, nowMs = 10_000L))
        assertEquals(30_000L, nextMinuteDelayMs(anchorMs = 10_000L, nowMs = 40_000L))
        assertEquals(1L, nextMinuteDelayMs(anchorMs = 0L, nowMs = minute - 1))
    }

    @Test
    fun `the page dims after the chosen delay and never with Never`() {
        assertFalse(nowPlayingDimmed(JarnPlayingDim.ThirtySeconds, lastWakeMs = 0L, nowMs = 29_999L))
        assertTrue(nowPlayingDimmed(JarnPlayingDim.ThirtySeconds, lastWakeMs = 0L, nowMs = 30_000L))
        assertTrue(nowPlayingDimmed(JarnPlayingDim.TenSeconds, lastWakeMs = 0L, nowMs = 10_000L))
        assertFalse(nowPlayingDimmed(JarnPlayingDim.Never, lastWakeMs = 0L, nowMs = Long.MAX_VALUE / 2))
    }

    @Test
    fun `a long dim starts after half an hour`() {
        assertFalse(nowPlayingLongDim(null, 99 * minute))
        assertFalse(nowPlayingLongDim(0L, 30 * minute - 1))
        assertTrue(nowPlayingLongDim(0L, 30 * minute))
    }

    @Test
    fun `the drift stays inside the burn in square and moves every step`() {
        val steps = (0L until 20L).map(::nowPlayingDrift)
        assertTrue(steps.all { (x, y) -> x in 0..NOW_PLAYING_DRIFT_MAX_DP && y in 0..NOW_PLAYING_DRIFT_MAX_DP })
        assertTrue(steps.zipWithNext().all { (a, b) -> a != b })
        assertTrue(steps.zipWithNext().all { (a, b) -> abs(a.first - b.first) + abs(a.second - b.second) <= 8 })
    }

    @Test
    fun `the dim choice cycles through every option`() {
        val seen = generateSequence(JarnPlayingDim.ThirtySeconds) { it.next() }.take(JarnPlayingDim.entries.size).toSet()
        assertEquals(JarnPlayingDim.entries.toSet(), seen)
    }

    @Test
    fun `the launched game is found by path, absolute or relative`() {
        val library = JarnLibraryGame(id = "3:gc:Melee.rvz", systemId = "gc", title = "Super Smash Bros. Melee")
        assertTrue(matchesNowPlaying(library, NowPlayingGame("gc", "/storage/ROMs/gc/Melee.rvz", "x")))
        assertTrue(matchesNowPlaying(library, NowPlayingGame("gc", "Melee.rvz", "x")))
        assertFalse(matchesNowPlaying(library, NowPlayingGame("gc", "/storage/ROMs/gc/OtherMelee.rvz", "x")))
        assertFalse(matchesNowPlaying(library, NowPlayingGame("wii", "Melee.rvz", "x")))
    }

    @Test
    fun `the battery reads low at 15 percent or less, unless charging`() {
        assertTrue(JarnDeviceStatus("1:00", 15).isBatteryLow)
        assertFalse(JarnDeviceStatus("1:00", 16).isBatteryLow)
        assertFalse(JarnDeviceStatus("1:00", 5, isCharging = true).isBatteryLow)
        assertFalse(JarnDeviceStatus("1:00", null).isBatteryLow)
    }

    @Test
    fun `the cover keeps 30 percent of its light at full dim and all of it awake`() {
        assertEquals(0.3f, 1f - nowPlayingCoverShade(1f), 0.001f)
        assertEquals(0f, nowPlayingCoverShade(0f), 0f)
        assertEquals(nowPlayingCoverShade(1f), nowPlayingCoverShade(2f), 0f)
    }
}
