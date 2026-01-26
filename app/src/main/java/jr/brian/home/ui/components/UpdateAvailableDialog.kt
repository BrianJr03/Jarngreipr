package jr.brian.home.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import jr.brian.home.R
import jr.brian.home.model.state.UpdateDialogState
import jr.brian.home.util.ApkVariant
import jr.brian.home.util.DownloadState
import jr.brian.home.util.UpdateDownloader
import jr.brian.home.util.UpdateInfo
import jr.brian.home.util.VariantType
import kotlinx.coroutines.launch
import java.io.File
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

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

@Composable
private fun InfoContent(
    updateInfo: UpdateInfo,
    currentVersion: String,
    selectedVariant: ApkVariant?,
    onVariantSelected: (ApkVariant) -> Unit,
    onRemindLater: () -> Unit,
    onDownloadVariant: (ApkVariant) -> Unit,
    onSkipVersion: () -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.update_available_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(
                            R.string.update_version_format,
                            currentVersion,
                            updateInfo.latestVersion
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = {
                    scope.launch {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.update_scroll_down_description),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(16.dp))

        if (updateInfo.hasMultipleVariants) {
            Text(
                text = stringResource(R.string.update_choose_variant),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.update_variant_hidden_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                updateInfo.apkVariants.forEach { variant ->
                    VariantSelectionCard(
                        variant = variant,
                        isSelected = selectedVariant == variant,
                        onSelect = {
                            onVariantSelected(variant)
                            onDownloadVariant(variant)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        } else if (selectedVariant != null && selectedVariant.size > 0) {
            Text(
                text = stringResource(
                    R.string.update_download_size,
                    UpdateDownloader.formatBytes(selectedVariant.size)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (updateInfo.releaseNotes.isNotBlank()) {
            Text(
                text = stringResource(R.string.update_whats_new),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = updateInfo.releaseNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRemindLater,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.update_button_later))
            }

            OutlinedButton(
                onClick = onSkipVersion,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.update_button_skip))
            }
        }
    }
}

@Composable
private fun VariantSelectionCard(
    variant: ApkVariant,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val variantName = when (variant.variantType) {
        VariantType.STANDARD -> stringResource(R.string.update_variant_standard)
        VariantType.THOR -> stringResource(R.string.update_variant_hidden)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect,
                role = Role.RadioButton
            ),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = null
                )
                Column {
                    Text(
                        text = variantName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = variant.fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (variant.size > 0) {
                Text(
                    text = UpdateDownloader.formatBytes(variant.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DownloadingContent(
    progress: Int,
    downloadedBytes: Long,
    totalBytes: Long
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.update_downloading_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.update_downloading_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (progress >= 0) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(
                        R.string.update_progress_format,
                        UpdateDownloader.formatBytes(downloadedBytes),
                        UpdateDownloader.formatBytes(totalBytes)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.update_progress_percent, progress),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = UpdateDownloader.formatBytes(downloadedBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DownloadCompleteContent(
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.update_complete_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.update_complete_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.update_button_later))
            }

            Button(
                onClick = onInstall,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.update_button_install))
            }
        }
    }
}

@Composable
private fun ErrorContent(
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onOpenBrowser: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.update_failed_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.update_button_retry))
            }

            OutlinedButton(
                onClick = onOpenBrowser,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.update_button_download_browser))
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.update_button_cancel))
            }
        }
    }
}

@Composable
private fun PermissionRequiredContent(
    onGrantPermission: () -> Unit,
    onStartDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(UpdateDownloader.canInstallPackages(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = UpdateDownloader.canInstallPackages(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.SystemUpdate,
            contentDescription = null,
            tint = if (hasPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (hasPermission) {
                stringResource(R.string.update_permission_granted_title)
            } else {
                stringResource(R.string.update_permission_title)
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (hasPermission) {
                stringResource(R.string.update_permission_granted_message)
            } else {
                stringResource(R.string.update_permission_message)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hasPermission) {
                Button(
                    onClick = onStartDownload,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.update_button_start_download))
                }
            } else {
                Button(
                    onClick = onGrantPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.update_button_open_settings))
                }
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.update_button_cancel))
            }
        }
    }
}
