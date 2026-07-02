package jr.brian.home.ui.util

import jr.brian.home.model.rss.RssFeed
import jr.brian.home.model.rss.RssItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavioral coverage for [computeRssVisibleItems]. The function is the single
 * source of "what's visible in the RSS tab right now" — the canvas RSS music
 * tile uses the same call to build its playback queue, so any drift here would
 * desync the tile's prev/next from the tab's list.
 */
class RssVisibleItemsTest {

    private val feedA = RssFeed(url = "https://a", title = "Feed A")
    private val feedB = RssFeed(url = "https://b", title = "Feed B")

    private fun item(
        feedUrl: String,
        id: String,
        pubDate: String = "",
        audio: String = "audio.mp3"
    ) = RssItem(
        id = id,
        feedUrl = feedUrl,
        title = id,
        link = "",
        description = "",
        pubDate = pubDate,
        imageUrl = "",
        videoUrl = "",
        audioUrl = audio
    )

    @Test
    fun `feed mode pins currently-playing feed first then preserves feed order`() {
        val items = listOf(
            item(feedA.url, "a1"),
            item(feedA.url, "a2"),
            item(feedB.url, "b1")
        )

        val visible = computeRssVisibleItems(
            items = items,
            feeds = listOf(feedA, feedB),
            selectedFeedUrls = emptySet(),
            isAudioOnly = false,
            isMixedMode = false,
            isHistoryMode = false,
            historyItemIds = emptyList(),
            searchQuery = "",
            currentlyPlayingFeedUrl = feedB.url
        )

        assertEquals(listOf("https://b", "https://a"), visible.orderedFeeds.map { it.url })
        assertEquals(listOf("b1", "a1", "a2"), visible.flatOrdered.map { it.id })
    }

    @Test
    fun `audioOnly drops video-only items from feed mode flat order`() {
        val items = listOf(
            item(feedA.url, "a1", audio = "a1.mp3"),
            item(feedA.url, "a2", audio = ""),
            item(feedA.url, "a3", audio = "a3.mp3")
        )

        val visible = computeRssVisibleItems(
            items = items,
            feeds = listOf(feedA),
            selectedFeedUrls = emptySet(),
            isAudioOnly = true,
            isMixedMode = false,
            isHistoryMode = false,
            historyItemIds = emptyList(),
            searchQuery = "",
            currentlyPlayingFeedUrl = null
        )

        assertEquals(listOf("a1", "a3"), visible.flatOrdered.map { it.id })
    }

    @Test
    fun `mixed mode interleaves feeds then sorts by pubDate desc`() {
        val items = listOf(
            item(feedA.url, "a-old", pubDate = "2020-01-01T00:00:00Z"),
            item(feedA.url, "a-new", pubDate = "2026-01-01T00:00:00Z"),
            item(feedB.url, "b-mid", pubDate = "2023-01-01T00:00:00Z")
        )

        val visible = computeRssVisibleItems(
            items = items,
            feeds = listOf(feedA, feedB),
            selectedFeedUrls = emptySet(),
            isAudioOnly = false,
            isMixedMode = true,
            isHistoryMode = false,
            historyItemIds = emptyList(),
            searchQuery = "",
            currentlyPlayingFeedUrl = null
        )

        assertEquals(listOf("a-new", "b-mid", "a-old"), visible.flatOrdered.map { it.id })
    }

    @Test
    fun `history mode returns items in history order ignoring filters`() {
        val items = listOf(
            item(feedA.url, "a1"),
            item(feedB.url, "b1"),
            item(feedA.url, "a2")
        )

        val visible = computeRssVisibleItems(
            items = items,
            feeds = listOf(feedA, feedB),
            selectedFeedUrls = setOf(feedA.url),
            isAudioOnly = true,
            isMixedMode = true,
            isHistoryMode = true,
            historyItemIds = listOf("b1", "a2", "missing"),
            searchQuery = "ignored",
            currentlyPlayingFeedUrl = null
        )

        assertEquals(listOf("b1", "a2"), visible.flatOrdered.map { it.id })
        assertEquals(listOf("b1", "a2"), visible.historyItems.map { it.id })
    }

    @Test
    fun `selectedFeedUrls filters the visible flat list`() {
        val items = listOf(
            item(feedA.url, "a1"),
            item(feedB.url, "b1")
        )

        val visible = computeRssVisibleItems(
            items = items,
            feeds = listOf(feedA, feedB),
            selectedFeedUrls = setOf(feedB.url),
            isAudioOnly = false,
            isMixedMode = false,
            isHistoryMode = false,
            historyItemIds = emptyList(),
            searchQuery = "",
            currentlyPlayingFeedUrl = null
        )

        assertEquals(listOf("b1"), visible.flatOrdered.map { it.id })
        assertTrue(visible.orderedFeeds.all { it.url == feedB.url })
    }
}
