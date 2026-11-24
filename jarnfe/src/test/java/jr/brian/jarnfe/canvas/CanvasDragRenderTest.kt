package jr.brian.jarnfe.canvas

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Proves the drag path's cost without a device. Real frame timing still needs the Thor. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CanvasDragRenderTest {
    @get:Rule
    val rule = createComposeRule()

    private val rects = mapOf(
        "a" to GridRect(0, 0, 1, 1),
        "b" to GridRect(1, 0, 1, 1),
        "far" to GridRect(3, 2, 1, 1),
    )
    private val layout = CanvasLayout(
        activeOrientation = CanvasScrollOrientation.HORIZONTAL,
        horizontalRows = 4,
        items = rects.keys.map { CanvasItem.AppItem(it, "pkg.$it") },
        verticalArrangement = rects,
        horizontalArrangement = rects,
    )
    private val page = JarnCanvasPage(0, JarnCanvasPageKind.Canvas, layout)
    private val board = CanvasBoardGeometry(cellPx = 90f, spacingPx = 10f, paddingPx = 20f)
    private val editor = CanvasEditor().apply { showPage(0) }
    private val driver = CanvasTouchDriver(editor).also {
        it.geometry = board
        it.actions = CanvasTouchActions(
            enabled = true,
            layout = layout,
            minSpans = { 1 to 1 },
            openMenu = {},
            commit = { _, _ -> },
            onGrabbed = {},
            haptic = {},
            showBlocked = {},
        )
    }
    private val gesture = CanvasTouchGesture(1f)
    private val press = Offset(60f, 60f)

    private fun send(effect: CanvasTouchEffect, point: Offset = press) =
        driver.handle(gesture, effect, "a", press, point)

    private fun moveTo(point: Offset) = send(gesture.moved(point.x, point.y), point)

    private fun startDrag() {
        send(gesture.down(driver.startFor("a"), press.x, press.y, 0L))
        send(gesture.timeReached(360L))
        moveTo(press + Offset(20f, 0f))
    }

    @Test
    fun `finger moves inside one cell never run the solver but still move the tile`() {
        startDrag()
        val grab = editor.grab
        repeat(40) { step ->
            val point = press + Offset(20f + step, step / 2f)
            moveTo(point)
            assertSame(grab, editor.grab)
            assertEquals(point.x - press.x + board.originOf(rects.getValue("a")).x, driver.visual.x, 0.01f)
        }
    }

    @Test
    fun `crossing a cell runs the solver once`() {
        startDrag()
        val before = editor.grab
        moveTo(press + Offset(80f, 0f))
        val after = editor.grab
        assertNotSame(before, after)
        moveTo(press + Offset(82f, 0f))
        assertSame(after, editor.grab)
    }

    @Test
    fun `a finger left behind by a controller takeover stops moving the tile`() {
        startDrag()
        val x = driver.visual.x
        editor.takeOver(CanvasGrabDriver.Controller, "a")
        moveTo(press + Offset(300f, 0f))
        assertEquals(x, driver.visual.x, 0f)
        assertNull(driver.heldId)
    }

    @Test
    fun `only the tiles that moved recompose on a cell change, and none on a finger move`() {
        val counts = mutableMapOf<String, Int>()
        rule.setContent {
            JarnTheme {
                CanvasGrid(
                    page = page,
                    editor = editor,
                    driver = driver,
                    boardModifier = Modifier,
                    onBoardGeometry = {},
                    focusRequesterFor = { requesters.getValue(it) },
                    onFocused = {},
                    onCellSize = {},
                    onVisibleCells = {},
                    onTileClick = { _, _ -> },
                ) { item ->
                    SideEffect { counts[item.id] = (counts[item.id] ?: 0) + 1 }
                    Box(Modifier.size(1.dp))
                }
            }
        }
        rule.runOnIdle { startDrag() }
        rule.waitForIdle()
        val settled = counts.toMap()
        rule.runOnIdle { repeat(30) { moveTo(press + Offset(21f + it, 0f)) } }
        rule.waitForIdle()
        assertEquals(settled, counts.toMap())

        rule.runOnIdle { moveTo(press + Offset(90f, 0f)) }
        rule.waitForIdle()
        assertEquals(settled["far"], counts["far"])
        assertEquals(CanvasGrabDriver.Touch, editor.grab?.driver)
        assertEquals(1, editor.grab?.rect?.col)
    }

    private fun showGrid(reducedMotion: Boolean) {
        rule.setContent {
            JarnTheme(reducedMotion = reducedMotion) {
                CanvasGrid(
                    page = page,
                    editor = editor,
                    driver = driver,
                    boardModifier = Modifier,
                    onBoardGeometry = {},
                    focusRequesterFor = { requesters.getValue(it) },
                    onFocused = {},
                    onCellSize = {},
                    onVisibleCells = {},
                    onTileClick = { _, _ -> },
                ) { Box(Modifier.size(1.dp)) }
            }
        }
    }

    @Test
    fun `a dropped tile settles over 160 ms and then leaves the float`() {
        rule.mainClock.autoAdvance = false
        showGrid(reducedMotion = false)
        rule.runOnIdle {
            startDrag()
            moveTo(press + Offset(90f, 0f))
            send(gesture.up())
        }
        rule.mainClock.advanceTimeBy(64L)
        assertEquals("a", driver.visual.tileId)
        rule.mainClock.advanceTimeBy(260L)
        rule.runOnIdle { assertNull(driver.visual.tileId) }
    }

    @Test
    fun `reduced motion drops the tile without a settle`() {
        rule.mainClock.autoAdvance = false
        showGrid(reducedMotion = true)
        rule.runOnIdle {
            startDrag()
            moveTo(press + Offset(90f, 0f))
            send(gesture.up())
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle { assertNull(driver.visual.tileId) }
    }

    @Test
    fun `settle eases position and lift to the cell`() {
        val visual = CanvasDragVisual()
        visual.hold("a", 100f, 40f)
        visual.follow(200f, 80f)
        visual.beginSettle()
        visual.settleStep(0.5f, 100f, 40f)
        assertEquals(150f, visual.x, 0.01f)
        assertEquals(60f, visual.y, 0.01f)
        assertEquals(0.5f, visual.lift, 0.01f)
        visual.settleStep(1f, 100f, 40f)
        assertEquals(100f, visual.x, 0.01f)
        visual.finish()
        assertNull(visual.tileId)
    }

    @Test
    fun `reduced motion never scales the lifted tile`() {
        assertEquals(1f, canvasLiftScale(1f, reduced = true), 0f)
        assertEquals(CANVAS_LIFT_SCALE, canvasLiftScale(1f, reduced = false), 0f)
    }

    @Test
    fun `a glide starts from where the tile was drawn and eases to its new cell`() {
        val glide = CanvasSlotGlide()
        glide.place(100, 0, glides = true)
        assertEquals(0f, glide.dx, 0f)
        glide.place(200, 0, glides = true)
        assertEquals(-100f, glide.dx, 0f)
        glide.step(0.5f)
        assertEquals(-50f, glide.dx, 0f)
        glide.place(300, 0, glides = true)
        assertEquals(-150f, glide.dx, 0f)
        glide.place(400, 0, glides = false)
        assertEquals(0f, glide.dx, 0f)
        assertTrue(!glide.isGliding)
    }

    private val requesters = rects.keys.associateWith { FocusRequester() }
}
