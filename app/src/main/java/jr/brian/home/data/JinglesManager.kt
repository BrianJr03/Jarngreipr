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
import androidx.media3.common.util.Log
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
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
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
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

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

    @OptIn(UnstableApi::class)
    suspend fun downloadRepo(repoSlug: String): Boolean = withContext(Dispatchers.IO) {
        val tag = "DownloadRepo"
        try {
            Log.d(tag, "Starting download for repo: $repoSlug")

            val fetchResult = fetchIndex(repoSlug)
            if (fetchResult == null) {
                Log.e(tag, "fetchIndex returned null for $repoSlug — aborting")
                return@withContext false
            }
            val (index, branch) = fetchResult
            Log.d(tag, "Fetched index. Branch: $branch, Entries: ${index.entries.size}")
            index.entries.forEachIndexed { i, e -> Log.d(tag, "  Entry[$i]: ${e.file}") }

            val repoDir = getDownloadedDir(repoSlug)
            val jinglesDir = File(repoDir, "jingles")
            Log.d(tag, "repoDir: ${repoDir.absolutePath}")
            Log.d(tag, "jinglesDir: ${jinglesDir.absolutePath}")

            repoDir.mkdirs()
            jinglesDir.mkdirs()
            Log.d(
                tag,
                "jinglesDir exists: ${jinglesDir.exists()}, isDir: ${jinglesDir.isDirectory}"
            )

            // Write index.json
            val indexFile = File(repoDir, "index.json")
            indexFile.writeText(json.encodeToString(index))
            Log.d(tag, "Wrote index.json (${indexFile.length()} bytes)")

            // Download each audio file
            for (entry in index.entries) {
                val fileName = entry.file.substringAfterLast("/")
                val rawUrl = "https://raw.githubusercontent.com/$repoSlug/$branch/${entry.file}"
                val dest = File(jinglesDir, fileName)

                Log.d(tag, "Processing entry: ${entry.file}")
                Log.d(tag, "  fileName: $fileName")
                Log.d(tag, "  rawUrl: $rawUrl")
                Log.d(tag, "  dest: ${dest.absolutePath}")

                if (dest.exists()) {
                    Log.d(tag, "  Skipping — dest already exists (${dest.length()} bytes)")
                    continue
                }

                val conn = (URL(rawUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 120_000
                    instanceFollowRedirects = true
                }

                val responseCode = conn.responseCode
                val contentType = conn.contentType
                val contentLength = conn.contentLengthLong
                Log.d(
                    tag,
                    "  HTTP $responseCode | Content-Type: $contentType | Length: $contentLength bytes"
                )

                if (responseCode !in 200..299) {
                    val errorBody =
                        conn.errorStream?.bufferedReader()?.readText() ?: "(no error body)"
                    Log.e(tag, "  Download failed for ${entry.file}: HTTP $responseCode")
                    Log.e(tag, "  Error body: ${errorBody.take(300)}")
                    conn.disconnect()
                    throw IOException("Failed to download ${entry.file}: HTTP $responseCode")
                }

                val tmp = File(jinglesDir, "$fileName.tmp")
                try {
                    conn.inputStream.use { input ->
                        tmp.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(tag, "  Downloaded to tmp (${tmp.length()} bytes)")

                    val renamed = tmp.renameTo(dest)
                    Log.d(
                        tag,
                        "  Renamed tmp -> dest: $renamed | dest exists: ${dest.exists()} (${dest.length()} bytes)"
                    )
                    if (!renamed) {
                        Log.e(
                            tag,
                            "  Rename failed! tmp: ${tmp.absolutePath} -> dest: ${dest.absolutePath}"
                        )
                        throw IOException("Failed to rename tmp file to $dest")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "  Exception during download of ${entry.file}: ${e.message}", e)
                    tmp.delete()
                    throw e
                } finally {
                    conn.disconnect()
                }
            }

            Log.d(tag, "All entries processed. Marking $repoSlug as downloaded.")
            val updated = getDownloadedRepos().toMutableSet().also { it.add(repoSlug) }
            prefs.edit { putStringSet(KEY_DOWNLOADED_REPOS, updated) }

            indicesLoaded = false
            loadIndices()
            Log.d(tag, "Done. loadIndices() called.")
            true
        } catch (e: Exception) {
            Log.e("DownloadRepo", "Unhandled exception: ${e.message}", e)
            throw e
        }
    }

    fun refreshLocalIndices() {
        indicesLoaded = false
        scope.launch { loadIndices() }
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
                    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15_000
                        readTimeout = 30_000
                        instanceFollowRedirects = true
                    }
                    conn.inputStream.bufferedReader().use { it.readText() }
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
