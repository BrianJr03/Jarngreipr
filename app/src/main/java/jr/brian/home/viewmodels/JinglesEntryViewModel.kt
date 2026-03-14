package jr.brian.home.viewmodels

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.data.JinglesManager
import jr.brian.home.model.JingleEntry
import jr.brian.home.model.JingleIndex
import jr.brian.home.model.jingles.EntryResult
import jr.brian.home.model.jingles.JingleEntryUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

private val indexJson = Json { ignoreUnknownKeys = true; isLenient = true; prettyPrint = true }

@HiltViewModel
class JingleEntryViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val jinglesManager: JinglesManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JingleEntryUiState())
    val uiState: StateFlow<JingleEntryUiState> = _uiState.asStateFlow()

    /** Pairs of (display name, folder path/URI) for all locally registered jingle packs. */
    val localPackOptions: StateFlow<List<Pair<String, String>>> =
        jinglesManager.indexNames.map { indexNames ->
            val prefs = context.getSharedPreferences("jingles_prefs", Context.MODE_PRIVATE)
            val folders = prefs.getStringSet("jingle_local_folders", emptySet()) ?: emptySet()
            folders.map { uriString ->
                val name = indexNames[uriString] ?: uriString.substringAfterLast("/")
                name to uriString
            }.filter { (name, _) -> name.isNotBlank() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onPackNameChange(value: String) = _uiState.update { it.copy(packName = value, existingPackPath = null) }
    fun onExistingPackSelected(name: String, path: String) = _uiState.update { it.copy(packName = name, existingPackPath = path) }
    fun onGameNameChange(value: String) = _uiState.update { it.copy(gameName = value) }
    fun onFileNameChange(value: String) = _uiState.update { it.copy(fileName = value) }
    fun onPlatformChange(value: String) = _uiState.update { it.copy(platform = value) }
    fun clearResult() = _uiState.update { it.copy(result = null) }

    fun resetForm() = _uiState.update { JingleEntryUiState() }

    fun onMp3Selected(uri: Uri) {
        val displayName = resolveDisplayName(uri)
        _uiState.update { state ->
            state.copy(
                selectedMp3Uri = uri,
                selectedMp3DisplayName = displayName,
                fileName = state.fileName.ifBlank { displayName.substringBeforeLast('.') }
            )
        }
    }

    fun addJingle(localFolderUri: Uri, createPack: Boolean = false) {
        val state = _uiState.value

        val errors = buildList {
            if (createPack && state.packName.isBlank()) add("Pack name is required")
            if (state.gameName.isBlank()) add("Game name is required")
            if (state.platform.isBlank()) add("Platform is required")
            if (state.fileName.isBlank()) add("File name is required")
            if (state.selectedMp3Uri == null) add("Please select an audio file")
        }
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(result = EntryResult.Failure(errors.joinToString("\n"))) }
            return
        }

        _uiState.update { it.copy(isProcessing = true, result = null) }

        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                // ── Determine working folder ──────────────────────────────────
                val folder = if (createPack) {
                    val existing = state.existingPackPath
                    if (existing != null) {
                        // Update an existing pack folder (SAF URI or filesystem path)
                        val existingFile = if (existing.startsWith("content://")) {
                            Uri.parse(existing).toFile() ?: error("Cannot resolve path for existing pack")
                        } else {
                            File(existing)
                        }
                        check(existingFile.exists()) { "Pack folder not found: ${existingFile.path}" }
                        existingFile
                    } else {
                        // Create a new pack subfolder inside the chosen parent
                        val baseFolder = localFolderUri.toFile()
                            ?: error("Cannot resolve folder path from URI")
                        check(baseFolder.exists()) { "Folder does not exist: ${baseFolder.path}" }
                        val packFolder = File(baseFolder, state.packName.trim())
                        packFolder.mkdirs()
                        packFolder
                    }
                } else {
                    val baseFolder = localFolderUri.toFile()
                        ?: error("Cannot resolve folder path from URI")
                    check(baseFolder.exists()) { "Folder does not exist: ${baseFolder.path}" }
                    baseFolder
                }

                val platform = state.platform.trim().lowercase()
                val rawFileName = state.fileName.trim()
                val sourceExt = state.selectedMp3DisplayName
                    .substringAfterLast('.', "mp3").lowercase()
                val audioFileName = if (rawFileName.contains('.')) rawFileName
                else "$rawFileName.$sourceExt"
                val relPath = "jingles/$platform/$audioFileName"

                // ── Step 1: Copy audio into jingles/<platform>/ ───────────────
                val platformDir = File(folder, "jingles/$platform")
                platformDir.mkdirs()
                val destFile = File(platformDir, audioFileName)
                destFile.delete()

                context.contentResolver.openInputStream(state.selectedMp3Uri!!)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Could not read source audio file")

                // ── Step 2: Read existing index.json using JingleIndex format ──
                val indexFile = File(folder, "index.json")
                val existingIndex = if (indexFile.exists()) {
                    runCatching { indexJson.decodeFromString<JingleIndex>(indexFile.readText()) }
                        .getOrDefault(JingleIndex())
                } else {
                    JingleIndex()
                }

                // ── Step 3: Upsert entry (replace same game name if present) ──
                val updatedEntries = existingIndex.entries
                    .filter { !it.game.equals(state.gameName.trim(), ignoreCase = true) }
                    .plus(JingleEntry(game = state.gameName.trim(), file = relPath))

                val packDisplayName = if (createPack) state.packName.trim() else existingIndex.name.ifBlank { "My Jingles" }
                val updatedIndex = existingIndex.copy(
                    name = packDisplayName,
                    entries = updatedEntries
                )

                // ── Step 4: Write index.json back in the same format ──────────
                indexFile.writeText(indexJson.encodeToString(JingleIndex.serializer(), updatedIndex))

                // ── Step 5: Register new pack in prefs (skip for existing packs) ─
                if (createPack && state.existingPackPath == null) {
                    val prefs = context.getSharedPreferences("jingles_prefs", Context.MODE_PRIVATE)
                    val registeredFolders = prefs.getStringSet("jingle_local_folders", emptySet()) ?: emptySet()
                    val packPath = folder.absolutePath
                    if (packPath !in registeredFolders) {
                        prefs.edit { putStringSet("jingle_local_folders", registeredFolders + packPath) }
                    }
                }

                jinglesManager.refreshLocalIndices()
            }

            val entryResult = result.fold(
                onSuccess = { EntryResult.Success },
                onFailure = { EntryResult.Failure(it.message ?: "Unknown error") }
            )
            _uiState.update { it.copy(isProcessing = false, result = entryResult) }
        }
    }

    /**
     * Converts a SAF tree URI to a real [File] path using the document ID.
     * Works for primary storage ("primary:path") and SD cards ("1234-5678:path").
     * Requires MANAGE_EXTERNAL_STORAGE to be granted.
     */
    private fun Uri.toFile(): File? = try {
        val docId = DocumentsContract.getTreeDocumentId(this)
        val colon = docId.indexOf(':')
        if (colon == -1) return null
        val volume = docId.substring(0, colon)
        val relativePath = docId.substring(colon + 1)
        val root = if (volume.equals("primary", ignoreCase = true)) {
            "/storage/emulated/0"
        } else {
            "/storage/$volume"
        }
        if (relativePath.isEmpty()) File(root) else File(root, relativePath)
    } catch (_: Exception) {
        null
    }

    private fun resolveDisplayName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return cursor.getString(idx)
                }
            }
        return uri.lastPathSegment ?: "audio.mp3"
    }
}
