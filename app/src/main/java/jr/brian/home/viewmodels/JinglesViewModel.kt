package jr.brian.home.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.home.data.JinglesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    val indexNames: StateFlow<Map<String, String>> = jinglesManager.indexNames

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
