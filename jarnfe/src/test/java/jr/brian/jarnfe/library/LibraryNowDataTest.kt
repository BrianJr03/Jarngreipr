package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.animation.core.snap
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryNowDataTest {
    @get:Rule
    val rule = createComposeRule()

    private data class Item(val id: String, val revision: Int = 0)

    @Test
    fun `a rapid scroll settles once on the last item`() {
        var input by mutableStateOf(Item("a"))
        val shown = mutableListOf<String>()
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val settled = rememberSettled(input, SETTLE_MS, { it.id })
            LaunchedEffect(settled.id) { shown += settled.id }
        }
        rule.mainClock.advanceTimeByFrame()
        listOf("b", "c", "d").forEach {
            change { input = Item(it) }
            rule.mainClock.advanceTimeBy(SETTLE_MS / 3)
        }
        rule.mainClock.advanceTimeBy(SETTLE_MS * 2)
        rule.waitForIdle()
        assertEquals(listOf("a", "d"), shown)
    }

    @Test
    fun `new data for the same item passes at once`() {
        var input by mutableStateOf(Item("a"))
        var seen: Item? = null
        rule.mainClock.autoAdvance = false
        rule.setContent { seen = rememberSettled(input, SETTLE_MS, { it.id }) }
        rule.mainClock.advanceTimeByFrame()
        change { input = Item("a", revision = 1) }
        rule.mainClock.advanceTimeBy(FRAMES_MS)
        assertEquals(1, seen?.revision)
    }

    @Test
    fun `an allowed change skips the wait`() {
        var input by mutableStateOf(Item("a"))
        var seen: Item? = null
        rule.mainClock.autoAdvance = false
        rule.setContent {
            seen = rememberSettled(input, SETTLE_MS, { it.id }, skipWait = { _, next -> next.id == "open" })
        }
        rule.mainClock.advanceTimeByFrame()
        change { input = Item("open") }
        rule.mainClock.advanceTimeBy(FRAMES_MS)
        assertEquals("open", seen?.id)
    }

    @Test
    fun `held art keeps the old image until the next decodes`() = runTest(UnconfinedTestDispatcher() + InstantFrames) {
        val state = HeldArtState()
        val first = FakeBitmap()
        val second = FakeBitmap()
        state.moveTo(listOf("one"), snap()) { first }
        val pending = CompletableDeferred<ImageBitmap?>()
        val job = launch { state.moveTo(listOf("two"), snap()) { pending.await() } }
        assertEquals("one", state.shown?.path)
        pending.complete(second)
        job.join()
        assertEquals("two", state.shown?.path)
        assertNull(state.outgoing)
    }

    @Test
    fun `held art tries the next path and then settles empty`() = runTest(UnconfinedTestDispatcher() + InstantFrames) {
        val state = HeldArtState()
        val image = FakeBitmap()
        state.moveTo(listOf("bad", "good"), snap()) { path -> image.takeIf { path == "good" } }
        assertEquals("good", state.shown?.path)
        state.moveTo(listOf("bad"), snap()) { null }
        assertNull(state.shown)
        assertTrue(state.isSettled)
    }

    @Test
    fun `the same path does not decode again`() = runTest(UnconfinedTestDispatcher() + InstantFrames) {
        val state = HeldArtState()
        var decodes = 0
        val decode: suspend (String) -> ImageBitmap? = {
            decodes++
            FakeBitmap()
        }
        state.moveTo(listOf("one"), snap(), decode)
        state.moveTo(listOf("one"), snap(), decode)
        assertEquals(1, decodes)
    }

    private fun change(block: () -> Unit) = rule.runOnUiThread {
        block()
        Snapshot.sendApplyNotifications()
    }

    private object InstantFrames : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(0L)
    }

    private class FakeBitmap : ImageBitmap {
        override val colorSpace = ColorSpaces.Srgb
        override val config = ImageBitmapConfig.Argb8888
        override val hasAlpha = true
        override val height = 1
        override val width = 1
        override fun prepareToDraw() = Unit
        override fun readPixels(
            buffer: IntArray,
            startX: Int,
            startY: Int,
            width: Int,
            height: Int,
            bufferOffset: Int,
            stride: Int,
        ) = Unit
    }

    private companion object {
        const val SETTLE_MS = 150L
        const val FRAMES_MS = 48L
    }
}
