package jr.brian.home.viewmodels

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.data.QuickDeleteManager
import jr.brian.home.model.FileExtensionInfo
import jr.brian.home.model.state.QuickDeleteUIState
import jr.brian.home.model.state.DeleteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class QuickDeleteViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val quickDeleteManager: QuickDeleteManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickDeleteUIState())
    val uiState = _uiState.asStateFlow()

    init {
        loadFolderPaths()
    }

    private fun loadFolderPaths() {
        viewModelScope.launch {
            quickDeleteManager.folderPaths.collect { paths ->
                _uiState.update { it.copy(folderPaths = paths.toList()) }
            }
        }
    }

    fun addFolderPath(uriString: String) {
        if (uriString.isBlank()) return

        viewModelScope.launch {
            quickDeleteManager.addFolderPath(uriString)
        }
    }

    fun removeFolderPath(path: String) {
        viewModelScope.launch {
            quickDeleteManager.removeFolderPath(path)
            clearScanResults()
        }
    }

    fun scanFolders() {
        val paths = _uiState.value.folderPaths
        if (paths.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isScanning = true, scanError = null) }

            try {
                val extensionMap = mutableMapOf<String, MutableList<Pair<String, Long>>>()

                paths.forEach { uriString ->
                    val uri = Uri.parse(uriString)
                    val documentFile = DocumentFile.fromTreeUri(context, uri)

                    documentFile?.let { root ->
                        scanDocumentFiles(root, extensionMap)
                    }
                }

                val fileExtensions = extensionMap.map { (extension, fileInfos) ->
                    FileExtensionInfo(
                        extension = extension,
                        fileCount = fileInfos.size,
                        totalSize = fileInfos.sumOf { it.second },
                        filePaths = fileInfos.map { it.first }
                    )
                }.sortedByDescending { it.fileCount }

                _uiState.update {
                    it.copy(
                        fileExtensions = fileExtensions,
                        isScanning = false,
                        selectedExtensions = emptySet()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanError = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    private fun scanDocumentFiles(
        directory: DocumentFile,
        extensionMap: MutableMap<String, MutableList<Pair<String, Long>>>
    ) {
        directory.listFiles().forEach { file ->
            if (file.isDirectory) {
                scanDocumentFiles(file, extensionMap)
            } else if (file.isFile) {
                val name = file.name ?: return@forEach
                val extension = name.substringAfterLast('.', "").lowercase()
                if (extension.isNotEmpty() && extension != name.lowercase()) {
                    val uri = file.uri.toString()
                    val size = file.length()
                    extensionMap.getOrPut(extension) { mutableListOf() }.add(uri to size)
                }
            }
        }
    }

    fun toggleExtensionSelection(extension: String) {
        _uiState.update { state ->
            val newSelection = if (extension in state.selectedExtensions) {
                state.selectedExtensions - extension
            } else {
                state.selectedExtensions + extension
            }
            state.copy(selectedExtensions = newSelection)
        }
    }

    fun selectAllExtensions() {
        _uiState.update { state ->
            state.copy(selectedExtensions = state.fileExtensions.map { it.extension }.toSet())
        }
    }

    fun deselectAllExtensions() {
        _uiState.update { it.copy(selectedExtensions = emptySet()) }
    }

    fun showDeleteConfirmation() {
        if (_uiState.value.selectedExtensions.isEmpty()) return
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun deleteSelectedFiles() {
        val selectedExtensions = _uiState.value.selectedExtensions
        val fileExtensions = _uiState.value.fileExtensions

        if (selectedExtensions.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isDeleting = true,
                    showDeleteConfirmation = false
                )
            }

            try {
                val filesToDelete = fileExtensions
                    .filter { it.extension in selectedExtensions }
                    .flatMap { it.filePaths }

                var deletedCount = 0
                var failedCount = 0

                filesToDelete.forEach { uriString ->
                    val uri = Uri.parse(uriString)
                    val documentFile = DocumentFile.fromSingleUri(context, uri)

                    if (documentFile?.exists() == true) {
                        if (documentFile.delete()) {
                            deletedCount++
                        } else {
                            failedCount++
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        deleteResult = if (failedCount == 0) {
                            DeleteResult.Success(deletedCount)
                        } else {
                            DeleteResult.Failure("Deleted $deletedCount files, failed to delete $failedCount files")
                        }
                    )
                }

                scanFolders()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        deleteResult = DeleteResult.Failure(e.message ?: "Unknown error occurred")
                    )
                }
            }
        }
    }

    fun clearDeleteResult() {
        _uiState.update { it.copy(deleteResult = null) }
    }

    fun clearScanError() {
        _uiState.update { it.copy(scanError = null) }
    }

    private fun clearScanResults() {
        _uiState.update {
            it.copy(
                fileExtensions = emptyList(),
                selectedExtensions = emptySet()
            )
        }
    }
}
