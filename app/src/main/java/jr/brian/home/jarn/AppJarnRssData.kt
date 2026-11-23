package jr.brian.home.jarn

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.view.View
import androidx.annotation.OptIn
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.data.NowPlayingManager
import jr.brian.home.data.RssRepository
import jr.brian.home.model.rss.RssFeed
import jr.brian.home.model.rss.RssItem
import jr.brian.home.ui.util.formatPubDate
import jr.brian.jarnfe.rss.JarnRssAddResult
import jr.brian.jarnfe.rss.JarnRssData
import jr.brian.jarnfe.rss.JarnRssDateStyle
import jr.brian.jarnfe.rss.JarnRssFeed
import jr.brian.jarnfe.rss.JarnRssItem
import jr.brian.jarnfe.rss.JarnRssPlayback
import jr.brian.jarnfe.rss.JarnRssProgress
import jr.brian.jarnfe.rss.JarnRssVideo
import jr.brian.jarnfe.rss.jarnRssSafeUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private const val RSS_PREFS = "rss_filter_prefs"
private const val KEY_DAY_FIRST = "use_dmy_date_format"
private const val KEY_24_HOUR = "use_24_hour_clock"
private const val KEY_HISTORY = "history_item_ids"
private const val HISTORY_MAX = 15

/** Reads and writes the stores Classic uses, so both frontends show the same feeds. */
@Singleton
class AppJarnRssData @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: Provider<RssRepository>,
    private val nowPlaying: Provider<NowPlayingManager>,
) : JarnRssData {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(RSS_PREFS, Context.MODE_PRIVATE)
    }

    override val feeds: Flow<List<JarnRssFeed>>
        get() = repository.get().feeds.map { list -> list.map { it.toJarn() } }

    override val dateStyle: Flow<JarnRssDateStyle>
        get() = prefValue(::readDateStyle)

    override val historyIds: Flow<List<String>>
        get() = prefValue(::readHistory)

    override val items: Flow<List<JarnRssItem>>
        get() = combine(repository.get().allItems, dateStyle) { list, style ->
            list.map { it.toJarn(style) }
        }.conflate().flowOn(Dispatchers.Default)

    override val playback: Flow<JarnRssPlayback?>
        get() {
            val manager = nowPlaying.get()
            return combine(
                manager.nowPlaying,
                manager.currentItemId,
                manager.currentFeedUrl,
                manager.volume,
            ) { info, itemId, feedUrl, volume ->
                info?.let {
                    JarnRssPlayback(
                        title = it.title,
                        artist = it.artist,
                        imageUrl = it.imageUrl?.let(::jarnRssSafeUrl)?.takeIf(String::isNotEmpty),
                        isPlaying = it.isPlaying,
                        isBuffering = it.isBuffering,
                        isSystemMedia = it.isSystemMedia,
                        itemId = itemId,
                        feedUrl = feedUrl,
                        volume = volume,
                    )
                }
            }
        }

    override val progress: Flow<JarnRssProgress>
        get() {
            val manager = nowPlaying.get()
            return combine(manager.currentPosition, manager.duration, ::JarnRssProgress)
        }

    override suspend fun addFeed(url: String): JarnRssAddResult {
        val repo = repository.get()
        if (repo.feeds.first().any { it.url == url }) return JarnRssAddResult.Duplicate
        repo.addFeed(url)
        val added = repo.feeds.first().firstOrNull { it.url == url } ?: return JarnRssAddResult.Failed
        return if (added.lastRefreshedAt > 0L) JarnRssAddResult.Added else JarnRssAddResult.AddedNotLoaded
    }

    override suspend fun removeFeed(url: String) = repository.get().removeFeed(url)

    override suspend fun refreshFeed(url: String): Boolean = repository.get().refreshFeed(url).isSuccess

    override suspend fun refreshAll(): Boolean =
        repository.get().refreshAllFeeds().all { (_, result) -> result.isSuccess }

    override suspend fun setRefreshInterval(url: String, minutes: Int) =
        repository.get().setRefreshInterval(url, minutes)

    override suspend fun reorderFeeds(orderedUrls: List<String>) =
        repository.get().reorderFeeds(orderedUrls)

    override suspend fun playAudio(item: JarnRssItem, queue: List<JarnRssItem>): Boolean {
        val stored = repository.get().allItems.first().associateBy { it.feedUrl to it.id }
        val audio = queue
            .filter { it.hasAudio }
            .mapNotNull { stored[it.feedUrl to it.id] }
            .filter { jarnRssSafeUrl(it.audioUrl).isNotEmpty() }
        val index = audio.indexOfFirst { it.id == item.id && it.feedUrl == item.feedUrl }
        if (index < 0) return false
        nowPlaying.get().play(audio, index)
        return true
    }

    override fun togglePlayPause() = nowPlaying.get().togglePlayPause()

    override fun skip(forward: Boolean) {
        val manager = nowPlaying.get()
        if (forward) manager.skipToNext() else manager.skipToPrevious()
    }

    override fun seekBy(deltaMs: Long) {
        val manager = nowPlaying.get()
        val duration = manager.duration.value
        val target = (manager.currentPosition.value + deltaMs).coerceAtLeast(0L)
        manager.seekTo(if (duration > 0L) target.coerceAtMost(duration) else target)
    }

    override fun stepVolume(delta: Float) {
        val manager = nowPlaying.get()
        manager.setVolume(manager.volume.value + delta)
    }

    override fun recordHistory(itemId: String) {
        val updated = (listOf(itemId) + (readHistory() - itemId)).take(HISTORY_MAX)
        prefs.edit { putString(KEY_HISTORY, updated.joinToString(",")) }
    }

    override fun setDateStyle(style: JarnRssDateStyle) {
        prefs.edit {
            putBoolean(KEY_DAY_FIRST, style.dayFirst)
            putBoolean(KEY_24_HOUR, style.use24Hour)
        }
    }

    override fun clearPlayPositions() = nowPlaying.get().clearSavedPositions()

    override fun openLink(host: Activity, url: String): Boolean {
        val safe = jarnRssSafeUrl(url)
        if (safe.isEmpty()) return false
        return try {
            host.startActivity(
                Intent(Intent.ACTION_VIEW, safe.toUri()).addCategory(Intent.CATEGORY_BROWSABLE),
            )
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    override fun createVideo(context: Context, url: String): JarnRssVideo =
        AppJarnRssVideo(context, jarnRssSafeUrl(url))

    private fun readDateStyle() = JarnRssDateStyle(
        dayFirst = prefs.getBoolean(KEY_DAY_FIRST, true),
        use24Hour = prefs.getBoolean(KEY_24_HOUR, true),
    )

    private fun readHistory(): List<String> =
        prefs.getString(KEY_HISTORY, "").orEmpty().split(",").filter { it.isNotBlank() }

    private fun <T> prefValue(read: () -> T): Flow<T> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(read())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    private fun RssFeed.toJarn() = JarnRssFeed(url, title, refreshIntervalMinutes, lastRefreshedAt)

    private fun RssItem.toJarn(style: JarnRssDateStyle) = JarnRssItem(
        id = id,
        feedUrl = feedUrl,
        title = title,
        link = jarnRssSafeUrl(link),
        description = description,
        dateLabel = formatPubDate(pubDate, style.dayFirst, style.use24Hour),
        imageUrl = jarnRssSafeUrl(imageUrl),
        videoUrl = jarnRssSafeUrl(videoUrl),
        audioUrl = jarnRssSafeUrl(audioUrl),
    )
}

@OptIn(UnstableApi::class)
private class AppJarnRssVideo(context: Context, url: String) : JarnRssVideo {
    private val player = ExoPlayer.Builder(context.applicationContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            /* handleAudioFocus= */ true,
        )
        .build()

    @Volatile
    override var failed: Boolean = url.isEmpty()
        private set

    override val view: View = PlayerView(context).apply {
        player = this@AppJarnRssVideo.player
        useController = false
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        setBackgroundColor(Color.BLACK)
    }

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                failed = true
            }
        })
        if (url.isNotEmpty()) {
            player.setMediaItem(MediaItem.fromUri(url))
            player.playWhenReady = true
            player.prepare()
        }
    }

    override val isPlaying: Boolean get() = player.playWhenReady
    override val positionMs: Long get() = player.currentPosition.coerceAtLeast(0L)
    override val durationMs: Long
        get() = player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L

    override fun togglePlaying() {
        player.playWhenReady = !player.playWhenReady
    }

    override fun seekBy(deltaMs: Long) {
        val target = (player.currentPosition + deltaMs).coerceAtLeast(0L)
        player.seekTo(if (durationMs > 0L) target.coerceAtMost(durationMs) else target)
    }

    override fun release() {
        (view as PlayerView).player = null
        player.release()
    }
}
