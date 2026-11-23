package jr.brian.home.jarn

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.data.AppUpdateManager
import jr.brian.home.data.config.BackupParse
import jr.brian.home.data.config.ImportExportManager
import jr.brian.home.data.config.JarngreiprConfig
import jr.brian.home.data.config.buildBackupPreview
import jr.brian.home.data.config.parseBackup
import jr.brian.home.data.config.readBackupText
import jr.brian.home.util.ApkVariant
import jr.brian.home.util.DownloadState
import jr.brian.home.util.UpdateChecker
import jr.brian.home.util.UpdateDownloader
import jr.brian.home.util.UpdateInfo
import jr.brian.home.util.VariantType
import jr.brian.jarnfe.backup.JarnBackup
import jr.brian.jarnfe.backup.JarnBackupCandidate
import jr.brian.jarnfe.backup.JarnBackupExport
import jr.brian.jarnfe.backup.JarnBackupInspection
import jr.brian.jarnfe.backup.JarnBackupPreview
import jr.brian.jarnfe.backup.JarnBackupProblem
import jr.brian.jarnfe.backup.JarnDownloadedUpdate
import jr.brian.jarnfe.backup.JarnUpdateCheck
import jr.brian.jarnfe.backup.JarnUpdateDownload
import jr.brian.jarnfe.backup.JarnUpdateOffer
import jr.brian.jarnfe.backup.JarnUpdateVariant
import jr.brian.jarnfe.backup.JarnUpdates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private class AppBackupCandidate(
    val config: JarngreiprConfig,
    override val preview: JarnBackupPreview,
) : JarnBackupCandidate {
    override fun toString(): String = "AppBackupCandidate(redacted)"
}

private class AppDownloadedUpdate(val file: File) : JarnDownloadedUpdate

/** Failures are never logged, because parser and provider errors can quote the file. */
@Singleton
class AppJarnBackup @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val manager: ImportExportManager,
) : JarnBackup {

    /** "wt" because plain "w" can leave old bytes behind when overwriting a longer file. */
    override suspend fun export(target: Uri): JarnBackupExport = guarded(JarnBackupExport.Failed) {
        context.contentResolver.openOutputStream(target, "wt")?.use { stream ->
            if (manager.exportToStream(stream) > 0) {
                JarnBackupExport.SavedWithoutSomeFolders
            } else {
                JarnBackupExport.Saved
            }
        } ?: JarnBackupExport.Failed
    }

    override suspend fun inspect(source: Uri): JarnBackupInspection =
        guarded(JarnBackupInspection.Failed(JarnBackupProblem.Unreadable)) {
            val text = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(source)?.use { readBackupText(it) }
            } ?: return@guarded JarnBackupInspection.Failed(JarnBackupProblem.Unreadable)
            val parsed = withContext(Dispatchers.Default) {
                parseBackup(text, JarngreiprConfig.CONFIG_VERSION, manager::decodeFromJson)
            }
            when (parsed) {
                is BackupParse.Rejected -> JarnBackupInspection.Failed(parsed.problem)
                is BackupParse.Ready -> JarnBackupInspection.Ready(
                    AppBackupCandidate(parsed.config, buildBackupPreview(parsed.config, manager.buildConfig())),
                )
            }
        }

    /** Can't be cancelled, so leaving the screen never stops a restore halfway through. */
    override suspend fun restore(candidate: JarnBackupCandidate): Boolean {
        val pending = candidate as? AppBackupCandidate ?: return false
        return withContext(NonCancellable) {
            guarded(false) {
                manager.applyConfig(pending.config)
                true
            }
        }
    }

    private suspend fun <T> guarded(fallback: T, block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        fallback
    }
}

/** A new prompt over the existing checker, downloader and skip rules. */
@Singleton
class AppJarnUpdates @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val manager: AppUpdateManager,
) : JarnUpdates {
    override val currentVersion: String
        get() = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

    override val rcEnabled: Boolean
        get() = manager.rcNotificationsEnabled

    override fun setRcEnabled(enabled: Boolean) = manager.setRcNotificationsEnabled(enabled)

    /** A manual check clears skip and download marks, the same as Classic's. */
    override suspend fun check(): JarnUpdateCheck {
        manager.clearSkippedVersion(context)
        manager.clearDownloadedVersion(context)
        val version = currentVersion
        val info = UpdateChecker.checkForUpdate(
            currentVersionName = version,
            includePrereleases = manager.rcNotificationsEnabled,
        )
        return when {
            info.downloadUrl.isBlank() -> JarnUpdateCheck.Failed
            info.isUpdateAvailable -> JarnUpdateCheck.Available(info.toOffer(version))
            else -> JarnUpdateCheck.UpToDate
        }
    }

    override fun skip(version: String) = manager.skipVersion(context, version)

    override fun markDownloaded(version: String) = manager.markVersionDownloaded(context, version)

    override fun canInstall(): Boolean = UpdateDownloader.canInstallPackages(context)

    override fun openInstallPermission(): Boolean = launching {
        UpdateDownloader.openInstallPermissionSettings(context)
    }

    override fun download(variant: JarnUpdateVariant): Flow<JarnUpdateDownload> =
        UpdateDownloader.downloadApk(
            context = context,
            downloadUrl = variant.downloadUrl,
            fileName = variant.fileName.ifBlank { "update.apk" },
        ).mapNotNull(DownloadState::toJarn)

    override fun install(file: JarnDownloadedUpdate): Boolean {
        val downloaded = file as? AppDownloadedUpdate ?: return false
        return launching { UpdateDownloader.installApk(context, downloaded.file) }
    }

    override fun openReleasePage(offer: JarnUpdateOffer): Boolean = launching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, offer.releasePageUrl.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private inline fun launching(start: () -> Unit): Boolean = try {
        start()
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }
}

private fun UpdateInfo.toOffer(installedVersion: String): JarnUpdateOffer {
    val installedType = VariantType.fromFileName("$installedVersion.apk")
    return JarnUpdateOffer(
        version = latestVersion,
        notes = releaseNotes,
        isPrerelease = isPrerelease,
        releasePageUrl = downloadUrl,
        variants = apkVariants
            .sortedByDescending { it.variantType == installedType }
            .map { it.toJarn(installedType) },
    )
}

private fun ApkVariant.toJarn(installedType: VariantType) = JarnUpdateVariant(
    fileName = fileName,
    downloadUrl = downloadUrl,
    sizeBytes = size,
    isHidden = variantType == VariantType.THOR,
    matchesInstall = variantType == installedType,
)

private fun DownloadState.toJarn(): JarnUpdateDownload? = when (this) {
    DownloadState.Idle -> null
    is DownloadState.Downloading -> JarnUpdateDownload.Progress(
        percent = progress.takeIf { it >= 0 },
        downloadedBytes = downloadedBytes,
        totalBytes = totalBytes,
    )
    is DownloadState.Success -> JarnUpdateDownload.Ready(AppDownloadedUpdate(file))
    is DownloadState.Error -> JarnUpdateDownload.Failed
}
