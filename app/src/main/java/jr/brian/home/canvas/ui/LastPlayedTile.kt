package jr.brian.home.canvas.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jr.brian.home.R
import jr.brian.home.canvas.model.EsdeContentScale
import jr.brian.home.canvas.model.ResolvedCanvasItem
import jr.brian.home.data.RecentlyPlayedEntry
import jr.brian.home.model.rom.PinnedRomInfo
import jr.brian.home.model.rom.resolveDisplayPath
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalRecentlyPlayedManager
import java.io.File

/**
 * Last-played tile — Random button + up-to-N recent-game rows. Content binds
 * live to [jr.brian.home.data.RecentlyPlayedManager.entries]; recording new
 * launches reshuffles the list reactively. Rows and the Random button own
 * their own tap handlers; the canvas-level `onTap` is a no-op (see
 * `handleTap` in `UnifiedCanvasTab`).
 *
 * Gamepad: each interactive child ([RandomButton], each [EntryRow]) is
 * individually `focusable()`, so D-pad up/down walks them naturally — no
 * enter/exit focus state machine. Walking past the last row escapes the tile
 * downward rather than stopping (fine for the shared-canvas-grid case; a
 * tap/drag selection on the whole tile falls through to the outer slot).
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun LastPlayedTile(
    resolved: ResolvedCanvasItem.LastPlayed,
    onLaunch: (PinnedRomInfo) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    editMode: Boolean = false,
    onRowLongPress: (RecentlyPlayedEntry) -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }
    val manager = LocalRecentlyPlayedManager.current
    val entries by manager.entries.collectAsStateWithLifecycle()
    val visible = remember(entries, resolved.raw.resolvedCount) {
        entries.take(resolved.raw.resolvedCount)
    }

    val backgroundColor = resolved.raw.backgroundColorArgb?.let { Color(it) }
        ?: OledCardColor.copy(alpha = 0.85f)
    val shape = RoundedCornerShape(resolved.raw.resolvedBackgroundCornerRadiusDp.dp)

    val outerClickModifier = if (editMode) {
        Modifier.clickable(onClick = {})
    } else {
        Modifier.combinedClickable(onClick = {}, onLongClick = onLongPress)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (editMode) Modifier.border(3.dp, ThemePrimaryColor, shape) else Modifier
            )
            .then(outerClickModifier)
            .focusable()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (resolved.raw.resolvedShowRandomButton) {
                RandomButton(
                    enabled = visible.isNotEmpty() && !editMode,
                    onClick = { visible.randomOrNull()?.let { onLaunch(it.rom) } }
                )
                Spacer(Modifier.height(8.dp))
            }
            if (visible.isEmpty()) {
                EmptyState()
            } else {
                EntriesColumn(
                    entries = visible,
                    contentScale = resolved.raw.resolvedContentScale,
                    enabled = !editMode,
                    onLaunch = onLaunch,
                    onLongPress = onRowLongPress
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RandomButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ThemePrimaryColor.copy(alpha = 0.22f * alpha))
            .border(
                width = 1.dp,
                color = ThemePrimaryColor.copy(alpha = 0.6f * alpha),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .focusable(enabled = enabled)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Casino,
            contentDescription = null,
            tint = Color.White.copy(alpha = alpha),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.canvas_last_played_random),
            color = Color.White.copy(alpha = alpha),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun EntriesColumn(
    entries: List<RecentlyPlayedEntry>,
    contentScale: EsdeContentScale,
    enabled: Boolean,
    onLaunch: (PinnedRomInfo) -> Unit,
    onLongPress: (RecentlyPlayedEntry) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        entries.forEach { entry ->
            EntryRow(
                entry = entry,
                contentScale = contentScale,
                enabled = enabled,
                onClick = { onLaunch(entry.rom) },
                onLongClick = { onLongPress(entry) }
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(
    entry: RecentlyPlayedEntry,
    contentScale: EsdeContentScale,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val rom = entry.rom
    val artwork = remember(rom) {
        rom.resolveDisplayPath()?.let { File(it) }?.takeIf { it.exists() }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .focusable(enabled = enabled)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(OledCardColor.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            if (artwork != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(artwork)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = when (contentScale) {
                        EsdeContentScale.FIT -> ContentScale.Fit
                        EsdeContentScale.CROP -> ContentScale.Crop
                    },
                    filterQuality = FilterQuality.High
                )
            } else {
                Icon(
                    imageVector = Icons.Default.VideogameAsset,
                    contentDescription = null,
                    tint = ThemePrimaryColor.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxSize(0.6f)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = rom.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${rom.systemName} · ${relativeTime(entry.launchedAt)}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.canvas_last_played_empty),
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 13.sp
        )
    }
}

private fun relativeTime(epochMillis: Long): String =
    DateUtils.getRelativeTimeSpanString(
        epochMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()
