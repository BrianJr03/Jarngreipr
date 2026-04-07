package jr.brian.home.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RssFeedDao {

    @Query("SELECT * FROM rss_feeds")
    fun getAllFeeds(): Flow<List<RssFeedEntity>>

    @Query("SELECT * FROM rss_feeds")
    suspend fun getAllFeedsSnapshot(): List<RssFeedEntity>

    @Query("SELECT * FROM rss_feeds WHERE url = :url")
    suspend fun getFeed(url: String): RssFeedEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFeed(feed: RssFeedEntity)

    @Query("DELETE FROM rss_feeds WHERE url = :url")
    suspend fun deleteFeed(url: String)

    @Query("SELECT * FROM rss_items ORDER BY pubDate DESC")
    fun getAllItems(): Flow<List<RssItemEntity>>

    @Query("SELECT * FROM rss_items WHERE feedUrl = :feedUrl ORDER BY pubDate DESC")
    fun getItemsForFeed(feedUrl: String): Flow<List<RssItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<RssItemEntity>)

    @Query("DELETE FROM rss_items WHERE feedUrl = :feedUrl")
    suspend fun deleteItemsForFeed(feedUrl: String)

    @Query("UPDATE rss_feeds SET lastRefreshedAt = :timestamp, title = :title, description = :description WHERE url = :url")
    suspend fun updateFeedAfterRefresh(url: String, title: String, description: String, timestamp: Long)

    @Query("UPDATE rss_feeds SET refreshIntervalMinutes = :minutes WHERE url = :url")
    suspend fun updateRefreshInterval(url: String, minutes: Int)
}
