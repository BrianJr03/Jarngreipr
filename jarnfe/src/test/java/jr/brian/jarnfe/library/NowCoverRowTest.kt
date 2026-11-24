package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.wallpaper.JarnWallpaperScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NowCoverRowTest {
    @get:Rule
    val rule = createComposeRule()

    private val shapes = mapOf("tall" to 3f / 4f, "square" to 1f, "wide" to 1.6f)
    private val shelf = shapes.keys.map { JarnShelfGame(JarnLibraryGame(it, "snes", it), JarnShelfGroup.Recent) }

    @Test
    fun `covers on the top screen are 20 percent bigger, still bottom aligned and in shape`() {
        rule.setContent {
            JarnTheme {
                Box(Modifier.size(1920.dp, 400.dp)) {
                    NowCoverRow(
                        shelf = shelf,
                        coverHeight = 104.dp,
                        coverModifier = { Modifier },
                        onCover = {},
                        aspectOf = { game, _ -> shapes.getValue(game.id) },
                        scale = nowCoverScale(JarnWallpaperScreen.Top, hasTwoScreens = true),
                    )
                }
            }
        }
        rule.waitForIdle()
        val bounds = shapes.keys.associateWith { rule.onNodeWithContentDescription(it).getUnclippedBoundsInRoot() }
        val bottom = bounds.getValue("tall").bottom.value
        bounds.values.forEach { card ->
            assertEquals(124.8f, (card.bottom - card.top).value, 0.5f)
            assertEquals(bottom, card.bottom.value, 0.5f)
        }
        shapes.forEach { (id, aspect) ->
            val card = bounds.getValue(id)
            assertEquals(aspect, (card.right - card.left).value / (card.bottom - card.top).value, 0.02f)
        }
    }

    @Test
    fun `only the top screen of two gets the bigger covers`() {
        assertEquals(1.2f, nowCoverScale(JarnWallpaperScreen.Top, hasTwoScreens = true), 0f)
        assertEquals(1f, nowCoverScale(JarnWallpaperScreen.Bottom, hasTwoScreens = true), 0f)
        assertEquals(1f, nowCoverScale(JarnWallpaperScreen.Top, hasTwoScreens = false), 0f)
        assertEquals(1f, nowCoverScale(null, hasTwoScreens = true), 0f)
    }

    @Test
    fun `covers of every shape share one bottom edge and keep their shape`() {
        rule.setContent {
            JarnTheme {
                Box(Modifier.size(900.dp, 300.dp)) {
                    NowCoverRow(
                        shelf = shelf,
                        coverHeight = 104.dp,
                        coverModifier = { Modifier },
                        onCover = {},
                        aspectOf = { game, _ -> shapes.getValue(game.id) },
                    )
                }
            }
        }
        rule.waitForIdle()
        val bounds = shapes.keys.associateWith { rule.onNodeWithContentDescription(it).getUnclippedBoundsInRoot() }
        val bottom = bounds.getValue("tall").bottom.value
        bounds.values.forEach { assertEquals(bottom, it.bottom.value, 0.5f) }
        shapes.forEach { (id, aspect) ->
            val card = bounds.getValue(id)
            assertEquals(aspect, (card.right - card.left).value / (card.bottom - card.top).value, 0.02f)
        }
        val ordered = shapes.keys.map(bounds::getValue)
        ordered.zipWithNext().forEach { (left, right) -> assertTrue(left.right < right.left) }
    }
}
