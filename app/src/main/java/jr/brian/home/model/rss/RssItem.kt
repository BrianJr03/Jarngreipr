package jr.brian.home.model.rss

data class RssItem(
    val id: String,
    val feedUrl: String,
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String = "",
    val imageUrl: String = "",
    val videoUrl: String = "",
    val audioUrl: String = ""
)
