package jr.brian.jarnfe.rss

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnRssModelTest {

    private fun item(id: String, feed: String = "https://a.test/feed", audio: String = "") = JarnRssItem(
        id = id,
        feedUrl = feed,
        title = id,
        link = "",
        description = "",
        dateLabel = "",
        imageUrl = "",
        videoUrl = "",
        audioUrl = audio,
    )

    private fun feed(url: String, minutes: Int = 30, last: Long = 0L) = JarnRssFeed(url, url, minutes, last)

    @Test
    fun `a bare host gets https`() {
        assertEquals("https://example.com/feed", jarnRssFeedUrl("  example.com/feed "))
    }

    @Test
    fun `a host with a port is not mistaken for a scheme`() {
        assertEquals("https://localhost:8080/rss", jarnRssFeedUrl("localhost:8080/rss"))
    }

    @Test
    fun `an uppercase scheme is normalized so the repository keeps it`() {
        assertEquals("https://example.com/a", jarnRssFeedUrl("HTTPS://example.com/a"))
    }

    @Test
    fun `non web feed urls are rejected`() {
        listOf(
            "file:///data/data/app/secret.xml",
            "file:/etc/hosts",
            "javascript:alert(1)",
            "content://media/1",
            "ftp://example.com/feed",
            "intent://x#Intent;end",
            "https://user:pass@example.com/feed",
            "https://exa mple.com",
            "",
        ).forEach { assertNull(it, jarnRssFeedUrl(it)) }
    }

    @Test
    fun `untrusted media urls only pass as plain web urls`() {
        assertEquals("https://cdn.test/a b.mp3", jarnRssSafeUrl("https://cdn.test/a b.mp3"))
        listOf(
            "file:///sdcard/a.mp3",
            "content://x/1",
            "javascript:alert(1)",
            "intent://x",
            "https://good.test@evil.test/a",
            "https://a\\@evil.test/",
        ).forEach { assertEquals(it, "", jarnRssSafeUrl(it)) }
    }

    @Test
    fun `article text drops scripts styles and tags`() {
        val text = jarnRssPlainText(
            "<p>Hello <b>there</b></p><script>steal()</script><style>p{}</style><SCRIPT>open",
        )
        assertTrue(text, text.startsWith("Hello there"))
        assertFalse(text, "steal" in text || "p{}" in text || "open" in text)
    }

    @Test
    fun `manual feeds are never due`() {
        assertFalse(feed("u", minutes = 0).isDue(Long.MAX_VALUE))
    }

    @Test
    fun `a feed is due once its interval passes`() {
        val feed = feed("u", minutes = 15, last = 1_000L)
        assertFalse(feed.isDue(1_000L + 15 * 60_000L - 1))
        assertTrue(feed.isDue(1_000L + 15 * 60_000L))
    }

    @Test
    fun `moving a feed stops at both ends`() {
        val urls = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), jarnRssMoved(urls, "b", -1))
        assertEquals(urls, jarnRssMoved(urls, "a", -1))
        assertEquals(urls, jarnRssMoved(urls, "c", 1))
        assertEquals(urls, jarnRssMoved(urls, "missing", 1))
    }

    @Test
    fun `the same article id in two feeds stays as two rows`() {
        val items = listOf(item("x", "https://a.test"), item("x", "https://b.test"))
        assertEquals(2, jarnRssVisibleItems(items, JarnRssFilter.All, emptyList()).size)
    }

    @Test
    fun `filters pick audio, one feed, and history order`() {
        val items = listOf(item("1", audio = "https://a.test/1.mp3"), item("2", "https://b.test"), item("3"))
        assertEquals(listOf("1"), jarnRssVisibleItems(items, JarnRssFilter.Audio, emptyList()).map { it.id })
        assertEquals(
            listOf("2"),
            jarnRssVisibleItems(items, JarnRssFilter.Feed("https://b.test"), emptyList()).map { it.id },
        )
        assertEquals(
            listOf("3", "1"),
            jarnRssVisibleItems(items, JarnRssFilter.History, listOf("3", "gone", "1")).map { it.id },
        )
    }

    @Test
    fun `a filter on a removed feed falls back to all`() {
        val filter = JarnRssFilter.Feed("gone")
        assertEquals(JarnRssFilter.All, jarnRssValidFilter(filter, listOf(feed("kept"))))
        assertEquals(filter, jarnRssValidFilter(filter, listOf(feed("gone"))))
    }

    @Test
    fun `stepping the reader stays inside the queue`() {
        val queue = listOf(item("1"), item("2"))
        val reader = JarnRssReader(queue[0], queue, watchingVideo = true)
        assertEquals("2", reader.stepped(1).item.id)
        assertFalse(reader.stepped(1).watchingVideo)
        assertEquals(reader, reader.stepped(-1))
    }
}
