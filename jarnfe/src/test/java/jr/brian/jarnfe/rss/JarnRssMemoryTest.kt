package jr.brian.jarnfe.rss

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnRssMemoryTest {

    @Test
    fun `fresh memory starts at the top with nothing focused`() {
        val browser = RssBrowserState(JarnRssMemory())

        assertNull(browser.focusKey)
        assertEquals(0, browser.articleList.firstVisibleItemIndex)
        assertEquals(0, browser.railList.firstVisibleItemIndex)
    }

    @Test
    fun `a new visit resumes the focus and scroll the last visit saved`() {
        val memory = JarnRssMemory().apply {
            rail = JarnRssScroll(index = 2, offset = 5)
            articles = JarnRssScroll(index = 7, offset = 30)
        }
        val first = RssBrowserState(memory)
        first.focusKey = "item:https://a.test/feed|7"
        first.save()

        val second = RssBrowserState(memory)

        assertEquals("item:https://a.test/feed|7", second.focusKey)
        assertEquals(7, second.articleList.firstVisibleItemIndex)
        assertEquals(30, second.articleList.firstVisibleItemScrollOffset)
        assertEquals(2, second.railList.firstVisibleItemIndex)
        assertEquals(5, second.railList.firstVisibleItemScrollOffset)
    }
}
