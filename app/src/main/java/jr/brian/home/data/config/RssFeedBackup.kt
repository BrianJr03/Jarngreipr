package jr.brian.home.data.config

import jr.brian.home.data.database.RssFeedDao
import jr.brian.home.data.database.RssFeedEntity
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_FEEDS = 200
private const val MAX_FEED_URL_LENGTH = 2048
private const val DEFAULT_REFRESH_MINUTES = 30
private val FeedRefreshRange = 0..(7 * 24 * 60)

@Serializable
data class RssFeedConfig(
    val url: String = "",
    val refreshIntervalMinutes: Int = DEFAULT_REFRESH_MINUTES,
)

internal fun RssFeedEntity.toConfig() = RssFeedConfig(url, refreshIntervalMinutes)

/** Only plain web feeds come back, so a backup can't point the reader at local files. */
internal fun RssFeedConfig.isRestorable(): Boolean =
    url.length <= MAX_FEED_URL_LENGTH &&
        url.none(Char::isWhitespace) &&
        (url.startsWith("https://") || url.startsWith("http://")) &&
        url.substringAfter("://").isNotEmpty()

/**
 * The feeds after a restore: backed-up feeds first in their order with their refresh time, then
 * the feeds only this device has. Nothing is removed.
 */
internal fun mergeRssFeeds(current: List<RssFeedConfig>, backup: List<RssFeedConfig>?): List<RssFeedConfig> {
    if (backup == null) return current
    val incoming = backup.filter { it.isRestorable() }
        .distinctBy { it.url }
        .take(MAX_FEEDS)
        .map { it.copy(refreshIntervalMinutes = it.refreshIntervalMinutes.coerceIn(FeedRefreshRange)) }
    val incomingUrls = incoming.map { it.url }.toSet()
    return incoming + current.filter { it.url !in incomingUrls }
}

@Singleton
class RssFeedBackup @Inject constructor(
    private val rssFeedDao: RssFeedDao,
) {
    suspend fun export(): List<RssFeedConfig> = rssFeedDao.getAllFeedsSnapshot().map { it.toConfig() }

    /** New feeds start unrefreshed, so the reader fetches them the next time it refreshes. */
    suspend fun restore(backup: List<RssFeedConfig>?) {
        if (backup == null) return
        val existing = rssFeedDao.getAllFeedsSnapshot().associateBy { it.url }
        val merged = mergeRssFeeds(existing.values.map { it.toConfig() }, backup)
        merged.forEachIndexed { index, feed ->
            if (feed.url !in existing) {
                rssFeedDao.insertFeed(
                    RssFeedEntity(
                        url = feed.url,
                        title = feed.url,
                        description = "",
                        refreshIntervalMinutes = feed.refreshIntervalMinutes,
                        lastRefreshedAt = 0L,
                        sortOrder = index,
                    )
                )
            } else {
                rssFeedDao.updateRefreshInterval(feed.url, feed.refreshIntervalMinutes)
                rssFeedDao.updateSortOrder(feed.url, index)
            }
        }
    }
}
