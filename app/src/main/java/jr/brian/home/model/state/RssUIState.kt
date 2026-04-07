package jr.brian.home.model.state

import jr.brian.home.model.rss.RssFeed
import jr.brian.home.model.rss.RssItem

data class RssUIState(
    val feeds: List<RssFeed> = emptyList(),
    val items: List<RssItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedFeedUrl: String? = null
)
