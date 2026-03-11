package jr.brian.home.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.home.data.JinglesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JinglesViewModel @Inject constructor(
    private val jinglesManager: JinglesManager
) : ViewModel() {
    private val _downloadedRepos = MutableStateFlow(jinglesManager.getDownloadedRepos())
    val downloadedRepos: StateFlow<Set<String>> = _downloadedRepos.asStateFlow()

    private val _downloadingRepo = MutableStateFlow<String?>(null)
    val downloadingRepo: StateFlow<String?> = _downloadingRepo.asStateFlow()

    private val _isRefreshingFolders = MutableStateFlow(false)
    val isRefreshingFolders: StateFlow<Boolean> = _isRefreshingFolders.asStateFlow()

    val indexNames: StateFlow<Map<String, String>> = jinglesManager.indexNames

    fun refreshLocalFolders() {
        if (_isRefreshingFolders.value) return
        _isRefreshingFolders.value = true
        jinglesManager.refreshLocalIndices()
        viewModelScope.launch {
            delay(1_000)
            _isRefreshingFolders.value = false
        }
    }

    fun downloadRepo(
        repoSlug: String,
        onComplete: (success: Boolean) -> Unit = {}
    ) {
        if (_downloadingRepo.value != null) return
        _downloadingRepo.value = repoSlug
        viewModelScope.launch {
            val success = jinglesManager.downloadRepo(repoSlug)
            _downloadedRepos.value = jinglesManager.getDownloadedRepos()
            _downloadingRepo.value = null
            onComplete(success)
        }
    }
}
