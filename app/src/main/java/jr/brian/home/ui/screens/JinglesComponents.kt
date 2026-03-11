package jr.brian.home.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import jr.brian.home.R
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.animatedGradientBorder
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.ui.extensions.clickWithHaptic
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.util.GitHubUrls
import kotlinx.coroutines.launch

@Composable
internal fun SectionHeader(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(
                    brush = Brush.verticalGradient(listOf(ThemePrimaryColor, ThemeSecondaryColor)),
                    shape = RoundedCornerShape(2.dp)
                )
        )
        Text(
            text = text.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.45f),
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
internal fun SearchRepoButton(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(brush = subtleCardGradient(isFocused), shape = RoundedCornerShape(12.dp))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                brush = borderBrush(isFocused),
                shape = RoundedCornerShape(12.dp)
            )
            .clickWithHaptic(haptic) { onClick() }
            .focusable()
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = stringResource(R.string.jingles_search_icon_description),
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
internal fun BrowseJinglesButton(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = 1.dp,
                brush = borderBrush(true),
                shape = RoundedCornerShape(8.dp)
            )
            .clickWithHaptic(haptic) { onClick() }
            .focusable()
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = stringResource(R.string.jingles_search_browse_button),
            color = ThemePrimaryColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
internal fun AddRepoButton(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(brush = subtleCardGradient(isFocused), shape = RoundedCornerShape(12.dp))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                brush = borderBrush(isFocused),
                shape = RoundedCornerShape(12.dp)
            )
            .clickWithHaptic(haptic) { onClick() }
            .focusable()
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.jingles_add_repo_description),
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
internal fun PickFolderButton(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(brush = subtleCardGradient(isFocused), shape = RoundedCornerShape(12.dp))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                brush = borderBrush(isFocused),
                shape = RoundedCornerShape(12.dp)
            )
            .clickWithHaptic(haptic) { onClick() }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = stringResource(R.string.jingles_pick_folder_description),
            tint = ThemePrimaryColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = stringResource(R.string.jingles_pick_folder_description),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
