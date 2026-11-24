package jr.brian.jarnfe.apps

import android.app.Application
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.dock.JarnDockApp
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.ProvideJarnInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Switching the Deck away from Apps and back must not show a frame without the tiles or their icons. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = JarnAppsTabReturnTest.HostApp::class)
class JarnAppsTabReturnTest {
    @get:Rule
    val rule = createComposeRule()

    class HostApp : Application(), JarnAppsDataProvider {
        override val jarnAppsData: JarnAppsData = FakeAppsData
    }

    private object FakeAppsData : JarnAppsData, JarnAppOptionsData by FakeJarnAppOptions() {
        override val snapshot: Flow<JarnAppsSnapshot> = MutableStateFlow(
            JarnAppsSnapshot(
                apps = (10..17).map { JarnDockApp("pkg.$it", "App $it", CountingDrawable()) },
                pinned = emptyList(),
                hidden = emptySet(),
            ),
        )
    }

    private class CountingDrawable : Drawable() {
        var draws = 0

        override fun draw(canvas: android.graphics.Canvas) {
            draws++
        }

        override fun setAlpha(alpha: Int) = Unit

        override fun setColorFilter(colorFilter: ColorFilter?) = Unit

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    private var showApps by mutableStateOf(true)
    private val visible = (10..13).map { "pkg.$it" }

    private fun switchTo(apps: Boolean) {
        rule.runOnUiThread {
            showApps = apps
            Snapshot.sendApplyNotifications()
        }
        rule.mainClock.advanceTimeByFrame()
    }

    @Test
    fun tilesAreThereOnTheFirstFrameBackOnApps() {
        rule.setContent {
            JarnTheme {
                ProvideJarnInput(JarnInputRouter(), JarnScreen.Deck) {
                    Box(Modifier.size(320.dp, 320.dp)) {
                        if (showApps) JarnAppsRoute()
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("pkg.10").assertExists()

        rule.mainClock.autoAdvance = false
        repeat(2) {
            switchTo(apps = false)
            rule.onNodeWithTag("pkg.10").assertDoesNotExist()
            switchTo(apps = true)
            visible.forEach { rule.onNodeWithTag(it).assertExists() }
        }
    }

    @Test
    fun drawableIconPaintsWithoutLoading() {
        val drawable = CountingDrawable()
        val painter = JarnDrawablePainter(drawable)
        val canvas = Canvas(android.graphics.Canvas())
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(32f, 32f)) {
            with(painter) { draw(size) }
        }
        assertEquals(1, drawable.draws)
        assertEquals(32, drawable.bounds.width())
    }
}
