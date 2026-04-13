package jr.brian.home.viewmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.data.NowPlayingManager
import jr.brian.home.data.RssRepository
import jr.brian.home.model.rss.RssItem
import jr.brian.home.model.state.RssUIState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.content.edit

@HiltViewModel
class RssViewModel @Inject constructor(
    private val rssRepository: RssRepository,
    private val nowPlayingManager: NowPlayingManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    val nowPlaying = nowPlayingManager.nowPlaying
    val currentlyPlayingItemId = nowPlayingManager.currentItemId
    val currentlyPlayingFeedUrl = nowPlayingManager.currentFeedUrl
    val volume = nowPlayingManager.volume
    val currentPosition = nowPlayingManager.currentPosition
    val duration = nowPlayingManager.duration
    val savedPositionCount = nowPlayingManager.savedPositionCount

    fun togglePlayPause() = nowPlayingManager.togglePlayPause()
    fun skipToNext() = nowPlayingManager.skipToNext()
    fun skipToPrevious() = nowPlayingManager.skipToPrevious()
    fun setVolume(v: Float) = nowPlayingManager.setVolume(v)
    fun seekTo(positionMs: Long) = nowPlayingManager.seekTo(positionMs)
    fun clearPlayTimes() = nowPlayingManager.clearSavedPositions()

    fun getPlaytimesFileSizeBytes(): Long {
        val file = java.io.File(
            context.applicationInfo.dataDir,
            "shared_prefs/${NowPlayingManager.PLAY_TIME_PREFS}.xml"
        )
        return if (file.exists()) file.length() else 0L
    }

    fun playAudio(item: RssItem) {
        val selectedUrls = _uiState.value.selectedFeedUrls
        val items = _uiState.value.items
        val filtered = if (selectedUrls.isEmpty()) items else items.filter { it.feedUrl in selectedUrls }
        val audioItems = filtered.filter { it.audioUrl.isNotEmpty() }
        val idx = audioItems.indexOfFirst { it.id == item.id }
        if (idx >= 0) nowPlayingManager.play(audioItems, idx)
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("rss_filter_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(RssUIState())
    val uiState = _uiState.asStateFlow()

    private val _selectedFeedUrls = MutableStateFlow(loadSelectedFeedUrls())

    private val _isMixedMode = MutableStateFlow(prefs.getBoolean(KEY_IS_MIXED_MODE, false))
    val isMixedMode = _isMixedMode.asStateFlow()

    fun setMixedMode(mixed: Boolean) {
        _isMixedMode.value = mixed
        prefs.edit { putBoolean(KEY_IS_MIXED_MODE, mixed) }
    }

    private val _isAudioOnly = MutableStateFlow(prefs.getBoolean(KEY_IS_AUDIO_ONLY, false))
    val isAudioOnly = _isAudioOnly.asStateFlow()

    fun setAudioOnly(audioOnly: Boolean) {
        _isAudioOnly.value = audioOnly
        prefs.edit { putBoolean(KEY_IS_AUDIO_ONLY, audioOnly) }
    }

    private val _isHistoryMode = MutableStateFlow(prefs.getBoolean(KEY_IS_HISTORY_MODE, false))
    val isHistoryMode = _isHistoryMode.asStateFlow()

    private val _historyItemIds = MutableStateFlow(loadHistoryItemIds())
    val historyItemIds = _historyItemIds.asStateFlow()

    fun setHistoryMode(enabled: Boolean) {
        _isHistoryMode.value = enabled
        prefs.edit { putBoolean(KEY_IS_HISTORY_MODE, enabled) }
    }

    fun recordItemClick(itemId: String) {
        val updated = (_historyItemIds.value.toMutableList().also { it.remove(itemId) })
            .apply { add(0, itemId) }
            .take(HISTORY_MAX_SIZE)
        _historyItemIds.value = updated
        prefs.edit { putString(KEY_HISTORY_ITEM_IDS, updated.joinToString(",")) }
    }

    private fun loadHistoryItemIds(): List<String> {
        val raw = prefs.getString(KEY_HISTORY_ITEM_IDS, "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
    }

    init {
        observeFeeds()
        startAutoRefreshTicker()
        _uiState.value = _uiState.value.copy(
            useDMYDateFormat = prefs.getBoolean(KEY_USE_DMY_DATE_FORMAT, true),
            use24HourClock = prefs.getBoolean(KEY_USE_24_HOUR_CLOCK, true)
        )
    }

    private fun loadSelectedFeedUrls(): Set<String> {
        return prefs.getStringSet(KEY_SELECTED_FEED_URLS, emptySet()) ?: emptySet()
    }

    private fun saveSelectedFeedUrls(urls: Set<String>) {
        prefs.edit { putStringSet(KEY_SELECTED_FEED_URLS, urls) }
    }

    fun setUseDMYDateFormat(useDMY: Boolean) {
        prefs.edit { putBoolean(KEY_USE_DMY_DATE_FORMAT, useDMY) }
        _uiState.value = _uiState.value.copy(useDMYDateFormat = useDMY)
    }

    fun setUse24HourClock(use24Hour: Boolean) {
        prefs.edit { putBoolean(KEY_USE_24_HOUR_CLOCK, use24Hour) }
        _uiState.value = _uiState.value.copy(use24HourClock = use24Hour)
    }

    private fun observeFeeds() {
        viewModelScope.launch {
            combine(
                rssRepository.feeds,
                rssRepository.allItems,
                _selectedFeedUrls
            ) { feeds, allItems, selectedUrls ->
                Triple(feeds, allItems, selectedUrls)
            }.collect { (feeds, items, selectedUrls) ->
                _uiState.value = _uiState.value.copy(
                    feeds = feeds,
                    items = items,
                    selectedFeedUrls = selectedUrls
                )
            }
        }
    }

    private fun startAutoRefreshTicker() {
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                val now = System.currentTimeMillis()
                val staleFeeds = _uiState.value.feeds.filter { feed ->
                    feed.refreshIntervalMinutes > 0 &&
                        (now - feed.lastRefreshedAt) >= feed.refreshIntervalMinutes * 60_000L
                }
                staleFeeds.forEach { feed ->
                    launch { rssRepository.refreshFeed(feed.url) }
                }
            }
        }
    }

    fun setSelectedFeedUrls(urls: Set<String>) {
        _selectedFeedUrls.value = urls
        saveSelectedFeedUrls(urls)
    }

    fun addFeed(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = runCatching { rssRepository.addFeed(url) }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun removeFeed(url: String) {
        viewModelScope.launch {
            rssRepository.removeFeed(url)
            val updated = _selectedFeedUrls.value - url
            if (updated != _selectedFeedUrls.value) {
                _selectedFeedUrls.value = updated
                saveSelectedFeedUrls(updated)
            }
        }
    }

    fun refreshFeed(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            val result = rssRepository.refreshFeed(url)
            _uiState.value = _uiState.value.copy(
                isRefreshing = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun refreshAllFeeds() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            val results = rssRepository.refreshAllFeeds()
            val firstError = results.firstNotNullOfOrNull { (_, result) ->
                result.exceptionOrNull()?.message
            }
            _uiState.value = _uiState.value.copy(
                isRefreshing = false,
                error = firstError
            )
        }
    }

    fun setRefreshInterval(url: String, minutes: Int) {
        viewModelScope.launch {
            rssRepository.setRefreshInterval(url, minutes)
        }
    }

    fun reorderFeeds(orderedUrls: List<String>) {
        viewModelScope.launch {
            rssRepository.reorderFeeds(orderedUrls)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    companion object {
        private const val KEY_SELECTED_FEED_URLS = "selected_feed_urls"
        private const val KEY_USE_DMY_DATE_FORMAT = "use_dmy_date_format"
        private const val KEY_USE_24_HOUR_CLOCK = "use_24_hour_clock"
        private const val KEY_IS_MIXED_MODE = "is_mixed_mode"
        private const val KEY_IS_AUDIO_ONLY = "is_audio_only"
        private const val KEY_IS_HISTORY_MODE = "is_history_mode"
        private const val KEY_HISTORY_ITEM_IDS = "history_item_ids"
        private const val HISTORY_MAX_SIZE = 15
    }
}
