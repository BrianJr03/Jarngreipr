package jr.brian.home.model.jingles

import android.net.Uri

sealed class EntryResult {
    data object Success : EntryResult()
    data class Failure(val message: String) : EntryResult()
}

data class JingleEntryUiState(
    val packName: String = "",
    val existingPackPath: String? = null,
    val gameName: String = "",
    val fileName: String = "",
    val platform: String = "",
    val selectedMp3Uri: Uri? = null,
    val selectedMp3DisplayName: String = "",
    val isProcessing: Boolean = false,
    val result: EntryResult? = null,
)
