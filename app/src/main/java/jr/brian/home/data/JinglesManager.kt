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
import jr.brian.home.util.GitHubUrls
import jr.brian.home.model.GitHubContentEntry
import jr.brian.home.model.GitHubRepoResult
import jr.brian.home.model.GitHubSearchResponse
import jr.brian.home.model.JingleEntry
import jr.brian.home.model.JingleIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
private const val KEY_MUTED = "jingles_muted"
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

    private val _indexCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val indexCounts: StateFlow<Map<String, Int>> = _indexCounts

    private var player: ExoPlayer? = null
    private var currentPlayJob: Job? = null
    private var currentGameFilename: String? = null
    private var cachedIndices: List<JingleSource> = emptyList()
    private var exactMatchMap: MutableMap<String, Pair<JingleSource, JingleEntry>> = mutableMapOf()
    private var lowerEntries: List<Triple<String, JingleSource, JingleEntry>> = emptyList()
    private var indicesLoaded = false
    private val cachedBranch = mutableMapOf<String, String>()

    private val _isMuted = MutableStateFlow(prefs.getBoolean(KEY_MUTED, false))
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        prefs.edit { putBoolean(KEY_MUTED, muted) }
        scope.launch(Dispatchers.Main) {
            player?.volume = if (muted) 0f else 1f
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        scope.launch { loadIndices() }
        scope.launch(Dispatchers.Main) {
            initPlayer()
            if (_isMuted.value) player?.volume = 0f
        }
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
                        "${GitHubUrls.RAW_BASE}/${source.key}/$branch/${entry.file}"
                    playFromUrl(rawUrl)
                }

                source.key.startsWith("content://") -> playLocalFile(source.key, entry.file)
                else -> playFileFromPath(source.key, entry.file)
            }
        }
    }

    fun getDownloadedRepos(): Set<String> =
        prefs.getStringSet(KEY_DOWNLOADED_REPOS, emptySet()) ?: emptySet()

    fun removeDownloadedRepo(repoSlug: String) {
        val updated = getDownloadedRepos().toMutableSet().also { it.remove(repoSlug) }
        prefs.edit { putStringSet(KEY_DOWNLOADED_REPOS, updated) }
    }

    @OptIn(UnstableApi::class)
    suspend fun downloadRepo(
        repoSlug: String,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
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
            val total = index.entries.size.coerceAtLeast(1)
            var processed = 0
            onProgress(0f)
            for (entry in index.entries) {
                val rawUrl = "${GitHubUrls.RAW_BASE}/$repoSlug/$branch/${entry.file}"
                val dest = File(repoDir, entry.file)
                dest.parentFile?.mkdirs()

                Log.d(tag, "Processing entry: ${entry.file}")
                Log.d(tag, "  rawUrl: $rawUrl")
                Log.d(tag, "  dest: ${dest.absolutePath}")

                if (dest.exists()) {
                    Log.d(tag, "  Skipping — dest already exists (${dest.length()} bytes)")
                    processed++
                    onProgress(processed.toFloat() / total)
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
                    processed++
                    onProgress(processed.toFloat() / total)
                    continue
                }

                val tmp = File(dest.parentFile, "${dest.name}.tmp")
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
                    }
                } catch (e: Exception) {
                    Log.e(tag, "  Exception during download of ${entry.file}: ${e.message}", e)
                    tmp.delete()
                } finally {
                    conn.disconnect()
                }
                processed++
                onProgress(processed.toFloat() / total)
            }

            Log.d(tag, "All entries processed. Marking $repoSlug as downloaded.")
            val updated = getDownloadedRepos().toMutableSet().also { it.add(repoSlug) }
            prefs.edit { putStringSet(KEY_DOWNLOADED_REPOS, updated) }

            indicesLoaded = false
            loadIndices()
            Log.d(tag, "Done. loadIndices() called.")
            true
        } catch (e: Exception) {
            Log.e("DownloadRepo", "Exception: ${e.message}", e)
            false
        }
    }

    suspend fun searchRepos(query: String): List<GitHubRepoResult> = withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "${GitHubUrls.API_SEARCH_REPOS}?q=$encoded+in:name&sort=stars&per_page=30"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Accept", "application/vnd.github+json")
                instanceFollowRedirects = true
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val results = json.decodeFromString<GitHubSearchResponse>(text).items
            coroutineScope {
                results
                    .map { repo -> async { repo to isValidJinglesRepo(repo.fullName) } }
                    .awaitAll()
                    .filter { (_, valid) -> valid }
                    .map { (repo, _) -> repo }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun isValidJinglesRepo(repoSlug: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val (_, branch) = fetchIndex(repoSlug) ?: return@withContext false
                val url = "${GitHubUrls.API_REPOS_BASE}/$repoSlug/contents/jingles?ref=$branch"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 15_000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    instanceFollowRedirects = true
                }
                val responseCode = conn.responseCode
                conn.disconnect()
                responseCode in 200..299
            } catch (_: Exception) {
                false
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

    /** Counts audio files actually present in the downloaded jingles folder for [repoSlug]. */
    fun getDownloadedFileCount(repoSlug: String): Int {
        val jinglesDir = File(getDownloadedDir(repoSlug), "jingles")
        return jinglesDir.walkTopDown().count { it.isFile }
    }

    /** Sums the sizes of all files in the downloaded jingles folder for [repoSlug]. */
    fun getDownloadedSizeBytes(repoSlug: String): Long {
        val jinglesDir = File(getDownloadedDir(repoSlug), "jingles")
        return jinglesDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /** Fetches the total size of jingle files for a GitHub repo via the contents API. */
    suspend fun fetchJinglesSizeBytes(repoSlug: String): Long? = withContext(Dispatchers.IO) {
        try {
            val branch = cachedBranch[repoSlug] ?: "main"
            val url = "${GitHubUrls.API_REPOS_BASE}/$repoSlug/contents/jingles?ref=$branch"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github+json")
                instanceFollowRedirects = true
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            json.decodeFromString<List<jr.brian.home.model.GitHubContentEntry>>(text)
                .filter { it.type == "file" }
                .sumOf { it.size }
        } catch (_: Exception) {
            null
        }
    }

    /** Returns the total size (bytes) of audio files in a SAF-picked local folder. */
    suspend fun getLocalFolderSizeBytes(uriString: String): Long = withContext(Dispatchers.IO) {
        try {
            val folder =
                DocumentFile.fromTreeUri(context, uriString.toUri()) ?: return@withContext 0L
            val jinglesFolder = folder.findFile("jingles") ?: return@withContext 0L
            jinglesFolder.listFiles().filter { it.isFile }.sumOf { it.length() }
        } catch (_: Exception) {
            0L
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

        // Build name map and count map: key → display name / entry count
        val nameMap = mutableMapOf<String, String>()
        val countMap = mutableMapOf<String, Int>()
        for (source in result) {
            if (source.index.name.isNotBlank()) nameMap[source.key] = source.index.name
            countMap[source.key] = source.index.entries.size
        }
        // Downloaded repos are keyed by file path, but the screen identifies them by repo slug —
        // map the slug too so the GitHub RepoCard can find the name/count before/without a network hit.
        val downloadedSlugs = prefs.getStringSet(KEY_DOWNLOADED_REPOS, emptySet()) ?: emptySet()
        for (slug in downloadedSlugs) {
            val filePath = getDownloadedDir(slug).absolutePath
            nameMap[filePath]?.let { nameMap[slug] = it }
            countMap[filePath]?.let { countMap[slug] = it }
        }
        _indexNames.value = nameMap
        _indexCounts.value = countMap

        val exact = mutableMapOf<String, Pair<JingleSource, JingleEntry>>()
        val lower = mutableListOf<Triple<String, JingleSource, JingleEntry>>()
        for (source in result) {
            for (entry in source.index.entries) {
                val key = entry.game.lowercase()
                if (key !in exact) exact[key] = source to entry
                lower.add(Triple(key, source, entry))
            }
        }
        exactMatchMap = exact
        lowerEntries = lower
    }

    private suspend fun fetchIndex(repoSlug: String): Pair<JingleIndex, String>? {
        for (branch in listOf("main", "master")) {
            try {
                val url = "${GitHubUrls.RAW_BASE}/$repoSlug/$branch/index.json"
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
            .lowercase()

        // O(1) exact match
        exactMatchMap[baseName]?.let { return it }

        // O(n) contains fallback — result cached so subsequent calls for this game are O(1)
        for ((lowerGame, source, entry) in lowerEntries) {
            if (baseName.contains(lowerGame) || lowerGame.contains(baseName)) {
                exactMatchMap[baseName] = source to entry
                return source to entry
            }
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

    private suspend fun playFileFromPath(
        basePath: String,
        filePath: String
    ) {
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
