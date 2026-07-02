package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NotificationManagerReorderTest {
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var manager: NotificationManager
    private val store = mutableMapOf<String, Any?>()

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences("notification_badge_prefs", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.apply() } answers { }
        every { editor.putInt(any(), any()) } answers {
            store[firstArg()] = secondArg(); editor
        }
        every { editor.putBoolean(any(), any()) } answers {
            store[firstArg()] = secondArg(); editor
        }
        every { editor.remove(any()) } answers {
            store.remove(firstArg<String>()); editor
        }
        every { prefs.getInt(any(), any()) } answers {
            store[firstArg()] as? Int ?: secondArg()
        }
        every { prefs.getBoolean(any(), any()) } answers {
            store[firstArg()] as? Boolean ?: secondArg()
        }

        manager = NotificationManager(context)
    }

    @Test
    fun `reorderPages moves shadeTabPage to its new index`() {
        manager.saveShadeTabPage(2)
        manager.reorderPages(mapOf(0 to 2, 1 to 0, 2 to 1))
        assertEquals(0, manager.shadeTabPage)
    }

    @Test
    fun `reorderPages clamps to 0 when shadeTabPage is no longer present`() {
        manager.saveShadeTabPage(3)
        // Only new index 0 maps from old 1; old 3 not in map → fall back to 0
        manager.reorderPages(mapOf(0 to 1))
        assertEquals(0, manager.shadeTabPage)
    }

    @Test
    fun `removePage resets shadeTabPage to 0 when its page is removed`() {
        manager.saveShadeTabPage(2)
        manager.removePage(2)
        assertEquals(0, manager.shadeTabPage)
    }

    @Test
    fun `removePage decrements shadeTabPage when above removed index`() {
        manager.saveShadeTabPage(3)
        manager.removePage(1)
        assertEquals(2, manager.shadeTabPage)
    }

    @Test
    fun `removePage leaves shadeTabPage unchanged when below removed index`() {
        manager.saveShadeTabPage(1)
        manager.removePage(3)
        assertEquals(1, manager.shadeTabPage)
    }
}
