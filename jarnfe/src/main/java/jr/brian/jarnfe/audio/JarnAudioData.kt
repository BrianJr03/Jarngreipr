package jr.brian.jarnfe.audio

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class JarnMusicSource { Folder, File }

@Immutable
data class JarnMusicSettings(
    val source: JarnMusicSource,
    val folderName: String?,
    val fileName: String?,
    val volume: Float,
    val muted: Boolean,
)

@Immutable
data class JarnJingleSettings(
    val enabled: Boolean = true,
    val muted: Boolean = false,
    val volume: Float = 1f,
    val regexPriority: Boolean = false,
    val normalize: Boolean = false,
)

@Immutable
data class JarnJingleRepo(
    val slug: String,
    val name: String?,
    val entryCount: Int?,
    val downloaded: Boolean,
)

@Immutable
data class JarnJingleFolder(val path: String, val name: String, val entryCount: Int?)

@Immutable
data class JarnJingleSearchResult(val slug: String, val description: String?, val stars: Int)

@Immutable
data class JarnJingleDownload(val slug: String, val progress: Float)

enum class JarnJingleDownloadResult { Done, Rejected, Failed }

enum class JarnJingleFolderResult { Added, Duplicate, Invalid }

@Immutable
data class JarnJinglePackDraft(
    val packName: String,
    val existingPath: String?,
    val gameName: String,
    val platform: String,
    val fileName: String,
    val audioUri: String,
)

/** Host services behind music and jingles, backed by the same engines and stores Classic uses. */
interface JarnAudioData {
    /** Reads Compose state, so a composable using it follows every change. */
    val music: JarnMusicSettings
    val jingles: Flow<JarnJingleSettings>
    val repos: Flow<List<JarnJingleRepo>>
    val folders: Flow<List<JarnJingleFolder>>
    val download: StateFlow<JarnJingleDownload?>

    /** True while RSS audio or another app's media is playing. */
    val otherMediaPlaying: Flow<Boolean>

    fun allowPlayback()

    fun holdPlayback()

    suspend fun playJingle(gameKey: String): Boolean

    fun stopJingle()

    fun setMusicFolder(uri: String)

    fun setMusicFile(uri: String)

    fun setMusicVolume(volume: Float)

    fun setMusicMuted(muted: Boolean)

    fun setJinglesEnabled(enabled: Boolean)

    fun setJinglesMuted(muted: Boolean)

    fun setJingleVolume(volume: Float)

    fun setRegexPriority(enabled: Boolean)

    fun setNormalize(enabled: Boolean)

    /** [slug] must pass [jarnJingleRepoSlug]. Returns false when invalid or already added. */
    fun addRepo(slug: String): Boolean

    fun removeRepo(slug: String)

    /** Null when the search failed. */
    suspend fun searchRepos(query: String): List<JarnJingleSearchResult>?

    fun startDownload(slug: String, onDone: (JarnJingleDownloadResult) -> Unit)

    fun cancelDownload()

    fun addFolder(uri: String): JarnJingleFolderResult

    fun removeFolder(path: String)

    fun refreshFolders()

    /** The saved parent for new packs, or null when it is unset or its access was lost. */
    fun packParent(): String?

    fun setPackParent(uri: String)

    suspend fun savePackEntry(parentUri: String, draft: JarnJinglePackDraft): Boolean

    suspend fun gameKey(libraryId: String): String?
}

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnAudioDataProvider {
    val jarnAudioData: JarnAudioData
}

internal fun Context.jarnAudioDataOrNull(): JarnAudioData? =
    (applicationContext as? JarnAudioDataProvider)?.jarnAudioData
