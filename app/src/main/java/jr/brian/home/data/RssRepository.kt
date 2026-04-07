package jr.brian.home.data

import android.util.Xml
import jr.brian.home.data.database.RssFeedDao
import jr.brian.home.data.database.RssFeedEntity
import jr.brian.home.data.database.RssItemEntity
import jr.brian.home.model.rss.RssFeed
import jr.brian.home.model.rss.RssItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL

class RssRepository(private val rssFeedDao: RssFeedDao) {

    val feeds: Flow<List<RssFeed>> = rssFeedDao.getAllFeeds().map { list ->
        list.map { it.toDomain() }
    }

    val allItems: Flow<List<RssItem>> = rssFeedDao.getAllItems().map { list ->
        list.map { it.toDomain() }
    }

    fun itemsForFeed(feedUrl: String): Flow<List<RssItem>> =
        rssFeedDao.getItemsForFeed(feedUrl).map { list -> list.map { it.toDomain() } }

    suspend fun addFeed(url: String) {
        val trimmed = url.trim()
        val normalizedUrl = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }
        rssFeedDao.insertFeed(
            RssFeedEntity(
                url = normalizedUrl,
                title = normalizedUrl,
                description = "",
                refreshIntervalMinutes = 30,
                lastRefreshedAt = 0L
            )
        )
        refreshFeed(normalizedUrl)
    }

    suspend fun removeFeed(url: String) {
        rssFeedDao.deleteFeed(url)
        rssFeedDao.deleteItemsForFeed(url)
    }

    suspend fun setRefreshInterval(url: String, minutes: Int) {
        rssFeedDao.updateRefreshInterval(url, minutes)
    }

    suspend fun reorderFeeds(orderedUrls: List<String>) {
        orderedUrls.forEachIndexed { index, url ->
            rssFeedDao.updateSortOrder(url, index)
        }
    }

    suspend fun refreshFeed(url: String): Result<Unit> = runCatching {
        val result = fetchAndParse(url)
        rssFeedDao.updateFeedAfterRefresh(
            url = url,
            title = result.title.ifEmpty { url },
            description = result.description,
            timestamp = System.currentTimeMillis()
        )
        rssFeedDao.insertItems(result.items)
    }

    suspend fun refreshAllFeeds(): List<Pair<String, Result<Unit>>> {
        val feeds = withContext(Dispatchers.IO) { rssFeedDao.getAllFeedsSnapshot() }
        return feeds.map { feed -> feed.url to refreshFeed(feed.url) }
    }

    private suspend fun fetchAndParse(url: String): FetchResult = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "Home-Launcher-RSS/1.0")
            instanceFollowRedirects = true
        }

        try {
            val inputStream = connection.inputStream.buffered()
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, null)

            var feedTitle = ""
            var feedDescription = ""
            val items = mutableListOf<RssItemEntity>()

            var inItem = false
            var inChannel = false
            var currentTag = ""
            var currentTitle = ""
            var currentLink = ""
            var currentDescription = ""
            var currentPubDate = ""
            var currentImageUrl = ""
            var currentVideoUrl = ""
            var currentAudioUrl = ""

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tagName = parser.name ?: ""
                        // Strip namespace prefix for comparison
                        currentTag = tagName.substringAfterLast(':').ifEmpty { tagName }
                        when {
                            tagName == "channel" || tagName == "feed" -> inChannel = true
                            tagName == "item" || tagName == "entry" -> inItem = true
                            tagName == "link" && inItem -> {
                                // Atom: <link href="..." rel="alternate"/>
                                val href = parser.getAttributeValue(null, "href")
                                if (href != null) currentLink = href
                            }
                            // <enclosure url="..." type="image/jpeg" /> or type="video/mp4" or type="audio/mpeg"
                            tagName == "enclosure" && inItem -> {
                                val encUrl = parser.getAttributeValue(null, "url") ?: ""
                                val encType = parser.getAttributeValue(null, "type") ?: ""
                                when {
                                    encType.startsWith("image") && currentImageUrl.isEmpty() ->
                                        currentImageUrl = encUrl
                                    encType.startsWith("video") && currentVideoUrl.isEmpty() ->
                                        currentVideoUrl = encUrl
                                    encType.startsWith("audio") && currentAudioUrl.isEmpty() ->
                                        currentAudioUrl = encUrl
                                }
                            }
                            // <itunes:image href="..."/> per-episode artwork
                            (tagName == "itunes:image" || tagName == "image") && inItem -> {
                                val href = parser.getAttributeValue(null, "href") ?: ""
                                if (href.isNotEmpty() && currentImageUrl.isEmpty()) {
                                    currentImageUrl = href
                                }
                            }
                            // <media:content url="..." type="image/jpeg" medium="image"/>
                            (tagName == "media:content" || tagName == "content") && inItem -> {
                                val mediaUrl = parser.getAttributeValue(null, "url") ?: ""
                                val mediaType = parser.getAttributeValue(null, "type") ?: ""
                                val medium = parser.getAttributeValue(null, "medium") ?: ""
                                when {
                                    (medium == "image" || mediaType.startsWith("image")) && currentImageUrl.isEmpty() ->
                                        currentImageUrl = mediaUrl
                                    (medium == "video" || mediaType.startsWith("video")) && currentVideoUrl.isEmpty() ->
                                        currentVideoUrl = mediaUrl
                                    (medium == "audio" || mediaType.startsWith("audio")) && currentAudioUrl.isEmpty() ->
                                        currentAudioUrl = mediaUrl
                                }
                            }
                            // <media:thumbnail url="..."/>
                            (tagName == "media:thumbnail" || tagName == "thumbnail") && inItem -> {
                                val thumbUrl = parser.getAttributeValue(null, "url") ?: ""
                                if (thumbUrl.isNotEmpty() && currentImageUrl.isEmpty()) {
                                    currentImageUrl = thumbUrl
                                }
                            }
                        }
                    }

                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim() ?: ""
                        if (text.isEmpty()) {
                            eventType = parser.next()
                            continue
                        }
                        if (inItem) {
                            when (currentTag) {
                                "title" -> currentTitle = text
                                "link" -> if (currentLink.isEmpty()) currentLink = text
                                "description", "summary", "content", "encoded" ->
                                    currentDescription = text
                                "pubDate", "published", "updated", "date" ->
                                    currentPubDate = text
                            }
                        } else if (inChannel) {
                            when (currentTag) {
                                "title" -> if (feedTitle.isEmpty()) feedTitle = text
                                "description", "subtitle" ->
                                    if (feedDescription.isEmpty()) feedDescription = text
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val tag = parser.name ?: ""
                        if (tag == "item" || tag == "entry") {
                            val id = currentLink.ifEmpty { currentTitle }
                            if (id.isNotEmpty()) {
                                // If no explicit image, try extracting first <img> from description HTML
                                val resolvedImage = currentImageUrl.ifEmpty {
                                    extractFirstImageFromHtml(currentDescription)
                                }
                                items.add(
                                    RssItemEntity(
                                        id = id,
                                        feedUrl = url,
                                        title = currentTitle,
                                        link = currentLink,
                                        description = currentDescription,
                                        pubDate = currentPubDate,
                                        imageUrl = resolvedImage,
                                        videoUrl = currentVideoUrl,
                                        audioUrl = currentAudioUrl
                                    )
                                )
                            }
                            currentTitle = ""
                            currentLink = ""
                            currentDescription = ""
                            currentPubDate = ""
                            currentImageUrl = ""
                            currentVideoUrl = ""
                            currentAudioUrl = ""
                            inItem = false
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }

            FetchResult(feedTitle, feedDescription, items)
        } finally {
            connection.disconnect()
        }
    }

    private fun extractFirstImageFromHtml(html: String): String {
        if (html.isBlank()) return ""
        val regex = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        return regex.find(html)?.groupValues?.getOrNull(1) ?: ""
    }

    private data class FetchResult(
        val title: String,
        val description: String,
        val items: List<RssItemEntity>
    )
}
