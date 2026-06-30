package jr.brian.home.canvas.ui.rss

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jr.brian.home.R
import jr.brian.home.model.rss.RssItem
import jr.brian.home.ui.screens.rss.NowPlayingDialog
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.util.computeRssVisibleItems
import jr.brian.home.viewmodels.RssViewModel

private val CompactArtworkThreshold = 110.dp
private val TitleRowHeight = 22.dp
private val TransportRowHeight = 44.dp

/**
 * Music-widget renderer for [jr.brian.home.canvas.model.CanvasItem.RssMusicItem].
 *
 * Resolves to the same [RssViewModel] instance the RSS tab uses (both call
 * `hiltViewModel()` at the activity scope), so the tile and the tab share
 * `NowPlayingManager` state — play/pause/skip on either surface reflects on
 * the other.
 *
 * The displayed item is `nowPlaying` when something is playing; otherwise the
 * first audio item in the tab's visible-ordered list (see
 * [computeRssVisibleItems]) so artwork is present before the user hits play.
 * Tap artwork opens the full [NowPlayingDialog] when something is playing, or
 * starts playback of the displayed item when nothing is.
 *
 * The playback queue is the same flat ordered list the tab uses, so
 * `skipToNext` / `skipToPrevious` traverse the tab's items in the tab's order.
 */
@Composable
internal fun CanvasRssMusicTileContent(
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    editMode: Boolean = false,
    viewModel: RssViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isMixedMode by viewModel.isMixedMode.collectAsStateWithLifecycle()
    val isAudioOnly by viewModel.isAudioOnly.collectAsStateWithLifecycle()
    val isHistoryMode by viewModel.isHistoryMode.collectAsStateWithLifecycle()
    val historyItemIds by viewModel.historyItemIds.collectAsStateWithLifecycle()
    val currentlyPlayingFeedUrl by viewModel.currentlyPlayingFeedUrl
        .collectAsStateWithLifecycle()
    val currentlyPlayingItemId by viewModel.currentlyPlayingItemId
        .collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val nowPlayingPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val nowPlayingDuration by viewModel.duration.collectAsStateWithLifecycle()
    val nowPlayingVolume by viewModel.volume.collectAsStateWithLifecycle()

    val visibleItems = remember(
        uiState.items,
        uiState.feeds,
        uiState.selectedFeedUrls,
        isAudioOnly,
        isMixedMode,
        isHistoryMode,
        historyItemIds,
        currentlyPlayingFeedUrl
    ) {
        computeRssVisibleItems(
            items = uiState.items,
            feeds = uiState.feeds,
            selectedFeedUrls = uiState.selectedFeedUrls,
            isAudioOnly = isAudioOnly,
            isMixedMode = isMixedMode,
            isHistoryMode = isHistoryMode,
            historyItemIds = historyItemIds,
            searchQuery = "",
            currentlyPlayingFeedUrl = currentlyPlayingFeedUrl
        )
    }
    val orderedQueue = visibleItems.flatOrdered

    val currentItem: RssItem? = remember(uiState.items, currentlyPlayingItemId, orderedQueue) {
        currentlyPlayingItemId?.let { id -> uiState.items.find { it.id == id } }
            ?: orderedQueue.firstOrNull { it.audioUrl.isNotEmpty() }
    }

    var showDialog by remember { mutableStateOf(false) }
    val info = nowPlaying
    if (showDialog && info != null) {
        NowPlayingDialog(
            info = info,
            volume = nowPlayingVolume,
            currentPosition = nowPlayingPosition,
            duration = nowPlayingDuration,
            onPlayPause = { viewModel.togglePlayPause() },
            onPrevious = { viewModel.skipToPrevious() },
            onNext = { viewModel.skipToNext() },
            onVolumeChange = { viewModel.setVolume(it) },
            onSeek = { viewModel.seekTo(it) },
            onDismiss = { showDialog = false }
        )
    }

    val isPlaying = info?.isPlaying == true
    val isBuffering = info?.isBuffering == true

    val playFirst: () -> Unit = {
        currentItem?.takeIf { it.audioUrl.isNotEmpty() }
            ?.let { viewModel.playAudio(it, orderedQueue) }
    }
    val onArtworkClick: () -> Unit = {
        if (info != null) showDialog = true else playFirst()
    }
    val onPlayPauseClick: () -> Unit = {
        if (info != null) viewModel.togglePlayPause() else playFirst()
    }
    val onPreviousClick: () -> Unit = {
        if (info != null) viewModel.skipToPrevious()
    }
    val onNextClick: () -> Unit = {
        if (info != null) viewModel.skipToNext()
    }

    MusicTileFrame(
        modifier = modifier,
        editMode = editMode,
        onEditTap = onTap,
        onLongPress = onLongPress
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxHeight < CompactArtworkThreshold
            val showTitle = !compact && maxHeight >= TitleRowHeight + TransportRowHeight + 40.dp
            val titleReserve = if (showTitle) TitleRowHeight else 0.dp
            val artworkHeight = (maxHeight - TransportRowHeight - titleReserve)
                .coerceAtLeast(0.dp)
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ArtworkBox(
                    imageUrl = currentItem?.imageUrl.orEmpty(),
                    interactionsEnabled = !editMode,
                    onClick = onArtworkClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(artworkHeight)
                )
                if (showTitle) {
                    Spacer(Modifier.height(4.dp))
                    TileTitle(
                        text = currentItem?.title
                            ?: stringResource(R.string.canvas_rss_music_empty)
                    )
                }
                Spacer(Modifier.height(4.dp))
                TransportRow(
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    interactionsEnabled = !editMode,
                    compact = compact,
                    onPrevious = onPreviousClick,
                    onPlayPause = onPlayPauseClick,
                    onNext = onNextClick
                )
            }
        }
    }
}

