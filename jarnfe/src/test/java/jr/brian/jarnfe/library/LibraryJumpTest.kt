package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w1280dp-h720dp")
class LibraryJumpTest {
    @get:Rule
    val rule = createComposeRule()

    private val ids = (0 until 300).map { "game$it" }
    private var focused by mutableStateOf("game2")
    private var refocus by mutableIntStateOf(0)
    private var jump by mutableStateOf<LibraryJump?>(null)
    private var landed: String? = null

    private fun land(id: String) {
        landed = id
        jump = null
        focused = id
        refocus++
    }

    private fun showGrid() = rule.setContent {
        JarnTheme {
            LibraryFocusGrid(
                items = ids,
                itemKey = { it },
                focusedKey = focused,
                onFocused = { focused = it },
                geometry = LibraryGridGeometry(80.dp),
                refocusSignal = refocus,
                jump = jump,
                onLanded = ::land,
            ) { id, modifier -> Box(modifier.size(80.dp).testTag(id).focusable()) }
        }
    }

    private fun showShelf() = rule.setContent {
        JarnTheme {
            LibraryShelf(
                items = ids,
                kind = LibraryShelfKind.Games,
                content = LibraryShelfContent(
                    itemKey = { it },
                    itemTitle = { it },
                    onOpen = {},
                    itemModifier = { Modifier.testTag(it) },
                    art = {},
                ),
                focusedKey = focused,
                onFocused = { focused = it },
                refocusSignal = refocus,
                jump = jump,
                onLanded = ::land,
            )
        }
    }

    private fun jumpTo(gameId: String, spin: Boolean) {
        rule.waitForIdle()
        rule.runOnIdle { jump = LibraryJump("snes", gameId, spin, id = 1) }
        rule.mainClock.advanceTimeBy(JUMP_RUN_MS)
        rule.waitForIdle()
    }

    /** A spin's steps wait on delays, which no frame drives once the shelf draws no wash. */
    private companion object {
        const val JUMP_RUN_MS = 2_000L
    }

    private fun assertLandedOn(gameId: String) {
        assertEquals(gameId, landed)
        rule.onNodeWithTag(gameId).assertIsDisplayed().assertIsFocused()
        assertEquals(gameId, focused)
    }

    @Test
    fun `a grid spin lands on the pre-picked game`() {
        showGrid()
        jumpTo("game240", spin = true)
        assertLandedOn("game240")
    }

    @Test
    fun `a grid spin to a nearby game still lands on it`() {
        showGrid()
        jumpTo("game5", spin = true)
        assertLandedOn("game5")
    }

    @Test
    fun `a grid jump lands on the game`() {
        showGrid()
        jumpTo("game150", spin = false)
        assertLandedOn("game150")
    }

    @Test
    fun `a carousel spin lands on the pre-picked game`() {
        showShelf()
        jumpTo("game77", spin = true)
        assertLandedOn("game77")
    }

    @Test
    fun `a carousel jump lands on the game`() {
        showShelf()
        jumpTo("game9", spin = false)
        assertLandedOn("game9")
    }
}
