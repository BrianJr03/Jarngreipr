package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import androidx.annotation.OptIn
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.esde.model.JingleSource
import jr.brian.home.model.JingleEntry
import jr.brian.home.model.JingleIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val JINGLES_PREFS = "jingles_prefs"
private const val KEY_REPOS = "jingle_repos"
private const val KEY_LOCAL_FOLDERS = "jingle_local_folders"
private const val KEY_DOWNLOADED_REPOS = "jingle_downloaded_repos"
private const val KEY_ENABLED = "jingles_enabled"
private const val JARNGREIPR_FOLDER = "Jarngreipr Jingles"

@Singleton
class JinglesManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(JINGLES_PREFS, Context.MODE_PRIVATE)

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_REPOS || key == KEY_LOCAL_FOLDERS || key == KEY_DOWNLOADED_REPOS) {
            indicesLoaded = false
            scope.launch { loadIndices() }
        }
    }

    private val _indexNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val indexNames: StateFlow<Map<String, String>> = _indexNames

    private var player: ExoPlayer? = null
    private var currentPlayJob: Job? = null
    private var currentGameFilename: String? = null
    private var cachedIndices: List<JingleSource> = emptyList()
    private var indicesLoaded = false
    private val cachedBranch = mutableMapOf<String, String>()

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        scope.launch { loadIndices() }
        scope.launch(Dispatchers.Main) { initPlayer() }
    }

    fun onGameSelected(gameFilename: String) {
        if (!prefs.getBoolean(KEY_ENABLED, true)) return
        if (gameFilename == currentGameFilename) return
        currentGameFilename = gameFilename
        currentPlayJob?.cancel()
        currentPlayJob = scope.launch {
            withContext(Dispatchers.Main) { player?.stop() }
            if (!indicesLoaded) loadIndices()
            val (source, entry) = findMatch(gameFilename) ?: return@launch
            when {
                !source.isLocal -> {
                    val branch = cachedBranch[source.key] ?: "main"
                    val rawUrl =
                        "https://raw.githubusercontent.com/${source.key}/$branch/${entry.file}"
                    playFromUrl(rawUrl)
                }

                source.key.startsWith("content://") -> playLocalFile(source.key, entry.file)
                else -> playFileFromPath(source.key, entry.file)
            }
        }
    }

    fun getDownloadedRepos(): Set<String> =
        prefs.getStringSet(KEY_DOWNLOADED_REPOS, emptySet()) ?: emptySet()

    suspend fun downloadRepo(repoSlug: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val (index, branch) = fetchIndex(repoSlug) ?: return@withContext false

            val repoDir = getDownloadedDir(repoSlug)
            val jinglesDir = File(repoDir, "jingles")
            repoDir.mkdirs()
            jinglesDir.mkdirs()

            // Write index.json
            File(repoDir, "index.json").writeText(json.encodeToString(index))

            // Download each audio file
            for (entry in index.entries) {
                val fileName = entry.file.substringAfterLast("/")
                val rawUrl = "https://raw.githubusercontent.com/$repoSlug/$branch/${entry.file}"
                val dest = File(jinglesDir, fileName)
                URL(rawUrl).openStream().use { it.copyTo(dest.outputStream()) }
            }

            // Mark as downloaded
            val updated = getDownloadedRepos().toMutableSet().also { it.add(repoSlug) }
            prefs.edit { putStringSet(KEY_DOWNLOADED_REPOS, updated) }

            indicesLoaded = false
            loadIndices()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun stop() {
        currentGameFilename = null
        currentPlayJob?.cancel()
        scope.launch(Dispatchers.Main) { player?.stop() }
    }

    fun release() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        scope.launch(Dispatchers.Main) {
            player?.release()
            player = null
        }
    }

    private fun getDownloadedDir(repoSlug: String): File =
        File(
            Environment.getExternalStorageDirectory(),
            "$JARNGREIPR_FOLDER/${repoSlug.replace("/", "-")}"
        )

    private suspend fun loadIndices() {
        val result = mutableListOf<JingleSource>()

        // 1. Downloaded repos — offline-first, direct file access
        val downloadedRepos =
            prefs.getStringSet(KEY_DOWNLOADED_REPOS, emptySet())?.toList() ?: emptyList()
        for (repoSlug in downloadedRepos) {
            val dir = getDownloadedDir(repoSlug)
            if (!dir.exists()) continue
            val indexText =
                runCatching { File(dir, "index.json").readText() }.getOrNull() ?: continue
            val index = runCatching { json.decodeFromString<JingleIndex>(indexText) }.getOrNull()
                ?: continue
            result.add(JingleSource(key = dir.absolutePath, index = index, isLocal = true))
        }

        // 2. User-picked local folders (SAF)
        val folders = prefs.getStringSet(KEY_LOCAL_FOLDERS, emptySet())?.toList() ?: emptyList()
        for (uriString in folders) {
            val index = loadLocalIndex(uriString) ?: continue
            result.add(JingleSource(key = uriString, index = index, isLocal = true))
        }

        // 3. GitHub repos
        val repos = prefs.getStringSet(KEY_REPOS, emptySet())?.toList() ?: emptyList()
        for (repo in repos) {
            val (index, branch) = fetchIndex(repo) ?: continue
            cachedBranch[repo] = branch
            result.add(JingleSource(key = repo, index = index, isLocal = false))
        }

        cachedIndices = result
        indicesLoaded = true

        // Build name map: key → index display name
        val nameMap = mutableMapOf<String, String>()
        for (source in result) {
            if (source.index.name.isNotBlank()) nameMap[source.key] = source.index.name
        }
        // Downloaded repos are keyed by file path, but the screen identifies them by repo slug —
        // map the slug too so the GitHub RepoCard can find the name before/without a network hit.
        val downloadedSlugs = prefs.getStringSet(KEY_DOWNLOADED_REPOS, emptySet()) ?: emptySet()
        for (slug in downloadedSlugs) {
            val filePath = getDownloadedDir(slug).absolutePath
            nameMap[filePath]?.let { nameMap[slug] = it }
        }
        _indexNames.value = nameMap
    }

    private suspend fun fetchIndex(repoSlug: String): Pair<JingleIndex, String>? {
        for (branch in listOf("main", "master")) {
            try {
                val url = "https://raw.githubusercontent.com/$repoSlug/$branch/index.json"
                val text = withContext(Dispatchers.IO) {
                    URL(url).openStream().bufferedReader().use { it.readText() }
                }
                return json.decodeFromString<JingleIndex>(text) to branch
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun loadLocalIndex(uriString: String): JingleIndex? {
        return try {
            val folder = DocumentFile.fromTreeUri(context, uriString.toUri()) ?: return null
            val indexFile = folder.findFile("index.json") ?: return null
            val text = context.contentResolver.openInputStream(indexFile.uri)
                ?.bufferedReader()?.use { it.readText() } ?: return null
            json.decodeFromString<JingleIndex>(text)
        } catch (_: Exception) {
            null
        }
    }

    private fun findMatch(gameFilename: String): Pair<JingleSource, JingleEntry>? {
        val baseName = gameFilename
            .substringAfterLast("/")
            .substringAfterLast("\\")
            .substringBeforeLast(".")
            .trim()

        for (source in cachedIndices) {
            val entry = source.index.entries.firstOrNull { entry ->
                entry.game.equals(baseName, ignoreCase = true) ||
                        baseName.contains(entry.game, ignoreCase = true) ||
                        entry.game.contains(baseName, ignoreCase = true)
            }
            if (entry != null) return source to entry
        }
        return null
    }

    private suspend fun playLocalFile(
        folderUriString: String,
        filePath: String
    ) {
        try {
            val folder = DocumentFile.fromTreeUri(context, folderUriString.toUri()) ?: return
            val parts = filePath.split("/")
            var current: DocumentFile? = folder
            for (part in parts.dropLast(1)) {
                current = current?.findFile(part) ?: return
            }
            val audioFile = current?.findFile(parts.last()) ?: return
            playFromUrl(audioFile.uri.toString())
        } catch (_: Exception) {
        }
    }

    private suspend fun playFileFromPath(basePath: String, filePath: String) {
        val file = File(basePath, filePath)
        if (file.exists()) playFromUrl(Uri.fromFile(file).toString())
    }

    @OptIn(UnstableApi::class)
    private fun initPlayer() {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                500,
                10_000,
                250,
                500
            ).build()
        player = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
    }

    @OptIn(UnstableApi::class)
    private suspend fun playFromUrl(url: String) {
        withContext(Dispatchers.Main) {
            val p = player ?: run { initPlayer(); player!! }
            with(p) {
                stop()
                clearMediaItems()
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                play()
            }
        }
    }
}
