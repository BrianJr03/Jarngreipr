package jr.brian.home.jarn

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.frontend.library.art.JarnLibraryData
import jr.brian.home.MainActivity
import jr.brian.home.data.BgMusicManager
import jr.brian.home.data.JinglesManager
import jr.brian.home.data.NowPlayingManager
import jr.brian.home.model.jingles.EntryResult
import jr.brian.home.model.jingles.JinglesResult
import jr.brian.home.viewmodels.JingleEntryViewModel
import jr.brian.jarnfe.audio.JarnAudioData
import jr.brian.jarnfe.audio.JarnJingleDownload
import jr.brian.jarnfe.audio.JarnJingleDownloadResult
import jr.brian.jarnfe.audio.JarnJingleFolder
import jr.brian.jarnfe.audio.JarnJingleFolderResult
import jr.brian.jarnfe.audio.JarnJinglePackDraft
import jr.brian.jarnfe.audio.JarnJingleRepo
import jr.brian.jarnfe.audio.JarnJingleSearchResult
import jr.brian.jarnfe.audio.JarnJingleSettings
import jr.brian.jarnfe.audio.JarnMusicSettings
import jr.brian.jarnfe.audio.JarnMusicSource
import jr.brian.jarnfe.audio.jarnJingleGameKey
import jr.brian.jarnfe.audio.jarnJingleIndexSafe
import jr.brian.jarnfe.audio.jarnJingleRepoSlug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private const val JINGLES_PREFS = "jingles_prefs"
private const val KEY_REPOS = "jingle_repos"
private const val KEY_LOCAL_FOLDERS = "jingle_local_folders"
private const val KEY_DOWNLOADED_REPOS = "jingle_downloaded_repos"
private const val KEY_ENABLED = "jingles_enabled"
private const val KEY_PACK_PARENT = "jingles_pack_parent_uri"

/**
 * Drives the engines Classic uses. While Classic's home is on screen it owns the music, so holds
 * and resumes from a Jarn FE window still closing are ignored.
 */
