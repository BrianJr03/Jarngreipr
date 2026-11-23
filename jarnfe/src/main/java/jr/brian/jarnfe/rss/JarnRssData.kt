package jr.brian.jarnfe.rss

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.Flow

@Immutable
data class JarnRssFeed(
    val url: String,
    val title: String,
    val refreshMinutes: Int,
    val lastRefreshedAt: Long,
)

/** Every URL here has passed [jarnRssSafeUrl], so a blank value means none or rejected. */
@Immutable
data class JarnRssItem(
    val id: String,
    val feedUrl: String,
    val title: String,
    val link: String,
    val description: String,
    val dateLabel: String,
    val imageUrl: String,
    val videoUrl: String,
    val audioUrl: String,
) {
    val key: String get() = "$feedUrl|$id"
    val hasAudio: Boolean get() = audioUrl.isNotEmpty()
    val hasVideo: Boolean get() = videoUrl.isNotEmpty()
}

@Immutable
data class JarnRssPlayback(
    val title: String,
    val artist: String?,
    val imageUrl: String?,
    val isPlaying: Boolean,
    val isBuffering: Boolean,
    val isSystemMedia: Boolean,
    val itemId: String?,
    val feedUrl: String?,
    val volume: Float,
)

@Immutable
data class JarnRssProgress(val positionMs: Long = 0L, val durationMs: Long = 0L)

/** Stored by Classic, so both frontends show dates the same way. */
@Immutable
data class JarnRssDateStyle(val dayFirst: Boolean = true, val use24Hour: Boolean = true)

enum class JarnRssAddResult { Added, AddedNotLoaded, Duplicate, Failed }

/** A video player owned by the screen showing it. Call [release] when that screen leaves. */
interface JarnRssVideo {
    val view: View
    val isPlaying: Boolean
    val failed: Boolean
    val positionMs: Long
    val durationMs: Long

    fun togglePlaying()

    fun seekBy(deltaMs: Long)

    fun release()
}

/** Host services behind RSS, backed by the same feeds and player Classic uses. */
interface JarnRssData {
    val feeds: Flow<List<JarnRssFeed>>
    val items: Flow<List<JarnRssItem>>
    val historyIds: Flow<List<String>>
    val dateStyle: Flow<JarnRssDateStyle>
    val playback: Flow<JarnRssPlayback?>
    val progress: Flow<JarnRssProgress>

    suspend fun addFeed(url: String): JarnRssAddResult

    suspend fun removeFeed(url: String)

    suspend fun refreshFeed(url: String): Boolean

    suspend fun refreshAll(): Boolean

    suspend fun setRefreshInterval(url: String, minutes: Int)

    suspend fun reorderFeeds(orderedUrls: List<String>)

    /** Plays [item] with the audio entries of [queue] as the skip order. */
    suspend fun playAudio(item: JarnRssItem, queue: List<JarnRssItem>): Boolean

    fun togglePlayPause()

    fun skip(forward: Boolean)

    fun seekBy(deltaMs: Long)

    fun stepVolume(delta: Float)

    fun recordHistory(itemId: String)

    fun setDateStyle(style: JarnRssDateStyle)

    fun clearPlayPositions()

    fun openLink(host: Activity, url: String): Boolean

    fun createVideo(context: Context, url: String): JarnRssVideo
}

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnRssDataProvider {
    val jarnRssData: JarnRssData
}

internal fun Context.jarnRssDataOrNull(): JarnRssData? =
    (applicationContext as? JarnRssDataProvider)?.jarnRssData
