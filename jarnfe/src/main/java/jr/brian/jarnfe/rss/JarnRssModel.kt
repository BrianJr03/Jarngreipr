package jr.brian.jarnfe.rss

import android.text.Html
import java.net.URI

internal const val JARN_RSS_MAX_URL_LENGTH = 512
internal const val JARN_RSS_SEEK_MS = 15_000L
internal const val JARN_RSS_VOLUME_STEP = 0.1f
internal val JarnRssRefreshMinutes = listOf(0, 15, 30, 60, 120, 360, 720)

private const val MAX_ARTICLE_HTML = 200_000
private val BareScheme = Regex("^[a-z][a-z0-9+.-]*:(?!\\d)", RegexOption.IGNORE_CASE)
private val SafeWebUrl = Regex("^https?://[^\\s/?#@\\\\]+([/?#].*)?$", RegexOption.IGNORE_CASE)
private val ActiveBlocks = Regex(
    "<(script|style)\\b[^>]*>.*?(</\\1\\s*>|$)",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val ExtraBlankLines = Regex("\\n{3,}")

/** Returns a fetchable http(s) feed URL, adding https when no scheme was typed, or null. */
fun jarnRssFeedUrl(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty() || trimmed.length > JARN_RSS_MAX_URL_LENGTH) return null
    if (trimmed.any { it.isWhitespace() || it.isISOControl() }) return null
    val hasScheme = "://" in trimmed
    if (!hasScheme && BareScheme.containsMatchIn(trimmed)) return null
    val candidate = if (hasScheme) trimmed else "https://$trimmed"
    val uri = runCatching { URI(candidate) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase() ?: return null
    if (scheme != "http" && scheme != "https") return null
    if (uri.host.isNullOrBlank() || uri.userInfo != null) return null
    return scheme + candidate.substring(scheme.length)
}

/** Feed content is untrusted, so anything but a plain web URL becomes blank. */
fun jarnRssSafeUrl(raw: String): String {
    val trimmed = raw.trim()
    return if (trimmed.length <= JARN_RSS_MAX_URL_LENGTH * 4 && SafeWebUrl.matches(trimmed)) trimmed else ""
}

/** Article text with markup removed. Nothing from the feed is ever executed. */
internal fun jarnRssPlainText(html: String): String {
    if (html.isBlank()) return ""
    val withoutActive = html.take(MAX_ARTICLE_HTML).replace(ActiveBlocks, " ")
    return Html.fromHtml(withoutActive, Html.FROM_HTML_MODE_COMPACT)
        .toString()
        .replace("￼", "")
        .replace(ExtraBlankLines, "\n\n")
        .trim()
}

internal fun JarnRssFeed.isDue(nowMs: Long): Boolean =
    refreshMinutes > 0 && nowMs - lastRefreshedAt >= refreshMinutes * 60_000L

internal fun jarnRssMoved(urls: List<String>, url: String, delta: Int): List<String> {
    val from = urls.indexOf(url)
    if (from < 0) return urls
    val to = (from + delta).coerceIn(urls.indices)
    if (to == from) return urls
    return urls.toMutableList().apply { add(to, removeAt(from)) }
}

internal sealed interface JarnRssFilter {
    data object All : JarnRssFilter
    data object Audio : JarnRssFilter
    data object History : JarnRssFilter
    data class Feed(val url: String) : JarnRssFilter
}

internal fun jarnRssVisibleItems(
    items: List<JarnRssItem>,
    filter: JarnRssFilter,
    historyIds: List<String>,
): List<JarnRssItem> = when (filter) {
    JarnRssFilter.All -> items
    JarnRssFilter.Audio -> items.filter { it.hasAudio }
    is JarnRssFilter.Feed -> items.filter { it.feedUrl == filter.url }
    JarnRssFilter.History -> {
        val byId = items.groupBy { it.id }
        historyIds.mapNotNull { byId[it]?.firstOrNull() }
    }
}.distinctBy { it.key }

/** A filter on a removed feed falls back to All, so the list is never stuck empty. */
internal fun jarnRssValidFilter(filter: JarnRssFilter, feeds: List<JarnRssFeed>): JarnRssFilter =
    if (filter is JarnRssFilter.Feed && feeds.none { it.url == filter.url }) JarnRssFilter.All else filter

internal fun jarnRssTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
