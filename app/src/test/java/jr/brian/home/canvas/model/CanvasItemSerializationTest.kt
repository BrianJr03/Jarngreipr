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
            romKey = "snes/super-mario.smc"
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CanvasItem>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `WidgetItem round trips through JSON`() {
        val original: CanvasItem = CanvasItem.WidgetItem(
            id = "w-1",
            widgetId = 42
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CanvasItem>(encoded)
        assertEquals(original, decoded)
        assertTrue(decoded is CanvasItem.WidgetItem)
    }

    @Test
    fun `RssLauncherItem round trips through JSON`() {
        val original: CanvasItem = CanvasItem.RssLauncherItem(id = "rss-1")
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CanvasItem>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `CanvasLayout with heterogeneous items and arrangements round trips`() {
        val original = CanvasLayout(
            activeOrientation = CanvasScrollOrientation.HORIZONTAL,
            verticalColumns = 4,
            horizontalRows = 3,
            items = listOf(
                CanvasItem.AppItem("a", packageName = "com.a"),
                CanvasItem.FolderItem("f", folderId = "fld"),
                CanvasItem.RomItem("r", romKey = "sys/game"),
                CanvasItem.WidgetItem("w", widgetId = 7),
                CanvasItem.RssLauncherItem("rss")
            ),
            verticalArrangement = mapOf(
                "a" to GridRect(0, 0, 1, 1),
                "f" to GridRect(1, 0, 1, 1),
                "r" to GridRect(2, 0, 1, 1),
                "w" to GridRect(0, 1, 2, 2),
                "rss" to GridRect(3, 0, 1, 1)
            ),
            horizontalArrangement = mapOf(
                "a" to GridRect(0, 0, 1, 1),
                "f" to GridRect(0, 1, 1, 1),
                "r" to GridRect(0, 2, 1, 1),
                "w" to GridRect(1, 0, 2, 2),
                "rss" to GridRect(3, 0, 1, 1)
            )
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CanvasLayout>(encoded)
        assertEquals(original, decoded)
        assertEquals(5, decoded.items.size)
        assertEquals(5, decoded.verticalArrangement.size)
        assertEquals(5, decoded.horizontalArrangement.size)
    }

    @Test
    fun `CanvasLayout defaults are stable for empty input`() {
        val empty = CanvasLayout()
        val encoded = json.encodeToString(empty)
        val decoded = json.decodeFromString<CanvasLayout>(encoded)
        assertEquals(empty, decoded)
        assertEquals(CanvasScrollOrientation.HORIZONTAL, decoded.activeOrientation)
        assertEquals(CanvasLayout.CURRENT_VERSION, decoded.version)
        assertTrue(decoded.verticalArrangement.isEmpty())
        assertTrue(decoded.horizontalArrangement.isEmpty())
    }

    @Test
    fun `polymorphic discriminator is preserved across variants`() {
        val items: List<CanvasItem> = listOf(
            CanvasItem.AppItem("a", packageName = "com.a"),
            CanvasItem.WidgetItem("w", widgetId = 9),
            CanvasItem.RssLauncherItem("rss")
        )
        val encoded = json.encodeToString(items)
        val decoded = json.decodeFromString<List<CanvasItem>>(encoded)
        assertEquals(items, decoded)
        assertNotNull(decoded.firstOrNull { it is CanvasItem.AppItem })
        assertNotNull(decoded.firstOrNull { it is CanvasItem.WidgetItem })
        assertNotNull(decoded.firstOrNull { it is CanvasItem.RssLauncherItem })
    }

    @Test
    fun `validateInvariant throws when an item lacks a placement in either arrangement`() {
        val missingVertical = CanvasLayout(
            items = listOf(CanvasItem.AppItem("a", packageName = "com.a")),
            verticalArrangement = emptyMap(),
            horizontalArrangement = mapOf("a" to GridRect(0, 0, 1, 1))
        )
        runCatching { missingVertical.validateInvariant() }
            .onSuccess { error("expected throw for missing vertical placement") }

        val missingHorizontal = CanvasLayout(
            items = listOf(CanvasItem.AppItem("a", packageName = "com.a")),
            verticalArrangement = mapOf("a" to GridRect(0, 0, 1, 1)),
            horizontalArrangement = emptyMap()
        )
        runCatching { missingHorizontal.validateInvariant() }
            .onSuccess { error("expected throw for missing horizontal placement") }

        val orphan = CanvasLayout(
            items = emptyList(),
            verticalArrangement = mapOf("ghost" to GridRect(0, 0, 1, 1)),
            horizontalArrangement = emptyMap()
        )
        runCatching { orphan.validateInvariant() }
            .onSuccess { error("expected throw for orphan arrangement entry") }
    }

    @Test
    fun `validateInvariant passes for a fully aligned layout`() {
        val ok = CanvasLayout(
            items = listOf(
                CanvasItem.AppItem("a", packageName = "com.a"),
                CanvasItem.WidgetItem("w", widgetId = 1)
            ),
            verticalArrangement = mapOf(
                "a" to GridRect(0, 0, 1, 1),
                "w" to GridRect(0, 1, 2, 2)
            ),
            horizontalArrangement = mapOf(
                "a" to GridRect(0, 0, 1, 1),
                "w" to GridRect(1, 0, 2, 2)
            )
        )
        ok.validateInvariant()
        assertTrue(ok.satisfiesInvariant())
    }
}
