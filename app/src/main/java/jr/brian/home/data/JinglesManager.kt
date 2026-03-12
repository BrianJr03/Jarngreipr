package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
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
import jr.brian.home.model.jingles.JinglesResult
import jr.brian.home.model.jingles.LookupSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
            scope.launch { reloadIndices() }
        }
    }

    private val _indexNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val indexNames: StateFlow<Map<String, String>> = _indexNames.asStateFlow()

    private val _indexCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val indexCounts: StateFlow<Map<String, Int>> = _indexCounts.asStateFlow()

    private val _isMuted = MutableStateFlow(prefs.getBoolean(KEY_MUTED, false))
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    /**
     * Emits a human-readable error message whenever a background operation fails.
     * UI can collect this to show a snackbar / toast. Uses SharedFlow so concurrent
     * failures are queued rather than the second silently clobbering the first.
     */
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    /**
     * All lookup state lives in one immutable snapshot.
     * Writes happen under [snapshotMutex]; reads are lock-free because
     * [@Volatile] guarantees visibility of the latest reference.
     */
    @Volatile
    private var snapshot = LookupSnapshot()
    private val snapshotMutex = Mutex()

    /**
     * Warm entries discovered via the contains() fallback are staged here
     * and merged into the next snapshot, so we never mutate [snapshot.exactMap]
     * from multiple coroutines simultaneously.
     */
    private val warmStagingMutex = Mutex()
    private val warmStaging = mutableMapOf<String, Pair<JingleSource, JingleEntry>>()

    private var player: ExoPlayer? = null
    private var currentPlayJob: Job? = null
    @Volatile private var currentGameFilename: String? = null

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        scope.launch { reloadIndices() }
        scope.launch(Dispatchers.Main) {
            initPlayer()
            if (_isMuted.value) player?.volume = 0f
        }
    }


    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        prefs.edit { putBoolean(KEY_MUTED, muted) }
        scope.launch(Dispatchers.Main) {
            player?.volume = if (muted) 0f else 1f
        }
    }

    fun onGameSelected(gameFilename: String) {
        if (!prefs.getBoolean(KEY_ENABLED, true)) return
        if (gameFilename == currentGameFilename) return
        currentGameFilename = gameFilename

        currentPlayJob?.cancel()
        currentPlayJob = scope.launch {
            withContext(Dispatchers.Main) { player?.stop() }

            // Ensure indices are loaded before trying to match
            if (snapshot.sources.isEmpty()) reloadIndices()

            val (source, entry) = findMatch(gameFilename) ?: return@launch
            val snap = snapshot

            when {
                !source.isLocal -> {
                    val branch = snap.branchMap[source.key] ?: "main"
                    val rawUrl = "${GitHubUrls.RAW_BASE}/${source.key}/$branch/${entry.file}"
                    playFromUrl(rawUrl)
                }
                source.key.startsWith("content://") ->
                    playLocalFile(source.key, entry.file)
                else ->
                    playFileFromPath(source.key, entry.file)
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
    ): JinglesResult<Unit> = withContext(Dispatchers.IO) {
        val tag = "DownloadRepo"
        try {
            Log.d(tag, "Starting download for repo: $repoSlug")

            val fetchResult = fetchIndex(repoSlug)
                ?: return@withContext JinglesResult.Failure("Could not fetch index.json for $repoSlug")

            val (index, branch) = fetchResult
            Log.d(tag, "Fetched index. Branch=$branch, Entries=${index.entries.size}")

            // Use app-specific external dir — no MANAGE_EXTERNAL_STORAGE needed
            val repoDir = getDownloadedDir(repoSlug)
            val jinglesDir = File(repoDir, "jingles")
            repoDir.mkdirs()
            jinglesDir.mkdirs()

            // Write index.json
            File(repoDir, "index.json").writeText(json.encodeToString(index))

            val total = index.entries.size.coerceAtLeast(1)
            var processed = 0
            onProgress(0f)

            for (entry in index.entries) {
                ensureActive() // respect cancellation between file downloads
                val rawUrl = "${GitHubUrls.RAW_BASE}/$repoSlug/$branch/${entry.file}"
                val dest = File(repoDir, entry.file)
                dest.parentFile?.mkdirs()

                if (dest.exists()) {
                    Log.d(tag, "Skipping ${entry.file} — already exists")
                    onProgress(++processed / total.toFloat())
                    continue
                }

                val downloadResult = downloadFile(rawUrl, dest)
                if (downloadResult is JinglesResult.Failure) {
                    // Log and continue — don't abort the whole batch for one missing file
                    Log.e(tag, "Failed to download ${entry.file}: ${downloadResult.message}")
                    _errors.tryEmit("Warning: could not download ${entry.file}")
                }
                onProgress(++processed / total.toFloat())
            }

            val updated = getDownloadedRepos().toMutableSet().also { it.add(repoSlug) }
            prefs.edit { putStringSet(KEY_DOWNLOADED_REPOS, updated) }
            reloadIndices()
            Log.d(tag, "Done.")
            JinglesResult.Success(Unit)
        } catch (e: Exception) {
            val msg = "downloadRepo failed for $repoSlug: ${e.message}"
            Log.e(tag, msg, e)
            _errors.tryEmit(msg)
            JinglesResult.Failure(msg, e)
        }
    }

    suspend fun searchRepos(query: String): JinglesResult<List<GitHubRepoResult>> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                val url =
                    "${GitHubUrls.API_SEARCH_REPOS}?q=$encoded+in:name&sort=stars&per_page=30"
                val conn = openConnection(url)
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val results = json.decodeFromString<GitHubSearchResponse>(text).items
                val filtered = coroutineScope {
                    results
                        .map { repo -> async { repo to isValidJinglesRepo(repo.fullName) } }
                        .awaitAll()
                        .filter { (_, valid) -> valid }
                        .map { (repo, _) -> repo }
                }
                JinglesResult.Success(filtered)
            } catch (e: Exception) {
                val msg = "searchRepos failed: ${e.message}"
                _errors.tryEmit(msg)
                JinglesResult.Failure(msg, e)
            }
        }

    fun refreshLocalIndices() {
        scope.launch { reloadIndices() }
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

    fun getDownloadedFileCount(repoSlug: String): Int {
        val jinglesDir = File(getDownloadedDir(repoSlug), "jingles")
        return jinglesDir.walkTopDown().count { it.isFile }
    }

    fun getDownloadedSizeBytes(repoSlug: String): Long {
        val jinglesDir = File(getDownloadedDir(repoSlug), "jingles")
        return jinglesDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    suspend fun fetchJinglesSizeBytes(repoSlug: String): JinglesResult<Long> =
        withContext(Dispatchers.IO) {
            try {
                val branch = snapshot.branchMap[repoSlug] ?: "main"
                val url =
                    "${GitHubUrls.API_REPOS_BASE}/$repoSlug/contents/jingles?ref=$branch"
                val conn = openConnection(url)
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val size = json.decodeFromString<List<GitHubContentEntry>>(text)
                    .filter { it.type == "file" }
                    .sumOf { it.size }
                JinglesResult.Success(size)
            } catch (e: Exception) {
                JinglesResult.Failure("fetchJinglesSizeBytes failed: ${e.message}", e)
            }
        }

    @OptIn(UnstableApi::class)
    suspend fun getLocalFolderSizeBytes(uriString: String): Long = withContext(Dispatchers.IO) {
        try {
            val folder =
                DocumentFile.fromTreeUri(context, uriString.toUri()) ?: return@withContext 0L
            val jinglesFolder = folder.findFile("jingles") ?: return@withContext 0L
            jinglesFolder.listFiles().filter { it.isFile }.sumOf { it.length() }
        } catch (e: Exception) {
            Log.w("JinglesManager", "getLocalFolderSizeBytes failed: ${e.message}")
            0L
        }
    }

    /**
     * Builds the full [LookupSnapshot] from all three source types and
     * replaces [snapshot] atomically under [snapshotMutex].
     * Cancellation-safe: [ensureActive] is called between each IO stage.
     */
    @OptIn(UnstableApi::class)
    private suspend fun reloadIndices() {
        // ── Build everything outside the lock (including all I/O and network) ──

        val result = mutableListOf<JingleSource>()
        val newBranchMap = mutableMapOf<String, String>()

        // 1. Downloaded repos — offline-first
        val downloadedRepos =
            prefs.getStringSet(KEY_DOWNLOADED_REPOS, emptySet())?.toList() ?: emptyList()
        for (repoSlug in downloadedRepos) {
            currentCoroutineContext().ensureActive()
            val dir = getDownloadedDir(repoSlug)
            if (!dir.exists()) continue
            val indexText = runCatching { File(dir, "index.json").readText() }
                .onFailure { Log.w("JinglesManager", "Cannot read index for $repoSlug: ${it.message}") }
                .getOrNull() ?: continue
            val index = runCatching { json.decodeFromString<JingleIndex>(indexText) }
                .onFailure { Log.w("JinglesManager", "Malformed index for $repoSlug: ${it.message}") }
                .getOrNull() ?: continue
            result.add(JingleSource(key = dir.absolutePath, index = index, isLocal = true))
        }

        // 2. User-picked local folders (SAF)
        currentCoroutineContext().ensureActive()
        val folders =
            prefs.getStringSet(KEY_LOCAL_FOLDERS, emptySet())?.toList() ?: emptyList()
        for (uriString in folders) {
            currentCoroutineContext().ensureActive()
            val index = loadLocalIndex(uriString) ?: continue
            result.add(JingleSource(key = uriString, index = index, isLocal = true))
        }

        // 3. GitHub repos (network — must stay outside the lock)
        currentCoroutineContext().ensureActive()
        val repos = prefs.getStringSet(KEY_REPOS, emptySet())?.toList() ?: emptyList()
        for (repo in repos) {
            currentCoroutineContext().ensureActive()
            val (index, branch) = fetchIndex(repo) ?: continue
            newBranchMap[repo] = branch
            result.add(JingleSource(key = repo, index = index, isLocal = false))
        }

        // Preserve branches already known from the old snapshot
        val mergedBranch = snapshot.branchMap + newBranchMap

        // Build lookup structures
        val exact = mutableMapOf<String, Pair<JingleSource, JingleEntry>>()
        val lower = mutableListOf<Triple<String, JingleSource, JingleEntry>>()
        for (source in result) {
            for (entry in source.index.entries) {
                val key = entry.game.lowercase()
                if (key !in exact) exact[key] = source to entry
                lower.add(Triple(key, source, entry))
            }
        }

        // Build UI state maps
        val nameMap = mutableMapOf<String, String>()
        val countMap = mutableMapOf<String, Int>()
        for (source in result) {
            if (source.index.name.isNotBlank()) nameMap[source.key] = source.index.name
            countMap[source.key] = source.index.entries.size
        }
        // Also expose downloaded repos by slug so cards work before network hit
        for (slug in downloadedRepos) {
            val filePath = getDownloadedDir(slug).absolutePath
            nameMap[filePath]?.let { nameMap[slug] = it }
            countMap[filePath]?.let { countMap[slug] = it }
        }

        // ── Atomic swap — only the final assignment is locked ─────────────────
        snapshotMutex.withLock {
            // Merge warm entries staged while we were building
            warmStagingMutex.withLock {
                for ((k, v) in warmStaging) {
                    if (k !in exact) exact[k] = v
                }
                warmStaging.clear()
            }

            snapshot = LookupSnapshot(
                sources = result,
                exactMap = exact,
                lowerEntries = lower,
                branchMap = mergedBranch,
            )
            _indexNames.value = nameMap
            _indexCounts.value = countMap
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun fetchIndex(repoSlug: String): Pair<JingleIndex, String>? {
        for (branch in listOf("main", "master")) {
            try {
                val url = "${GitHubUrls.RAW_BASE}/$repoSlug/$branch/index.json"
                val text = withContext(Dispatchers.IO) {
                    val conn = openConnection(url)
                    conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
                }
                return json.decodeFromString<JingleIndex>(text) to branch
            } catch (e: Exception) {
                Log.d("JinglesManager", "fetchIndex: branch=$branch miss for $repoSlug: ${e.message}")
            }
        }
        Log.w("JinglesManager", "fetchIndex: could not fetch index.json for $repoSlug")
        return null
    }

    @OptIn(UnstableApi::class)
    private fun loadLocalIndex(uriString: String): JingleIndex? {
        return try {
            val folder = DocumentFile.fromTreeUri(context, uriString.toUri()) ?: return null
            val indexFile = folder.findFile("index.json") ?: return null
            val text = context.contentResolver.openInputStream(indexFile.uri)
                ?.bufferedReader()?.use { it.readText() } ?: return null
            json.decodeFromString<JingleIndex>(text)
        } catch (e: Exception) {
            Log.w("JinglesManager", "loadLocalIndex failed for $uriString: ${e.message}")
            null
        }
    }

    private suspend fun isValidJinglesRepo(repoSlug: String): Boolean =
        fetchIndex(repoSlug) != null

    /**
     * Two-tier lookup:
     * 1. O(1) exact match against the current snapshot's exactMap.
     * 2. O(n) contains fallback — result is staged for merge into the next snapshot.
     *
     * Reading [snapshot] without a lock is intentional: [@Volatile] guarantees
     * we always see the latest fully-constructed snapshot reference.
     */
    private suspend fun findMatch(
        gameFilename: String
    ): Pair<JingleSource, JingleEntry>? {
        val baseName = gameFilename
            .substringAfterLast("/")
            .substringAfterLast("\\")
            .substringBeforeLast(".")
            .trim()
            .lowercase()

        val snap = snapshot

        // Tier 1 — O(1)
        snap.exactMap[baseName]?.let { return it }

        // Tier 2 — O(n) contains fallback
        for ((lowerGame, source, entry) in snap.lowerEntries) {
            if (baseName.contains(lowerGame) || lowerGame.contains(baseName)) {
                // Stage the warm entry; it will be merged on the next reloadIndices()
                warmStagingMutex.withLock { warmStaging[baseName] = source to entry }
                return source to entry
            }
        }
        return null
    }

    private fun downloadFile(rawUrl: String, dest: File): JinglesResult<Unit> {
        val conn = openConnection(rawUrl)
        return try {
            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val body = conn.errorStream?.bufferedReader()?.readText()?.take(300) ?: ""
                return JinglesResult.Failure("HTTP $responseCode for $rawUrl — $body")
            }
            val tmp = File(dest.parentFile, "${dest.name}.tmp")
            conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            if (!tmp.renameTo(dest)) {
                tmp.delete()
                return JinglesResult.Failure("Rename failed: ${tmp.absolutePath} -> ${dest.absolutePath}")
            }
            JinglesResult.Success(Unit)
        } catch (e: IOException) {
            JinglesResult.Failure("IO error downloading $rawUrl: ${e.message}", e)
        } finally {
            conn.disconnect()
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun playLocalFile(folderUriString: String, filePath: String) {
        try {
            val folder =
                DocumentFile.fromTreeUri(context, folderUriString.toUri()) ?: return
            val parts = filePath.split("/")
            var current: DocumentFile? = folder
            for (part in parts.dropLast(1)) {
                current = current?.findFile(part) ?: return
            }
            val audioFile = current?.findFile(parts.last()) ?: return
            playFromUrl(audioFile.uri.toString())
        } catch (e: Exception) {
            Log.w("JinglesManager", "playLocalFile failed: ${e.message}")
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun playFileFromPath(basePath: String, filePath: String) {
        val file = File(basePath, filePath)
        if (file.exists()) {
            playFromUrl(Uri.fromFile(file).toString())
        } else {
            Log.w("JinglesManager", "playFileFromPath: file not found: ${file.absolutePath}")
        }
    }

    @OptIn(UnstableApi::class)
    private fun initPlayer() {
        if (player != null) return
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(500, 10_000, 250, 500)
            .build()
        player = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
    }

    @OptIn(UnstableApi::class)
    private suspend fun playFromUrl(url: String) {
        withContext(Dispatchers.Main) {
            initPlayer()
            val p = player ?: return@withContext
            p.stop()
            p.clearMediaItems()
            p.setMediaItem(MediaItem.fromUri(url))
            p.prepare()
            if (!_isMuted.value) p.volume = 1f else p.volume = 0f
            p.play()
        }
    }

    /**
     * Opens an [HttpURLConnection] with sensible defaults.
     * Caller is responsible for calling [HttpURLConnection.disconnect].
     */
    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/vnd.github+json")
            instanceFollowRedirects = true
        }

    /**
     * Uses [Context.getExternalFilesDir] — app-specific, no special permissions needed,
     * automatically cleaned up on uninstall.
     */
    private fun getDownloadedDir(repoSlug: String): File =
        File(
            context.getExternalFilesDir(null),
            "${JARNGREIPR_FOLDER}/${repoSlug.replace("/", "-")}"
        )
}
