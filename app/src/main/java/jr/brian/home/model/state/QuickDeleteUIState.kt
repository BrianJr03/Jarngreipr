package jr.brian.home.model.state

import jr.brian.home.model.FileExtensionInfo

data class QuickDeleteUIState(
    val folderPaths: List<String> = emptyList(),
    val fileExtensions: List<FileExtensionInfo> = emptyList(),
    val selectedExtensions: Set<String> = emptySet(),
    val isScanning: Boolean = false,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val scanError: String? = null,
    val deleteResult: DeleteResult? = null
)

sealed class DeleteResult {
    data class Success(val deletedCount: Int) : DeleteResult()
    data class Failure(val message: String) : DeleteResult()
}
