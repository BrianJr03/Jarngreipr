package jr.brian.home.ui.util

import android.text.Html
import jr.brian.home.model.rss.RssFeed
import jr.brian.home.model.rss.RssItem

internal val pubDateFormats = listOf(
    "EEE, dd MMM yyyy HH:mm:ss Z",
    "EEE, dd MMM yyyy HH:mm:ss z",
    "yyyy-MM-dd'T'HH:mm:ssZ",
    "yyyy-MM-dd'T'HH:mm:ss'Z'",
    "yyyy-MM-dd'T'HH:mm:ssz",
    "dd MMM yyyy HH:mm:ss Z"
)

internal fun parsePubDateMillis(raw: String): Long {
    if (raw.isBlank()) return 0L
    for (fmt in pubDateFormats) {
        runCatching {
            val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.ENGLISH)
            sdf.isLenient = false
            return sdf.parse(raw.trim())!!.time
        }
    }
    return 0L
}

internal fun formatPubDate(raw: String, useDMY: Boolean, use24Hour: Boolean): String {
    if (raw.isBlank()) return ""
    val datePattern = if (useDMY) "d/M/yyyy" else "M/d/yyyy"
    val timePattern = if (use24Hour) "HH:mm" else "h:mm a"
    val output =
        java.text.SimpleDateFormat("$datePattern @ $timePattern", java.util.Locale.getDefault())
    for (fmt in pubDateFormats) {
        runCatching {
            val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.ENGLISH)
            sdf.isLenient = false
            return output.format(sdf.parse(raw.trim())!!)
        }
    }
    return raw.take(30).trimEnd()
}

internal fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0L)
    val hours = totalSec / 3600
    val min = (totalSec % 3600) / 60
    val sec = totalSec % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, min, sec)
    else "%d:%02d".format(min, sec)
}

internal fun stripHtml(html: String): String {
    if (html.isBlank()) return ""
    return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
}

/**
 * Snapshot of the RSS items the user can currently see in the RSS tab, in the
 * order the tab renders them. Computed by [computeRssVisibleItems] so the
 * tab's list and the canvas RSS music tile's queue can never drift apart.
 *
 * [flatOrdered] is the queue: a single flat sequence matching the tab's render
 * order for the active mode. The other fields preserve the per-mode groupings
 * the tab still uses for sticky headers / per-feed sections.
 */
internal data class RssVisibleItems(
    val flatOrdered: List<RssItem>,
    val itemsByFeed: Map<String, List<RssItem>>,
    val mixedItems: List<RssItem>,
    val historyItems: List<RssItem>,
    val orderedFeeds: List<RssFeed>
)

/**
 * Compute the items the RSS tab currently shows for the given inputs, plus the
 * flat ordered queue the canvas RSS music tile uses for prev/next.
 *
 * - History mode short-circuits filtering: it just maps [historyItemIds] back
 *   to items by id, matching the tab's history list.
 * - Mixed mode applies selected-feed + audio-only + search filters, interleaves
 *   round-robin across feeds, then sorts by pubDate descending.
 * - Feed mode applies the same filters per feed, then flattens in feed order
 *   with the currently-playing feed pinned first.
 */
internal fun computeRssVisibleItems(
    items: List<RssItem>,
    feeds: List<RssFeed>,
    selectedFeedUrls: Set<String>,
    isAudioOnly: Boolean,
    isMixedMode: Boolean,
    isHistoryMode: Boolean,
    historyItemIds: List<String>,
    searchQuery: String,
    currentlyPlayingFeedUrl: String?
): RssVisibleItems {
    val itemsByFeed = items.groupBy { it.feedUrl }

    val feedFiltered = if (selectedFeedUrls.isEmpty()) itemsByFeed
    else itemsByFeed.filterKeys { it in selectedFeedUrls }

    val audioFiltered = if (!isAudioOnly) feedFiltered
    else feedFiltered.mapValues { (_, feedItems) ->
        feedItems.filter { it.audioUrl.isNotEmpty() }
    }.filterValues { it.isNotEmpty() }

    val filteredItemsByFeed = if (searchQuery.isBlank()) audioFiltered
    else audioFiltered.mapValues { (_, feedItems) ->
        feedItems.filter { item ->
            item.title.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true)
        }
    }.filterValues { it.isNotEmpty() }

    val mixedItems = if (!isMixedMode) emptyList()
    else {
        val lists = filteredItemsByFeed.values.toList()
        val maxSize = lists.maxOfOrNull { it.size } ?: 0
        buildList {
            for (i in 0 until maxSize) {
                lists.forEach { feedItems -> feedItems.getOrNull(i)?.let { add(it) } }
            }
        }.sortedByDescending { parsePubDateMillis(it.pubDate) }
    }

    val orderedFeeds = run {
        val base = if (selectedFeedUrls.isEmpty()) feeds
        else feeds.filter { it.url in selectedFeedUrls }
        val withItems = base.filter { it.url in filteredItemsByFeed }
        val playingUrl = currentlyPlayingFeedUrl
        val playing = playingUrl?.let { url -> withItems.find { it.url == url } }
        if (playing == null) withItems
        else listOf(playing) + withItems.filter { it.url != playingUrl }
    }

    val historyItems = if (!isHistoryMode) emptyList()
    else {
        val itemMap = items.associateBy { it.id }
        historyItemIds.mapNotNull { itemMap[it] }
    }

    val flatOrdered = when {
        isHistoryMode -> historyItems
        isMixedMode -> mixedItems
        else -> orderedFeeds.flatMap { feed -> filteredItemsByFeed[feed.url].orEmpty() }
    }

    return RssVisibleItems(
        flatOrdered = flatOrdered,
        itemsByFeed = filteredItemsByFeed,
        mixedItems = mixedItems,
        historyItems = historyItems,
        orderedFeeds = orderedFeeds
    )
}
