package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.look.JarnCoverGlow
import jr.brian.jarnfe.backdrop.JARN_ADAPTIVE_SAMPLE_MS
import jr.brian.jarnfe.backdrop.JarnAdaptiveContent
import jr.brian.jarnfe.backdrop.JarnAdaptiveTextState
import jr.brian.jarnfe.backdrop.JarnBackdropPixels
import jr.brian.jarnfe.backdrop.JarnBackdropReader
import jr.brian.jarnfe.backdrop.JarnTextTone
import jr.brian.jarnfe.backdrop.LocalJarnAdaptiveText
import jr.brian.jarnfe.backdrop.rememberJarnAdaptiveText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LibraryCoverGlowTest {
    @get:Rule
    val rule = createComposeRule()

    /** A backdrop that is white everywhere and counts how often it is read. */
    private class WhiteBackdrop : JarnBackdropReader {
        var captures = 0

        override fun local(windowRect: Rect): Rect = windowRect

        override suspend fun capture(density: Density, direction: LayoutDirection): JarnBackdropPixels {
            captures++
            return JarnBackdropPixels(1, 1, Float.MAX_VALUE, intArrayOf(Color.White.toArgb()))
        }
    }

    private val content = LibraryShelfContent<String>(
        itemKey = { it },
        itemTitle = { it },
        onOpen = {},
        art = {},
    )

    @Test
    fun `off gives shelves no glow to hand over and full screen does`() {
        assertNull(libraryShelfGlowFor(JarnCoverGlow.Off))
        assertNotNull(libraryShelfGlowFor(JarnCoverGlow.Full))
    }

    @Test
    fun `with the glow off a shelf draws no wash and the header is never sampled`() {
        val backdrop = WhiteBackdrop()
        lateinit var state: JarnAdaptiveTextState
        rule.setContent {
            JarnTheme {
                val glow = remember { libraryShelfGlowFor(JarnCoverGlow.Off) }
                state = header(backdrop, glow)
                CompositionLocalProvider(LocalLibraryShelfGlow provides glow) { LibraryShelfWash("mario", content) }
            }
        }
        rule.mainClock.advanceTimeBy(JARN_ADAPTIVE_SAMPLE_MS * 4)
        rule.waitForIdle()
        assertEquals(0, backdrop.captures)
        assertEquals(JarnTextTone.Theme, state.tone)
    }

    @Test
    fun `full screen shows the centered art, samples, and adapts the header`() {
        val backdrop = WhiteBackdrop()
        val glow = libraryShelfGlowFor(JarnCoverGlow.Full)!!
        lateinit var state: JarnAdaptiveTextState
        rule.setContent {
            JarnTheme {
                state = header(backdrop, glow)
                CompositionLocalProvider(LocalLibraryShelfGlow provides glow) { LibraryShelfWash("mario", content) }
            }
        }
        rule.mainClock.advanceTimeBy(JARN_ADAPTIVE_SAMPLE_MS * 4)
        rule.waitForIdle()
        assertTrue(glow.isShowing)
        assertTrue(backdrop.captures > 0)
        assertEquals(JarnTextTone.Inverse, state.tone)
    }

    @Test
    fun `turning the glow off live clears the art and stops sampling`() {
        val backdrop = WhiteBackdrop()
        var choice by mutableStateOf(JarnCoverGlow.Full)
        lateinit var state: JarnAdaptiveTextState
        rule.setContent {
            JarnTheme {
                val glow = remember(choice) { libraryShelfGlowFor(choice) }
                state = header(backdrop, glow)
                CompositionLocalProvider(LocalLibraryShelfGlow provides glow) { LibraryShelfWash("mario", content) }
            }
        }
        rule.mainClock.advanceTimeBy(JARN_ADAPTIVE_SAMPLE_MS * 2)
        rule.waitForIdle()
        assertEquals(JarnTextTone.Inverse, state.tone)

        choice = JarnCoverGlow.Off
        rule.waitForIdle()
        val stoppedAt = backdrop.captures
        rule.mainClock.advanceTimeBy(JARN_ADAPTIVE_SAMPLE_MS * 4)
        rule.waitForIdle()
        assertEquals(stoppedAt, backdrop.captures)
        assertEquals(JarnTextTone.Theme, state.tone)
    }

    @androidx.compose.runtime.Composable
    private fun header(backdrop: JarnBackdropReader, glow: LibraryShelfGlow?): JarnAdaptiveTextState {
        val adaptive = rememberJarnAdaptiveText(
            sampler = backdrop,
            isNeeded = remember(glow) { derivedStateOf { glow?.isShowing == true } },
            canSample = remember { mutableStateOf(true) },
        )
        CompositionLocalProvider(LocalJarnAdaptiveText provides adaptive) {
            JarnAdaptiveContent(Modifier.size(40.dp)) {
                JarnText("JARN", JarnTheme.baseType.label, JarnTheme.colors.textHigh)
            }
        }
        return adaptive
    }
}
