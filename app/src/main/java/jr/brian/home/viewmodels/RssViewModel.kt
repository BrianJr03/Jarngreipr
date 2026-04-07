package jr.brian.home.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.home.data.RssRepository
import jr.brian.home.model.rss.RssItem
import jr.brian.home.model.state.RssUIState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RssViewModel @Inject constructor(
    private val rssRepository: RssRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RssUIState())
    val uiState = _uiState.asStateFlow()

    private val _selectedFeedUrl = MutableStateFlow<String?>(null)

    init {
        observeFeeds()
        startAutoRefreshTicker()
    }

    private fun observeFeeds() {
        viewModelScope.launch {
            combine(
                rssRepository.feeds,
                rssRepository.allItems,
                _selectedFeedUrl
            ) { feeds, allItems, selectedUrl ->
                val filteredItems = if (selectedUrl == null) {
                    allItems
                } else {
                    allItems.filter { it.feedUrl == selectedUrl }
                }
                Triple(feeds, filteredItems, selectedUrl)
            }.collect { (feeds, items, selectedUrl) ->
                _uiState.value = _uiState.value.copy(
                    feeds = feeds,
                    items = items,
                    selectedFeedUrl = selectedUrl
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

    fun selectFeed(feedUrl: String?) {
        _selectedFeedUrl.value = feedUrl
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
            if (_selectedFeedUrl.value == url) _selectedFeedUrl.value = null
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
