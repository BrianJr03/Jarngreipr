package jr.brian.home.canvas.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasItemSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `AppItem round trips through JSON`() {
        val original: CanvasItem = CanvasItem.AppItem(
            id = "a-1",
            col = 0,
            row = 1,
            packageName = "com.example.app"
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CanvasItem>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `FolderItem round trips through JSON`() {
        val original: CanvasItem = CanvasItem.FolderItem(
            id = "f-1",
            col = 2,
            row = 3,
            folderId = "folder-uuid-1"
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CanvasItem>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `RomItem round trips through JSON`() {
        val original: CanvasItem = CanvasItem.RomItem(
            id = "r-1",
            col = 1,
            row = 0,
            romKey = "snes/super-mario.smc"
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CanvasItem>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `WidgetItem round trips through JSON and preserves spans`() {
        val original: CanvasItem = CanvasItem.WidgetItem(
            id = "w-1",
            col = 0,
            row = 4,
            colSpan = 3,
            rowSpan = 2,
            widgetId = 42
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CanvasItem>(encoded)
        assertEquals(original, decoded)
        assertTrue(decoded is CanvasItem.WidgetItem)
        assertEquals(3, decoded.colSpan)
        assertEquals(2, decoded.rowSpan)
    }

    @Test
    fun `RssLauncherItem round trips through JSON`() {
        val original: CanvasItem = CanvasItem.RssLauncherItem(
            id = "rss-1",
            col = 3,
            row = 5
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CanvasItem>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `CanvasLayout with heterogeneous items round trips`() {
        val original = CanvasLayout(
            orientation = CanvasScrollOrientation.HORIZONTAL,
            columns = 4,
            rows = 3,
            items = listOf(
                CanvasItem.AppItem("a", 0, 0, packageName = "com.a"),
                CanvasItem.FolderItem("f", 1, 0, folderId = "fld"),
                CanvasItem.RomItem("r", 2, 0, romKey = "sys/game"),
                CanvasItem.WidgetItem("w", 0, 1, colSpan = 2, rowSpan = 2, widgetId = 7),
                CanvasItem.RssLauncherItem("rss", 3, 0)
            )
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CanvasLayout>(encoded)
        assertEquals(original, decoded)
        assertEquals(5, decoded.items.size)
    }

    @Test
    fun `CanvasLayout defaults are stable for empty input`() {
        val empty = CanvasLayout()
        val encoded = json.encodeToString(empty)
        val decoded = json.decodeFromString<CanvasLayout>(encoded)
        assertEquals(empty, decoded)
        assertEquals(CanvasScrollOrientation.VERTICAL, decoded.orientation)
        assertEquals(CanvasLayout.CURRENT_VERSION, decoded.version)
    }

    @Test
    fun `polymorphic discriminator is preserved across variants`() {
        val items: List<CanvasItem> = listOf(
            CanvasItem.AppItem("a", 0, 0, packageName = "com.a"),
            CanvasItem.WidgetItem("w", 1, 1, widgetId = 9),
            CanvasItem.RssLauncherItem("rss", 2, 2)
        )
        val encoded = json.encodeToString(items)
        val decoded = json.decodeFromString<List<CanvasItem>>(encoded)
        assertEquals(items, decoded)
        assertNotNull(decoded.firstOrNull { it is CanvasItem.AppItem })
        assertNotNull(decoded.firstOrNull { it is CanvasItem.WidgetItem })
        assertNotNull(decoded.firstOrNull { it is CanvasItem.RssLauncherItem })
    }
}