@Singleton
class AppJarnAudioData @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val bgMusic: Provider<BgMusicManager>,
    private val jinglesManager: Provider<JinglesManager>,
    private val nowPlaying: Provider<NowPlayingManager>,
    private val libraryData: Provider<JarnLibraryData>,
) : JarnAudioData {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(JINGLES_PREFS, Context.MODE_PRIVATE)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _download = MutableStateFlow<JarnJingleDownload?>(null)
    private var downloadJob: Job? = null

    override val download: StateFlow<JarnJingleDownload?> = _download.asStateFlow()

    override val music: JarnMusicSettings
        get() {
            val manager = bgMusic.get()
            return JarnMusicSettings(
                source = if (manager.mode == BgMusicManager.Mode.FOLDER) JarnMusicSource.Folder else JarnMusicSource.File,
                folderName = manager.folderUri?.displayName(),
                fileName = manager.singleFileUri?.displayName(),
                volume = manager.vol,
                muted = manager.muted,
            )
        }

    override val jingles: Flow<JarnJingleSettings>
        get() {
            val manager = jinglesManager.get()
            val enabled = prefValue { prefs.getBoolean(KEY_ENABLED, true) }
            val levels = combine(manager.isMuted, manager.volume, ::Pair)
            val matching = combine(manager.regexPriority, manager.isNormalizationEnabled, ::Pair)
            return combine(enabled, levels, matching) { on, (muted, volume), (regex, normalize) ->
                JarnJingleSettings(on, muted, volume, regex, normalize)
            }
        }

    override val repos: Flow<List<JarnJingleRepo>>
        get() {
            val manager = jinglesManager.get()
            val slugs = prefValue { readSet(KEY_REPOS) to readSet(KEY_DOWNLOADED_REPOS) }
            return combine(slugs, manager.indexNames, manager.indexCounts) { (added, saved), names, counts ->
                added.sorted().map { JarnJingleRepo(it, names[it], counts[it], it in saved) }
            }
        }

    override val folders: Flow<List<JarnJingleFolder>>
        get() {
            val manager = jinglesManager.get()
            val paths = prefValue { readSet(KEY_LOCAL_FOLDERS) }
            return combine(paths, manager.indexNames, manager.indexCounts) { folders, names, counts ->
                folders.sorted().map { path ->
                    val name = names[path]?.takeIf(String::isNotBlank) ?: path.substringAfterLast('/')
                    JarnJingleFolder(path, name, counts[path])
                }
            }
        }

    override val otherMediaPlaying: Flow<Boolean>
        get() = nowPlaying.get().nowPlaying.map { it?.isPlaying == true }.distinctUntilChanged()

    override fun allowPlayback() {
        if (!MainActivity.isRunning) bgMusic.get().releaseHold()
    }

    override fun holdPlayback() {
        if (!MainActivity.isRunning) bgMusic.get().holdPlayback()
    }

    override suspend fun playJingle(gameKey: String): Boolean {
        if (MainActivity.isRunning) return false
        jinglesManager.get().onGameSelected(gameKey)
        return true
    }

    override fun stopJingle() {
        if (!MainActivity.isRunning) jinglesManager.get().stop()
    }

    override fun setMusicFolder(uri: String) = bgMusic.get().setFolderUri(uri.toUri())

    override fun setMusicFile(uri: String) = bgMusic.get().setSingleFileUri(uri.toUri())

    override fun setMusicVolume(volume: Float) = bgMusic.get().setVolume(volume)

    override fun setMusicMuted(muted: Boolean) = bgMusic.get().setMuted(muted)

    override fun setJinglesEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_ENABLED, enabled) }
        if (!enabled) jinglesManager.get().stop()
    }

    override fun setJinglesMuted(muted: Boolean) = jinglesManager.get().setMuted(muted)

    override fun setJingleVolume(volume: Float) = jinglesManager.get().setVolume(volume)

    override fun setRegexPriority(enabled: Boolean) = jinglesManager.get().setRegexPriority(enabled)

    override fun setNormalize(enabled: Boolean) = jinglesManager.get().setNormalizationEnabled(enabled)

    override fun addRepo(slug: String): Boolean {
        val valid = jarnJingleRepoSlug(slug) ?: return false
        val current = readSet(KEY_REPOS)
        if (current.any { it.equals(valid, ignoreCase = true) }) return false
        prefs.edit { putStringSet(KEY_REPOS, current + valid) }
        jinglesManager.get().refreshLocalIndices()
        return true
    }

    override fun removeRepo(slug: String) {
        if (_download.value?.slug == slug) cancelDownload()
        prefs.edit { putStringSet(KEY_REPOS, readSet(KEY_REPOS) - slug) }
        jinglesManager.get().removeDownloadedRepo(slug)
        jinglesManager.get().refreshLocalIndices()
    }

    override suspend fun searchRepos(query: String): List<JarnJingleSearchResult>? =
        when (val result = jinglesManager.get().searchRepos(query.trim().lowercase())) {
            is JinglesResult.Success -> result.value.mapNotNull { repo ->
                jarnJingleRepoSlug(repo.fullName)?.let { JarnJingleSearchResult(it, repo.description, repo.stars) }
            }
            is JinglesResult.Failure -> null
        }

    override fun startDownload(slug: String, onDone: (JarnJingleDownloadResult) -> Unit) {
        if (_download.value != null) return
        val valid = jarnJingleRepoSlug(slug)
        if (valid == null) {
            onDone(JarnJingleDownloadResult.Rejected)
            return
        }
        _download.value = JarnJingleDownload(valid, 0f)
        downloadJob = scope.launch {
            var rejected = false
            try {
                val result = jinglesManager.get().downloadRepo(
                    repoSlug = valid,
                    acceptIndex = { index ->
                        jarnJingleIndexSafe(index.entries.map { it.file }).also { rejected = !it }
                    },
                    onProgress = { _download.value = JarnJingleDownload(valid, it) },
                )
                onDone(
                    when {
                        rejected -> JarnJingleDownloadResult.Rejected
                        result is JinglesResult.Success -> JarnJingleDownloadResult.Done
                        else -> JarnJingleDownloadResult.Failed
                    },
                )
            } finally {
                _download.value = null
                downloadJob = null
            }
        }
    }

    override fun cancelDownload() {
        downloadJob?.cancel()
    }

    override fun addFolder(uri: String): JarnJingleFolderResult {
        val parsed = uri.toUri()
        val folder = DocumentFile.fromTreeUri(context, parsed)
        if (folder?.findFile("index.json") == null || folder.findFile("jingles")?.isDirectory != true) {
            return JarnJingleFolderResult.Invalid
        }
        val granted = runCatching {
            context.contentResolver.takePersistableUriPermission(
                parsed,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }.isSuccess
        if (!granted) return JarnJingleFolderResult.Invalid
        val current = readSet(KEY_LOCAL_FOLDERS)
        if (uri in current) return JarnJingleFolderResult.Duplicate
        prefs.edit { putStringSet(KEY_LOCAL_FOLDERS, current + uri) }
        jinglesManager.get().refreshLocalIndices()
        return JarnJingleFolderResult.Added
    }

    override fun removeFolder(path: String) {
        prefs.edit { putStringSet(KEY_LOCAL_FOLDERS, readSet(KEY_LOCAL_FOLDERS) - path) }
        jinglesManager.get().refreshLocalIndices()
    }

    override fun refreshFolders() = jinglesManager.get().refreshLocalIndices()

    override fun packParent(): String? {
        val saved = prefs.getString(KEY_PACK_PARENT, null) ?: return null
        return saved.takeIf {
            context.contentResolver.persistedUriPermissions.any { grant ->
                grant.uri.toString() == saved && grant.isReadPermission && grant.isWritePermission
            }
        }
    }

    override fun setPackParent(uri: String) {
        val granted = runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }.isSuccess
        if (granted) prefs.edit { putString(KEY_PACK_PARENT, uri) }
    }

    override suspend fun savePackEntry(parentUri: String, draft: JarnJinglePackDraft): Boolean {
        val editor = JingleEntryViewModel(context, jinglesManager.get())
        editor.onPackNameChange(draft.packName)
        draft.existingPath?.let { editor.onExistingPackSelected(draft.packName, it) }
        editor.onGameNameChange(draft.gameName)
        editor.onPlatformChange(draft.platform)
        editor.onFileNameChange(draft.fileName)
        editor.onMp3Selected(draft.audioUri.toUri())
        editor.addJingle(parentUri.toUri(), createPack = true)
        val finished = editor.uiState.first { !it.isProcessing && it.result != null }
        return finished.result is EntryResult.Success
    }

    override suspend fun gameKey(libraryId: String): String? {
        val game = libraryData.get().findGame(libraryId) ?: return null
        return jarnJingleGameKey(game.systemId, game.absolutePath ?: game.relativePath)
    }

    private fun String.displayName(): String? =
        toUri().lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')?.takeIf(String::isNotBlank)

    private fun readSet(key: String): Set<String> = prefs.getStringSet(key, emptySet()).orEmpty().toSet()

    private fun <T> prefValue(read: () -> T): Flow<T> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(read())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()
}