@Composable
private fun MusicTileFrame(
    modifier: Modifier,
    editMode: Boolean,
    onEditTap: () -> Unit,
    onLongPress: () -> Unit,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val frameModifier = modifier
        .fillMaxSize()
        .clip(shape)
        .background(OledCardColor.copy(alpha = 0.9f))
        .then(
            if (editMode) Modifier.border(
                width = 3.dp,
                color = ThemePrimaryColor,
                shape = shape
            ) else Modifier
        )
        .then(
            if (editMode) {
                Modifier.clickable(onClick = onEditTap)
            } else {
                Modifier.pointerInput(onLongPress) {
                    detectTapGestures(onLongPress = { onLongPress() })
                }
            }
        )
        .padding(8.dp)
    Box(modifier = frameModifier) {
        content()
    }
}

@Composable
private fun ArtworkBox(
    imageUrl: String,
    interactionsEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(OledBackgroundColor)
            .then(if (interactionsEnabled) Modifier.clickable(onClick = onClick) else Modifier)
            .focusable(enabled = interactionsEnabled),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.canvas_rss_music_artwork_cd),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = stringResource(R.string.canvas_rss_music_artwork_cd),
                tint = ThemePrimaryColor,
                modifier = Modifier.fillMaxSize(0.5f)
            )
        }
    }
}

@Composable
private fun TileTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    )
}

@Composable
private fun TransportRow(
    isPlaying: Boolean,
    isBuffering: Boolean,
    interactionsEnabled: Boolean,
    compact: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    val iconSide: Dp = if (compact) 20.dp else 24.dp
    val buttonSide: Dp = if (compact) 32.dp else 40.dp
    val playButtonSide: Dp = if (compact) 36.dp else 44.dp
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        TransportButton(
            icon = Icons.Default.SkipPrevious,
            contentDescription = stringResource(R.string.rss_tab_previous_cd),
            side = buttonSide,
            iconSide = iconSide,
            enabled = interactionsEnabled,
            onClick = onPrevious
        )
        PlayPauseButton(
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            side = playButtonSide,
            iconSide = iconSide,
            enabled = interactionsEnabled,
            onClick = onPlayPause
        )
        TransportButton(
            icon = Icons.Default.SkipNext,
            contentDescription = stringResource(R.string.rss_tab_next_cd),
            side = buttonSide,
            iconSide = iconSide,
            enabled = interactionsEnabled,
            onClick = onNext
        )
    }
}

@Composable
private fun TransportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    side: Dp,
    iconSide: Dp,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(side)
            .clip(CircleShape)
            .background(ThemePrimaryColor.copy(alpha = 0.15f))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .focusable(enabled = enabled),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = ThemePrimaryColor,
            modifier = Modifier.size(iconSide)
        )
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    side: Dp,
    iconSide: Dp,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(side)
            .clip(CircleShape)
            .background(ThemePrimaryColor)
            .then(
                if (enabled && !isBuffering) Modifier.clickable(onClick = onClick) else Modifier
            )
            .focusable(enabled = enabled),
        contentAlignment = Alignment.Center
    ) {
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.size(iconSide),
                color = OledBackgroundColor,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(
                    if (isPlaying) R.string.rss_tab_pause_cd else R.string.rss_tab_play_cd
                ),
                tint = OledBackgroundColor,
                modifier = Modifier.size(iconSide)
            )
        }
    }
}