internal fun RepoCard(
    repo: String,
    jingleName: String?,
    jingleCount: Int?,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    downloadedFileCount: Int?,
    onRemove: () -> Unit,
    onDownload: () -> Unit,
    onStopDownload: () -> Unit,
    onFetchSizeBytes: suspend () -> Long?
) {
    var isFocused by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var sizeBytes by remember(isDownloaded) { mutableStateOf<Long?>(null) }
    var isFetchingSize by remember(isDownloaded) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val failedCount = if (jingleCount != null && downloadedFileCount != null)
        (jingleCount - downloadedFileCount).coerceAtLeast(0) else null

    if (showInfo) {
        JingleInfoDialog(
            title = jingleName?.takeIf { it.isNotBlank() } ?: repo.substringAfterLast("/"),
            count = jingleCount,
            sizeBytes = sizeBytes,
            isLoadingSize = isFetchingSize,
            failedCount = failedCount,
            repoUrl = "${GitHubUrls.WEB_BASE}/$repo",
            onDismiss = { showInfo = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(brush = subtleCardGradient(isFocused), shape = RoundedCornerShape(16.dp))
            .then(
                if (isDownloading)
                    Modifier.animatedGradientBorder(
                        colors = listOf(ThemePrimaryColor, ThemeSecondaryColor),
                        shape = RoundedCornerShape(16.dp)
                    )
                else
                    Modifier.border(
                        width = if (isFocused) 2.dp else 1.dp,
                        brush = Brush.linearGradient(
                            colors = if (isFocused) listOf(ThemePrimaryColor, ThemeSecondaryColor)
                            else listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.07f))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .focusable()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = ThemePrimaryColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = ThemePrimaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val title = jingleName?.takeIf { it.isNotBlank() } ?: repo.substringAfterLast("/")
                    Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = repo, color = Color.Gray, fontSize = 13.sp)
                }
                IconButton(
                    onClick = {
                        showInfo = true
                        if (sizeBytes == null && !isFetchingSize) {
                            isFetchingSize = true
                            scope.launch {
                                sizeBytes = onFetchSizeBytes()
                                isFetchingSize = false
                            }
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.jingles_pack_info_icon_description),
                        tint = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = { if (isDownloading) onStopDownload() else onDownload() },
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isDownloading) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = stringResource(R.string.jingles_download_description),
                            tint = ThemeSecondaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (isDownloaded) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = stringResource(R.string.jingles_update_description),
                            tint = ThemePrimaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = stringResource(R.string.jingles_download_description),
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.jingles_remove_repo_description, repo),
                        tint = ThemeSecondaryColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (isDownloading) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = ThemePrimaryColor,
                    trackColor = ThemePrimaryColor.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
internal fun FolderCard(
    uriString: String,
    jingleName: String?,
    jingleCount: Int?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onRemove: () -> Unit,
    onFetchSizeBytes: suspend () -> Long
) {
    var isFocused by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var sizeBytes by remember { mutableStateOf<Long?>(null) }
    var isFetchingSize by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val displayName = remember(uriString) {
        DocumentFile.fromTreeUri(context, uriString.toUri())?.name
            ?: uriString.toUri().lastPathSegment
                ?.substringAfterLast(":")
                ?.substringAfterLast("/")
            ?: uriString
    }

    if (showInfo) {
        JingleInfoDialog(
            title = jingleName?.takeIf { it.isNotBlank() } ?: displayName,
            count = jingleCount,
            sizeBytes = sizeBytes,
            isLoadingSize = isFetchingSize,
            onDismiss = { showInfo = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(brush = subtleCardGradient(isFocused), shape = RoundedCornerShape(16.dp))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                brush = Brush.linearGradient(
                    colors = if (isFocused) listOf(ThemePrimaryColor, ThemeSecondaryColor)
                    else listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.07f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .focusable()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = ThemeAccentColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = ThemeAccentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                val title = jingleName?.takeIf { it.isNotBlank() } ?: displayName
                Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (jingleName?.isNotBlank() == true) {
                    Text(text = displayName, color = Color.Gray, fontSize = 13.sp)
                }
            }
            IconButton(
                onClick = {
                    showInfo = true
                    if (sizeBytes == null && !isFetchingSize) {
                        isFetchingSize = true
                        scope.launch {
                            sizeBytes = onFetchSizeBytes()
                            isFetchingSize = false
                        }
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.jingles_pack_info_icon_description),
                    tint = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = { if (!isRefreshing) onRefresh() },
                modifier = Modifier.size(36.dp)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = ThemePrimaryColor
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = stringResource(R.string.jingles_refresh_folder_description),
                        tint = ThemePrimaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.jingles_remove_folder_description, displayName),
                    tint = ThemeSecondaryColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun JingleInfoDialog(
    title: String,
    count: Int?,
    sizeBytes: Long?,
    isLoadingSize: Boolean,
    failedCount: Int? = null,
    repoUrl: String? = null,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledCardColor,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = { Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (count != null) {
                    Text(
                        text = stringResource(R.string.jingles_pack_info_tracks, count),
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
                if (failedCount != null && failedCount > 0) {
                    Text(
                        text = stringResource(R.string.jingles_pack_info_failed, failedCount),
                        fontSize = 15.sp,
                        color = ThemeSecondaryColor
                    )
                }
                when {
                    isLoadingSize -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = ThemePrimaryColor
                        )
                        Text(
                            text = stringResource(R.string.jingles_pack_info_fetching_size),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                    sizeBytes != null && sizeBytes > 0 -> Text(
                        text = stringResource(R.string.jingles_pack_info_size, formatFileSize(sizeBytes)),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                if (repoUrl != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = ThemePrimaryColor.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .focusable()
                            .clickWithHaptic(haptic) { uriHandler.openUri(repoUrl) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = repoUrl,
                            color = ThemePrimaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = ThemePrimaryColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.jingles_info_close),
                    color = ThemePrimaryColor
                )
            }
        }
    )
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_000_000_000L -> "%.1f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000L -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000L -> "%.1f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}
