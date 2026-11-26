package jr.brian.jarnfe.backup

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.Flow

enum class JarnBackupArea {
    HomeLayout,
    Folders,
    Dock,
    PinnedApps,
    GameFolders,
    HiddenSystems,
    HiddenGames,
    ArtChoices,
    Collections,
    Favorites,
    RecentlyPlayed,
    Look,
    Wallpaper,
    Scraper,
    OtherSettings,
}

sealed interface JarnBackupChange {
    data object Same : JarnBackupChange
    data object Replaces : JarnBackupChange
    data class Adds(val count: Int) : JarnBackupChange

    /** The backup predates this area, so restoring leaves the current value alone. */
    data object NotIncluded : JarnBackupChange
}

@Immutable
data class JarnBackupItem(
    val area: JarnBackupArea,
    val backupCount: Int?,
    val currentCount: Int?,
    val change: JarnBackupChange,
)

/** Counts and flags only. Saved accounts and file contents never reach the preview. */
@Immutable
data class JarnBackupPreview(
    val schemaVersion: Int,
    val currentSchemaVersion: Int,
    val exportedAtMillis: Long?,
    val items: List<JarnBackupItem>,
    val imageReferenceCount: Int,
    val leavesJarnFe: Boolean,
) {
    val changeCount: Int
        get() = items.count { it.change is JarnBackupChange.Replaces || it.change is JarnBackupChange.Adds }
}

enum class JarnBackupProblem { NotABackup, Damaged, TooNew, Unreadable }

/** A checked backup waiting for confirmation. Its contents stay inside `:app`. */
interface JarnBackupCandidate {
    val preview: JarnBackupPreview
}

sealed interface JarnBackupInspection {
    class Ready(val candidate: JarnBackupCandidate) : JarnBackupInspection
    data class Failed(val problem: JarnBackupProblem) : JarnBackupInspection
}

enum class JarnBackupExport { Saved, SavedWithoutSomeFolders, Failed }

/** Implemented in `:app`, over the same backup format Classic uses. */
interface JarnBackup {
    suspend fun export(target: Uri): JarnBackupExport

    /** Reads and checks [source] without changing anything. */
    suspend fun inspect(source: Uri): JarnBackupInspection

    suspend fun restore(candidate: JarnBackupCandidate): Boolean
}

@Immutable
data class JarnUpdateVariant(
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val isHidden: Boolean,
    val matchesInstall: Boolean,
)

@Immutable
data class JarnUpdateOffer(
    val version: String,
    val notes: String,
    val isPrerelease: Boolean,
    val releasePageUrl: String,
    val variants: List<JarnUpdateVariant>,
)

sealed interface JarnUpdateCheck {
    data object UpToDate : JarnUpdateCheck
    data object Failed : JarnUpdateCheck
    data class Available(val offer: JarnUpdateOffer) : JarnUpdateCheck
}

/** A finished download, ready to hand to the system installer. */
interface JarnDownloadedUpdate

sealed interface JarnUpdateDownload {
    /** [percent] is null when the server doesn't report a size. */
    data class Progress(val percent: Int?, val downloadedBytes: Long, val totalBytes: Long) : JarnUpdateDownload
    class Ready(val file: JarnDownloadedUpdate) : JarnUpdateDownload
    data object Failed : JarnUpdateDownload
}

/** Implemented in `:app`, over the existing update checker and downloader. */
interface JarnUpdates {
    val currentVersion: String
    val rcEnabled: Boolean
    fun setRcEnabled(enabled: Boolean)
    suspend fun check(): JarnUpdateCheck
    fun skip(version: String)
    fun markDownloaded(version: String)
    fun canInstall(): Boolean
    fun openInstallPermission(): Boolean
    fun download(variant: JarnUpdateVariant): Flow<JarnUpdateDownload>
    fun install(file: JarnDownloadedUpdate): Boolean
    fun openReleasePage(offer: JarnUpdateOffer): Boolean
}

interface JarnDataProvider {
    val jarnBackup: JarnBackup
    val jarnUpdates: JarnUpdates
}

internal class JarnDataServices(val backup: JarnBackup, val updates: JarnUpdates)

internal fun Context.jarnDataServicesOrNull(): JarnDataServices? =
    (applicationContext as? JarnDataProvider)?.let { JarnDataServices(it.jarnBackup, it.jarnUpdates) }
