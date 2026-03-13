package jr.brian.home.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.home.data.JinglesManager
import jr.brian.home.model.GitHubRepoResult
import jr.brian.home.model.jingles.JinglesResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JinglesSearchUiState(
    val isLoading: Boolean = false,
    val results: List<GitHubRepoResult> = emptyList(),
    val query: String = "",
    val isBrowseMode: Boolean = false,
    val hasSearched: Boolean = false,
    val isError: Boolean = false
)

@HiltViewModel
class JinglesViewModel @Inject constructor(
    private val jinglesManager: JinglesManager
) : ViewModel() {
    private val _downloadedRepos = MutableStateFlow(jinglesManager.getDownloadedRepos())
    val downloadedRepos: StateFlow<Set<String>> = _downloadedRepos.asStateFlow()

    private val _downloadingRepo = MutableStateFlow<String?>(null)
    val downloadingRepo: StateFlow<String?> = _downloadingRepo.asStateFlow()

    private val _downloadProgress = MutableStateFlow(-1f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _isRefreshingFolders = MutableStateFlow(false)
    val isRefreshingFolders: StateFlow<Boolean> = _isRefreshingFolders.asStateFlow()

    private val _searchState = MutableStateFlow(JinglesSearchUiState())
    val searchState: StateFlow<JinglesSearchUiState> = _searchState.asStateFlow()

    val indexNames: StateFlow<Map<String, String>> = jinglesManager.indexNames
    val indexCounts: StateFlow<Map<String, Int>> = jinglesManager.indexCounts
    val isMuted: StateFlow<Boolean> = jinglesManager.isMuted
    val volume: StateFlow<Float> = jinglesManager.volume

    fun setVolume(volume: Float) = jinglesManager.setVolume(volume)

    fun refreshLocalFolders() {
        if (_isRefreshingFolders.value) return
        _isRefreshingFolders.value = true
        jinglesManager.refreshLocalIndices()
        viewModelScope.launch {
            delay(1_000)
            _isRefreshingFolders.value = false
        }
    }

    fun browseAllJingles() {
        viewModelScope.launch {
            val query = "jingles"
            _searchState.value = JinglesSearchUiState(isLoading = true, query = query, isBrowseMode = true)
            val result = jinglesManager.searchRepos(query)
            _searchState.value = JinglesSearchUiState(
                isLoading = false,
                results = if (result is JinglesResult.Success) result.value else emptyList(),
                query = query,
                isBrowseMode = true,
                hasSearched = true,
                isError = result is JinglesResult.Failure
            )
        }
    }

    fun searchRepo(query: String) {
        if (query.isBlank()) return
        val normalizedQuery = query.trim().lowercase()
        viewModelScope.launch {
            _searchState.value = JinglesSearchUiState(isLoading = true, query = normalizedQuery, isBrowseMode = false)
            val result = jinglesManager.searchRepos(normalizedQuery)
            _searchState.value = JinglesSearchUiState(
                isLoading = false,
                results = if (result is JinglesResult.Success) result.value else emptyList(),
                query = normalizedQuery,
                isBrowseMode = false,
                hasSearched = true,
                isError = result is JinglesResult.Failure
            )
        }
    }

    fun clearSearch() {
        _searchState.value = JinglesSearchUiState()
    }

    private var downloadJob: Job? = null

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _downloadingRepo.value = null
        _downloadProgress.value = -1f
    }

    fun downloadRepo(
        repoSlug: String,
        onComplete: (success: Boolean) -> Unit = {}
    ) {
        if (_downloadingRepo.value != null) return
        _downloadingRepo.value = repoSlug
        _downloadProgress.value = 0f
        downloadJob = viewModelScope.launch {
            val success = jinglesManager.downloadRepo(repoSlug) { progress ->
                _downloadProgress.value = progress
            }
            _downloadedRepos.value = jinglesManager.getDownloadedRepos()
            _downloadingRepo.value = null
            _downloadProgress.value = -1f
            downloadJob = null
            onComplete(success is JinglesResult.Success)
        }
    }

    suspend fun fetchRepoSizeBytes(repoSlug: String): Long? {
        val result = jinglesManager.fetchJinglesSizeBytes(repoSlug)
        return if (result is JinglesResult.Success) result.value else null
    }

    fun getDownloadedSizeBytes(repoSlug: String): Long =
        jinglesManager.getDownloadedSizeBytes(repoSlug)

    fun removeDownloadedRepo(repoSlug: String) {
        jinglesManager.removeDownloadedRepo(repoSlug)
        _downloadedRepos.value = jinglesManager.getDownloadedRepos()
    }

    fun getDownloadedFileCount(repoSlug: String): Int =
        jinglesManager.getDownloadedFileCount(repoSlug)

    suspend fun getLocalFolderSizeBytes(uriString: String): Long =
        jinglesManager.getLocalFolderSizeBytes(uriString)
}
