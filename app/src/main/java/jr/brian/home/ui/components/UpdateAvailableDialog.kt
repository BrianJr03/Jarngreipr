package jr.brian.home.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import jr.brian.home.model.state.UpdateDialogState
import jr.brian.home.ui.components.update.DownloadCompleteContent
import jr.brian.home.ui.components.update.DownloadingContent
import jr.brian.home.ui.components.update.ErrorContent
import jr.brian.home.ui.components.update.InfoContent
import jr.brian.home.ui.components.update.PermissionRequiredContent
import jr.brian.home.util.ApkVariant
import jr.brian.home.util.DownloadState
import jr.brian.home.util.UpdateDownloader
import jr.brian.home.util.UpdateInfo
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun UpdateAvailableDialog(
    updateInfo: UpdateInfo,
    currentVersion: String,
    onDismiss: () -> Unit,
    onRemindLater: () -> Unit,
    onSkipVersion: () -> Unit = {},
    onDownloadComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var dialogState by remember { mutableStateOf(UpdateDialogState.INFO) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var downloadedBytes by remember { mutableLongStateOf(0L) }
    var totalBytes by remember { mutableLongStateOf(updateInfo.apkSize) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    var selectedVariant by remember {
        mutableStateOf<ApkVariant?>(null)
    }

    fun startDownload(variant: ApkVariant?) {
        val apkVariant = variant ?: updateInfo.apkVariants.firstOrNull()

        if (apkVariant == null || apkVariant.downloadUrl.isBlank()) {
            val intent = Intent(Intent.ACTION_VIEW, updateInfo.downloadUrl.toUri())
            context.startActivity(intent)
            onDismiss()
            return
        }

        if (!UpdateDownloader.canInstallPackages(context)) {
            dialogState = UpdateDialogState.PERMISSION_REQUIRED
            return
        }

        totalBytes = apkVariant.size
        dialogState = UpdateDialogState.DOWNLOADING
        scope.launch {
            UpdateDownloader.downloadApk(
                context = context,
                downloadUrl = apkVariant.downloadUrl,
                fileName = apkVariant.fileName.ifBlank { "update.apk" }
            ).collect { state ->
                when (state) {
                    is DownloadState.Idle -> {
                        downloadProgress = 0
                    }

                    is DownloadState.Downloading -> {
                        downloadProgress = state.progress
                        downloadedBytes = state.downloadedBytes
                        totalBytes = state.totalBytes
                    }

                    is DownloadState.Success -> {
                        downloadedFile = state.file
                        dialogState = UpdateDialogState.DOWNLOAD_COMPLETE
                        onDownloadComplete()
                    }

                    is DownloadState.Error -> {
                        errorMessage = state.message
                        dialogState = UpdateDialogState.ERROR
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (dialogState != UpdateDialogState.DOWNLOADING) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = dialogState != UpdateDialogState.DOWNLOADING,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 600.dp)
                .padding(vertical = 16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            AnimatedContent(
                targetState = dialogState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "dialog_state"
            ) { state ->
                when (state) {
                    UpdateDialogState.INFO -> InfoContent(
                        updateInfo = updateInfo,
                        currentVersion = currentVersion,
                        selectedVariant = selectedVariant,
                        onVariantSelected = { selectedVariant = it },
                        onRemindLater = onRemindLater,
                        onDownloadVariant = { variant -> startDownload(variant) },
                        onSkipVersion = onSkipVersion
                    )

                    UpdateDialogState.DOWNLOADING -> DownloadingContent(
                        progress = downloadProgress,
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes
                    )

                    UpdateDialogState.DOWNLOAD_COMPLETE -> DownloadCompleteContent(
                        onInstall = {
                            downloadedFile?.let { file ->
                                UpdateDownloader.installApk(context, file)
                            }
                            onDismiss()
                        },
                        onDismiss = onDismiss
                    )

                    UpdateDialogState.ERROR -> ErrorContent(
                        errorMessage = errorMessage,
                        onRetry = { startDownload(selectedVariant) },
                        onDismiss = onDismiss,
                        onOpenBrowser = {
                            val intent = Intent(Intent.ACTION_VIEW, updateInfo.downloadUrl.toUri())
                            context.startActivity(intent)
                            onDismiss()
                        }
                    )

                    UpdateDialogState.PERMISSION_REQUIRED -> PermissionRequiredContent(
                        onGrantPermission = {
                            UpdateDownloader.openInstallPermissionSettings(context)
                        },
                        onStartDownload = {
                            startDownload(selectedVariant)
                        },
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}
