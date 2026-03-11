package jr.brian.home.ui.screens

import android.net.Uri
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import jr.brian.home.R
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.ui.extensions.clickWithHaptic
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

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
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onRemove: () -> Unit,
    onDownload: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

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
                onClick = { if (!isDownloading) onDownload() },
                modifier = Modifier.size(32.dp)
            ) {
                when {
                    isDownloading -> CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = ThemePrimaryColor
                    )
                    isDownloaded -> Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = stringResource(R.string.jingles_update_description),
                        tint = ThemePrimaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                    else -> Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = stringResource(R.string.jingles_download_description),
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.jingles_remove_repo_description, repo),
                    tint = ThemeSecondaryColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
internal fun FolderCard(uriString: String, jingleName: String?, isRefreshing: Boolean, onRefresh: () -> Unit, onRemove: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val displayName = remember(uriString) {
        DocumentFile.fromTreeUri(context, uriString.toUri())?.name
            ?: Uri.parse(uriString).lastPathSegment
                ?.substringAfterLast(":")
                ?.substringAfterLast("/")
            ?: uriString
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
                onClick = { if (!isRefreshing) onRefresh() },
                modifier = Modifier.size(32.dp)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = ThemePrimaryColor
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = stringResource(R.string.jingles_refresh_folder_description),
                        tint = ThemePrimaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.jingles_remove_folder_description, displayName),
                    tint = ThemeSecondaryColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
