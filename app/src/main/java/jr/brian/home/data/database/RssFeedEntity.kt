package jr.brian.home.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import jr.brian.home.model.rss.RssFeed

@Entity(tableName = "rss_feeds")
data class RssFeedEntity(
    @PrimaryKey val url: String,
    val title: String,
    val description: String,
    val refreshIntervalMinutes: Int,
    val lastRefreshedAt: Long
) {
    fun toDomain() = RssFeed(
        url = url,
        title = title,
        description = description,
        refreshIntervalMinutes = refreshIntervalMinutes,
        lastRefreshedAt = lastRefreshedAt
    )
}
