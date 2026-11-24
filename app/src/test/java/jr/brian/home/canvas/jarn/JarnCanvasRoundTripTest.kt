package jr.brian.home.canvas.jarn

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import jr.brian.home.canvas.data.CanvasLayoutManager
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect
import jr.brian.home.model.widget.WidgetConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** A layout saved from Jarn FE must load in Classic exactly as Classic wrote it, apart from the move. */
class JarnCanvasRoundTripTest {

    private val classicBlob = """
        {"activeOrientation":"HORIZONTAL","verticalColumns":5,"horizontalRows":7,"items":[
        {"type":"app","id":"a","packageName":"com.example.app"},
        {"type":"folder","id":"f","folderId":"folder-1"},
        {"type":"rom","id":"r","romKey":"snes/game.smc","contentScale":"FIT"},
        {"type":"widget","id":"w","widgetId":42},
        {"type":"rss_launcher","id":"l"},
        {"type":"rss_music","id":"m"},
        {"type":"esde_art","id":"e","imageType":"Marquee","contentScale":"CROP","backgroundColorArgb":4278190080,"backgroundCornerRadiusDp":8,"artType":"LOGO"},
        {"type":"photo","id":"p","imageUri":"file:///photo.png","contentScale":"FIT"},
        {"type":"last_played","id":"n","count":7,"showRandomButton":false,"contentScale":"CROP","backgroundColorArgb":4278190335,"backgroundCornerRadiusDp":16}
        ],"verticalArrangement":${arrangement(0)},"horizontalArrangement":${arrangement(1)},
        "editMode":true,"menuButtonVisible":false,"version":2}
    """.trimIndent().replace("\n", "")

    private val store = mutableMapOf<String, String?>()
    private lateinit var context: Context

    private fun arrangement(startCol: Int): String =
        listOf("a", "f", "r", "w", "l", "m", "e", "p", "n").withIndex().joinToString(
            prefix = "{",
            postfix = "}",
        ) { (i, id) -> "\"$id\":{\"col\":${startCol + i},\"row\":0,\"colSpan\":1,\"rowSpan\":1}" }

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        val prefs = mockk<SharedPreferences>(relaxed = true)
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } answers { store[firstArg()] = secondArg(); editor }
        every { editor.remove(any()) } answers { store.remove(firstArg<String>()); editor }
        every { prefs.getString(any(), any()) } answers { store[firstArg()] ?: secondArg() }
        store["layout_0"] = classicBlob
    }

    @Test
    fun `a Jarn FE save reloads in Classic with only the moved tile changed`() {
        val before = CanvasLayoutManager(context).getLayout(0)
        val moved = before.horizontalArrangement + ("a" to GridRect(3, 2, 2, 1))

        val manager = CanvasLayoutManager(context)
        manager.replaceLayout(
            0,
            mergeJarnPlacements(before, CanvasScrollOrientation.HORIZONTAL, moved),
        )
        val reloaded = CanvasLayoutManager(context).getLayout(0)

        assertEquals(GridRect(3, 2, 2, 1), reloaded.horizontalArrangement["a"])
        assertEquals(before.verticalArrangement, reloaded.verticalArrangement)
        assertEquals(before.items, reloaded.items)
        assertEquals(before.copy(horizontalArrangement = moved), reloaded)
    }

    @Test
    fun `the saved json keeps the stored field names and tile types`() {
        val manager = CanvasLayoutManager(context)
        val layout = manager.getLayout(0)
        manager.replaceLayout(
            0,
            mergeJarnPlacements(layout, CanvasScrollOrientation.HORIZONTAL, layout.horizontalArrangement),
        )

        val saved = Json.parseToJsonElement(store.getValue("layout_0")!!).jsonObject
        assertTrue(
            saved.keys.containsAll(
                listOf("verticalColumns", "horizontalRows", "items", "verticalArrangement", "horizontalArrangement"),
            ),
        )
        val types = saved.getValue("items").toString()
        listOf("app", "folder", "rom", "widget", "rss_launcher", "rss_music", "esde_art", "photo", "last_played")
            .forEach { assertTrue(it, types.contains("\"type\":\"$it\"")) }
    }

    @Test
    fun `a placement for a removed tile is never written`() {
        val layout = CanvasLayoutManager(context).getLayout(0)
        val merged = mergeJarnPlacements(
            layout,
            CanvasScrollOrientation.HORIZONTAL,
            mapOf("ghost" to GridRect(0, 0, 1, 1)),
        )
        assertEquals(layout.horizontalArrangement.keys, merged.horizontalArrangement.keys)
    }

    @Test
    fun `deleting a widget page keeps and shifts the other pages' widgets`() {
        fun widget(id: Int, page: Int) = WidgetConfig(id, "c", "p", pageIndex = page, order = id)
        val (kept, released) = widgetConfigsAfterPageDelete(
            listOf(widget(1, 0), widget(2, 1), widget(3, 2)),
            deleted = 1,
        )
        assertEquals(listOf(2), released)
        assertEquals(listOf(widget(1, 0), widget(3, 1)), kept)
    }
}
