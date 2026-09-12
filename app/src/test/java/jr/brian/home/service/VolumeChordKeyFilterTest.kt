package jr.brian.home.service

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the chord rules that make Select+Volume adjust the Thor's bottom
 * screen without breaking normal Volume behaviour or swallowing Select itself:
 *
 * - Disabled → pass-through in every case.
 * - Volume without Select → pass-through so top-screen volume + system UI is
 *   untouched.
 * - Select+Volume → consumes DOWN and UP, emits AdjustBottom on DOWN.
 * - Select DOWN/UP is never consumed (games must still see it).
 * - Auto-repeat is throttled so a long hold ramps at a controllable rate.
 * - Held state resets on Select UP, and on [VolumeChordKeyFilter.reset].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class VolumeChordKeyFilterTest {

    private var currentTime: Long = 1_000L
    private val clock: () -> Long = { currentTime }

    private fun keyEvent(
        action: Int,
        keyCode: Int,
        repeatCount: Int = 0
    ): KeyEvent = KeyEvent(
        /* downTime  */ currentTime,
        /* eventTime */ currentTime,
        /* action    */ action,
        /* code      */ keyCode,
        /* repeat    */ repeatCount
    )

    @Test
    fun `disabled flag passes everything through`() {
        val filter = VolumeChordKeyFilter(nowMs = clock)

        assertEquals(
            VolumeChordKeyFilter.Result.PassThrough,
            filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT), enabled = false)
        )
        assertEquals(
            VolumeChordKeyFilter.Result.PassThrough,
            filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP), enabled = false)
        )
        assertEquals(
            VolumeChordKeyFilter.Result.PassThrough,
            filter.onKey(keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN), enabled = false)
        )
    }

    @Test
    fun `volume without Select held falls through`() {
        val filter = VolumeChordKeyFilter(nowMs = clock)

        assertEquals(
            VolumeChordKeyFilter.Result.PassThrough,
            filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP), enabled = true)
        )
        assertEquals(
            VolumeChordKeyFilter.Result.PassThrough,
            filter.onKey(keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP), enabled = true)
        )
        assertEquals(
            VolumeChordKeyFilter.Result.PassThrough,
            filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN), enabled = true)
        )
    }

    @Test
    fun `Select itself is never consumed`() {
        val filter = VolumeChordKeyFilter(nowMs = clock)

        assertEquals(
            VolumeChordKeyFilter.Result.PassThrough,
            filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT), enabled = true)
        )
        assertEquals(
            VolumeChordKeyFilter.Result.PassThrough,
            filter.onKey(keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_SELECT), enabled = true)
        )
    }

    @Test
    fun `Select held plus Volume Up adjusts by +1 on DOWN and consumes UP`() {
        val filter = VolumeChordKeyFilter(nowMs = clock)

        filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT), enabled = true)

        val down = filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP), enabled = true)
        assertEquals(VolumeChordKeyFilter.Result.AdjustBottom(+1), down)

        val up = filter.onKey(keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP), enabled = true)
        assertEquals(VolumeChordKeyFilter.Result.Consume, up)
    }

    @Test
    fun `Select held plus Volume Down adjusts by -1`() {
        val filter = VolumeChordKeyFilter(nowMs = clock)

        filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT), enabled = true)

        val down = filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN), enabled = true)
        assertEquals(VolumeChordKeyFilter.Result.AdjustBottom(-1), down)
    }

    @Test
    fun `auto-repeat within throttle window is consumed but not applied`() {
        val filter = VolumeChordKeyFilter(minStepIntervalMs = 150L, nowMs = clock)

        currentTime = 1_000L
        filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT), enabled = true)

        currentTime = 1_010L
        val first = filter.onKey(
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, repeatCount = 0),
            enabled = true
        )
        assertEquals(VolumeChordKeyFilter.Result.AdjustBottom(+1), first)

        currentTime = 1_050L // 40ms later, inside the 150ms window
        val throttled = filter.onKey(
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, repeatCount = 1),
            enabled = true
        )
        assertEquals(VolumeChordKeyFilter.Result.Consume, throttled)
    }

    @Test
    fun `auto-repeat past the throttle window applies again`() {
        val filter = VolumeChordKeyFilter(minStepIntervalMs = 150L, nowMs = clock)

        currentTime = 1_000L
        filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT), enabled = true)

        currentTime = 1_010L
        assertEquals(
            VolumeChordKeyFilter.Result.AdjustBottom(+1),
            filter.onKey(
                keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, repeatCount = 0),
                enabled = true
            )
        )

        currentTime = 1_200L // 190ms later, past the 150ms window
        assertEquals(
            VolumeChordKeyFilter.Result.AdjustBottom(+1),
            filter.onKey(
                keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, repeatCount = 3),
                enabled = true
            )
        )
    }

    @Test
    fun `state resets after Select UP so a subsequent Volume alone passes through`() {
        val filter = VolumeChordKeyFilter(nowMs = clock)

        filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT), enabled = true)
        filter.onKey(keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_SELECT), enabled = true)

        assertEquals(
            VolumeChordKeyFilter.Result.PassThrough,
            filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP), enabled = true)
        )
        assertEquals(
            VolumeChordKeyFilter.Result.PassThrough,
            filter.onKey(keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP), enabled = true)
        )
    }

    @Test
    fun `disabled after Select-held clears held state`() {
        val filter = VolumeChordKeyFilter(nowMs = clock)

        filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT), enabled = true)
        // User flips the toggle off mid-hold.
        filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP), enabled = false)

        // Toggle back on — held state should be gone.
        val result = filter.onKey(
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP),
            enabled = true
        )
        assertEquals(VolumeChordKeyFilter.Result.PassThrough, result)
    }

    @Test
    fun `explicit reset clears held state`() {
        val filter = VolumeChordKeyFilter(nowMs = clock)

        filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT), enabled = true)
        filter.reset()

        assertEquals(
            VolumeChordKeyFilter.Result.PassThrough,
            filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP), enabled = true)
        )
    }

    @Test
    fun `unrelated keys are always pass-through`() {
        val filter = VolumeChordKeyFilter(nowMs = clock)

        filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT), enabled = true)

        assertEquals(
            VolumeChordKeyFilter.Result.PassThrough,
            filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A), enabled = true)
        )
    }

    @Test
    fun `default throttle interval is 150ms`() {
        assertEquals(150L, VolumeChordKeyFilter.DEFAULT_MIN_STEP_INTERVAL_MS)
    }

    @Test
    fun `select held state can be re-armed by pressing Select again`() {
        val filter = VolumeChordKeyFilter(nowMs = clock)

        filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT), enabled = true)
        filter.onKey(keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_SELECT), enabled = true)
        filter.onKey(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT), enabled = true)

        assertTrue(
            "Select held again should route Volume through the chord",
            filter.onKey(
                keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP),
                enabled = true
            ) is VolumeChordKeyFilter.Result.AdjustBottom
        )
    }
}
