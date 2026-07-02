package jr.brian.home.model.rss

data class RssFeed(
    val url: String,
    val title: String,
    val description: String = "",
    val refreshIntervalMinutes: Int = 30,
    val lastRefreshedAt: Long = 0L
)
