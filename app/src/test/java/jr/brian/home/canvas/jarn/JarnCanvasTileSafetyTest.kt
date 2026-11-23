package jr.brian.home.canvas.jarn

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class JarnCanvasTileSafetyTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun layoutWith(vararg items: CanvasItem) = CanvasLayout(items = items.toList())

    @Test
    fun `a widget id shared with another tile or widget page stays allocated`() {
        val other = layoutWith(CanvasItem.WidgetItem("w2", 42))
        assertTrue(isWidgetIdReferenced(listOf(other), emptyList(), 42))
        assertTrue(isWidgetIdReferenced(emptyList(), listOf(42), 42))
        assertFalse(isWidgetIdReferenced(listOf(other), listOf(7), 8))
    }

    @Test
    fun `only files inside the canvas photo folder can be deleted`() {
        val dir = temp.newFolder("canvas_photos")
        val inside = File(dir, "photo_1.png").apply { writeText("x") }
        val outside = temp.newFile("secret.png")
        assertEquals(inside.canonicalPath, ownedPhotoFile(dir, "file://${inside.path}")?.canonicalPath)
        assertNull(ownedPhotoFile(dir, "file://${outside.path}"))
        assertNull(ownedPhotoFile(dir, "file://${dir.path}/../secret.png"))
        assertNull(ownedPhotoFile(dir, "content://media/1"))
        assertNull(ownedPhotoFile(dir, null))
    }

    @Test
    fun `closing the last lease hands widget updates back to Classic exactly once`() {
        val classic = mockk<AppWidgetHost>(relaxed = true)
        val jarn = mockk<AppWidgetHost>(relaxed = true)
        val manager = mockk<AppWidgetManager>(relaxed = true)
        JarnWidgetHostHandoff.classicStarted(classic)
        val first = JarnWidgetHostHandoff.acquire(manager) { jarn }
        val second = JarnWidgetHostHandoff.acquire(manager) { error("host is shared") }
        first.close()
        first.close()
        verify(exactly = 0) { jarn.stopListening() }
        second.close()
        verifyOrder {
            jarn.startListening()
            jarn.stopListening()
            classic.startListening()
        }
        JarnWidgetHostHandoff.classicStopped(classic)
    }
}
