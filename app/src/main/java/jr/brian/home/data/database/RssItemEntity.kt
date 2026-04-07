package jr.brian.home.data.database

import androidx.room.Entity
import jr.brian.home.model.rss.RssItem

@Entity(tableName = "rss_items", primaryKeys = ["id", "feedUrl"])
data class RssItemEntity(
    val id: String,
    val feedUrl: String,
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String,
    val imageUrl: String = "",
    val videoUrl: String = "",
    val audioUrl: String = ""
) {
    fun toDomain() = RssItem(
        id = id,
        feedUrl = feedUrl,
        title = title,
        link = link,
        description = description,
        pubDate = pubDate,
        imageUrl = imageUrl,
        videoUrl = videoUrl,
        audioUrl = audioUrl
    )
}
