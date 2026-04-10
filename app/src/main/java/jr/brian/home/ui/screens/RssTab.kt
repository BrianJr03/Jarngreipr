package jr.brian.home.ui.screens

import android.net.Uri
import android.text.Html
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import jr.brian.home.R
import jr.brian.home.data.NowPlayingManager
import jr.brian.home.model.rss.RssFeed
import jr.brian.home.model.rss.RssItem
import jr.brian.home.ui.colors.animatedGradientBorder
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.ui.components.NotificationShade
import jr.brian.home.ui.components.QwertyKeyboard
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.util.animatedColor
import jr.brian.home.ui.util.rememberTopFlingTrigger
import jr.brian.home.viewmodels.RssViewModel

@Composable
fun RssTab(
    onSettingsClick: () -> Unit,
    viewModel: RssViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var showFilterMenu by remember { mutableStateOf(false) }
    val isMixedMode by viewModel.isMixedMode.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var isKeyboardVisible by remember { mutableStateOf(false) }
    val keyboardFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
    val selectedFeedUrls = uiState.selectedFeedUrls

    val itemsByFeed = remember(uiState.items) {
        uiState.items.groupBy { it.feedUrl }
    }

    val filteredItemsByFeed = remember(itemsByFeed, selectedFeedUrls, searchQuery) {
        val feedFiltered = if (selectedFeedUrls.isEmpty()) itemsByFeed
        else itemsByFeed.filterKeys { it in selectedFeedUrls }
        if (searchQuery.isBlank()) feedFiltered
        else feedFiltered.mapValues { (_, items) ->
            items.filter { item ->
                item.title.contains(searchQuery, ignoreCase = true) ||
                        item.description.contains(searchQuery, ignoreCase = true)
            }
        }.filterValues { it.isNotEmpty() }
    }

    val currentlyPlayingFeedUrl: String? by viewModel.currentlyPlayingFeedUrl.collectAsStateWithLifecycle()
    val currentlyPlayingItemId: String? by viewModel.currentlyPlayingItemId.collectAsStateWithLifecycle()

    val currentlyPlayingItem = remember(uiState.items, currentlyPlayingItemId) {
        currentlyPlayingItemId?.let { id -> uiState.items.find { it.id == id } }
    }

    val mixedItems = remember(filteredItemsByFeed, isMixedMode) {
        if (!isMixedMode) emptyList()
        else {
            val lists = filteredItemsByFeed.values.toList()
            val maxSize = lists.maxOfOrNull { it.size } ?: 0
            buildList {
                for (i in 0 until maxSize) {
                    lists.forEach { feedItems -> feedItems.getOrNull(i)?.let { add(it) } }
                }
            }.sortedByDescending { parsePubDateMillis(it.pubDate) }
        }
    }

    val filteredFeeds =
        remember(uiState.feeds, selectedFeedUrls, currentlyPlayingFeedUrl, filteredItemsByFeed) {
            val base = if (selectedFeedUrls.isEmpty()) uiState.feeds
            else uiState.feeds.filter { it.url in selectedFeedUrls }
            val withItems = base.filter { it.url in filteredItemsByFeed }
            val playingUrl = currentlyPlayingFeedUrl ?: return@remember withItems
            val playing = withItems.find { it.url == playingUrl } ?: return@remember withItems
            listOf(playing) + withItems.filter { it.url != playingUrl }
        }

    LaunchedEffect(selectedFeedUrls) {
        if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
            listState.animateScrollToItem(0)
        }
    }

    var playingVideoUrl by remember { mutableStateOf<String?>(null) }
    playingVideoUrl?.let { url ->
        RssVideoPlayerDialog(url = url, onDismiss = { playingVideoUrl = null })
    }

    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val nowPlayingVolume by viewModel.volume.collectAsStateWithLifecycle()
    val nowPlayingPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val nowPlayingDuration by viewModel.duration.collectAsStateWithLifecycle()
    val gridSettingsManager = LocalGridSettingsManager.current
    var showNowPlayingDialog by remember { mutableStateOf(false) }
    var showNotificationShade by remember { mutableStateOf(false) }

    val topFlingTrigger = rememberTopFlingTrigger(listState) {
        if (gridSettingsManager.notificationShadeEnabled) showNotificationShade = true
    }

    nowPlaying?.let { info ->
        if (showNowPlayingDialog) {
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
                onDismiss = { showNowPlayingDialog = false }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBackgroundColor)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(.8f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RssFeed,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.rss_tab_title),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = ThemePrimaryColor,
                            strokeWidth = 2.dp
                        )
                    }
                }

                val info = nowPlaying
                Box(
                    modifier = Modifier.weight(1.2f),
                    contentAlignment = Alignment.Center
                ) {
                    if (info != null) {
                        NowPlayingBubble(
                            title = info.title,
                            onPrevious = { viewModel.skipToPrevious() },
                            onNext = { viewModel.skipToNext() },
                            onClick = { showNowPlayingDialog = true }
                        )
                    }
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { viewModel.refreshAllFeeds() },
                        enabled = !uiState.isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.rss_tab_refresh_cd),
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (uiState.feeds.size > 1) {
                        Box {
                            IconButton(onClick = { showFilterMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.rss_tab_filter_cd),
                                    tint = if (selectedFeedUrls.isEmpty() && !isMixedMode) Color.White.copy(alpha = 0.7f)
                                    else ThemePrimaryColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false },
                                offset = DpOffset(x = 0.dp, y = (-4).dp),
                                modifier = Modifier.background(
                                    Color(0xFF1A1A1A),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.rss_tab_all_feeds),
                                            color = if (selectedFeedUrls.isEmpty() && !isMixedMode) ThemePrimaryColor
                                            else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = if (selectedFeedUrls.isEmpty() && !isMixedMode) FontWeight.Bold
                                            else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = if (selectedFeedUrls.isEmpty() && !isMixedMode) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = ThemePrimaryColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        viewModel.setMixedMode(false)
                                        viewModel.setSelectedFeedUrls(emptySet())
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Mixed",
                                            color = if (isMixedMode) ThemePrimaryColor else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = if (isMixedMode) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = if (isMixedMode) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = ThemePrimaryColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        viewModel.setMixedMode(true)
                                        showFilterMenu = false
                                    }
                                )
                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.08f),
                                    thickness = 1.dp
                                )
                                uiState.feeds.forEach { feed ->
                                    val isSelected = feed.url in selectedFeedUrls
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = feed.title,
                                                color = if (isSelected) ThemePrimaryColor
                                                else Color.White.copy(alpha = 0.85f),
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold
                                                else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = ThemePrimaryColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            viewModel.setMixedMode(false)
                                            val next = if (isSelected) {
                                                selectedFeedUrls - feed.url
                                            } else {
                                                selectedFeedUrls + feed.url
                                            }
                                            viewModel.setSelectedFeedUrls(next)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.rss_tab_settings_cd),
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(onClick = { isKeyboardVisible = !isKeyboardVisible }) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = if (isKeyboardVisible) "Hide search" else "Search feeds",
                            tint = if (isKeyboardVisible || searchQuery.isNotEmpty()) ThemePrimaryColor
                            else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isKeyboardVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    QwertyKeyboard(
                        searchQuery = searchQuery,
                        showFlipLayoutButton = false,
                        showVolControl = false,
                        showSettings = false,
                        showController = false,
                        showAtKey = false,
                        keyboardFocusRequesters = keyboardFocusRequesters,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    if (searchQuery.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Filtering by: \"$searchQuery\"",
                                color = ThemePrimaryColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            when {
                uiState.feeds.isEmpty() -> EmptyRssState()
                uiState.items.isEmpty() && !uiState.isRefreshing -> NoItemsState(
                    onRefresh = { viewModel.refreshAllFeeds() }
                )

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(topFlingTrigger),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 4.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        currentlyPlayingItem?.let { playingItem ->
                            stickyHeader(key = "now_playing_header") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(OledBackgroundColor)
                                        .padding(horizontal = 4.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(ThemePrimaryColor, CircleShape)
                                    )
                                    Text(
                                        text = "Now Playing",
                                        color = ThemePrimaryColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            item(key = "now_playing_pinned") {
                                RssItemCard(
                                    item = playingItem,
                                    isCurrentlyPlaying = true,
                                    modifier = Modifier.animateItem(),
                                    useDMYDateFormat = uiState.useDMYDateFormat,
                                    use24HourClock = uiState.use24HourClock,
                                    onClick = { showNowPlayingDialog = true },
                                    onVideoClick = {
                                        if (playingItem.videoUrl.isNotEmpty()) {
                                            playingVideoUrl = playingItem.videoUrl
                                        }
                                    },
                                    onAudioClick = { showNowPlayingDialog = true }
                                )
                            }
                        }

                        if (isMixedMode) {
                            items(
                                items = mixedItems,
                                key = { "mixed_${it.feedUrl}_${it.id}" }
                            ) { item ->
                                RssItemCard(
                                    item = item,
                                    isCurrentlyPlaying = item.id == currentlyPlayingItemId,
                                    modifier = Modifier.animateItem(),
                                    useDMYDateFormat = uiState.useDMYDateFormat,
                                    use24HourClock = uiState.use24HourClock,
                                    onClick = {
                                        when {
                                            item.id == currentlyPlayingItemId -> showNowPlayingDialog = true
                                            item.link.isNotEmpty() -> runCatching {
                                                CustomTabsIntent.Builder()
                                                    .build()
                                                    .launchUrl(context, Uri.parse(item.link))
                                            }
                                            item.audioUrl.isNotEmpty() -> viewModel.playAudio(item)
                                        }
                                    },
                                    onVideoClick = {
                                        if (item.videoUrl.isNotEmpty()) playingVideoUrl = item.videoUrl
                                    },
                                    onAudioClick = {
                                        if (item.id == currentlyPlayingItemId) showNowPlayingDialog = true
                                        else if (item.audioUrl.isNotEmpty()) viewModel.playAudio(item)
                                    }
                                )
                            }
                        } else {
                            filteredFeeds.forEach { feed ->
                                val feedItems = filteredItemsByFeed[feed.url]
                                if (!feedItems.isNullOrEmpty()) {
                                    stickyHeader(key = "header_${feed.url}") {
                                        FeedSectionHeader(
                                            feed = feed,
                                            isPlaying = feed.url == currentlyPlayingFeedUrl,
                                            onClick = {
                                                if (feed.url == currentlyPlayingFeedUrl) {
                                                    showNowPlayingDialog = true
                                                }
                                            },
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                    items(
                                        items = feedItems,
                                        key = { "${feed.url}_${it.id}" }) { item ->
                                        RssItemCard(
                                            item = item,
                                            isCurrentlyPlaying = item.id == currentlyPlayingItemId,
                                            modifier = Modifier.animateItem(),
                                            useDMYDateFormat = uiState.useDMYDateFormat,
                                            use24HourClock = uiState.use24HourClock,
                                            onClick = {
                                                when {
                                                    item.id == currentlyPlayingItemId -> showNowPlayingDialog =
                                                        true

                                                    item.link.isNotEmpty() -> runCatching {
                                                        CustomTabsIntent.Builder()
                                                            .build()
                                                            .launchUrl(context, Uri.parse(item.link))
                                                    }

                                                    item.audioUrl.isNotEmpty() -> viewModel.playAudio(
                                                        item
                                                    )
                                                }
                                            },
                                            onVideoClick = {
                                                if (item.videoUrl.isNotEmpty()) {
                                                    playingVideoUrl = item.videoUrl
                                                }
                                            },
                                            onAudioClick = {
                                                if (item.id == currentlyPlayingItemId) {
                                                    showNowPlayingDialog = true
                                                } else if (item.audioUrl.isNotEmpty()) {
                                                    viewModel.playAudio(item)
                                                }
                                            }
                                        )
                                    }
                                    item(key = "spacer_${feed.url}") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    NotificationShade(
        visible = showNotificationShade,
        nowPlaying = nowPlaying,
        currentPosition = nowPlayingPosition,
        duration = nowPlayingDuration,
        volume = nowPlayingVolume,
        onPlayPause = { viewModel.togglePlayPause() },
        onPrevious = { viewModel.skipToPrevious() },
        onNext = { viewModel.skipToNext() },
        onVolumeChange = { viewModel.setVolume(it) },
        onSeek = { viewModel.seekTo(it) },
        onDismiss = { showNotificationShade = false }
    )
    }
}

@Composable
private fun FeedSectionHeader(
    feed: RssFeed,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(OledBackgroundColor)
            .then(if (isPlaying) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(ThemePrimaryColor, CircleShape)
        )
        Text(
            text = feed.title,
            color = ThemePrimaryColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .height(1.dp)
                .weight(0.3f)
                .background(
                    ThemePrimaryColor.copy(alpha = 0.2f),
                    RoundedCornerShape(1.dp)
                )
        )
    }
}

@Composable
private fun RssItemCard(
    item: RssItem,
    isCurrentlyPlaying: Boolean,
    modifier: Modifier = Modifier,
    useDMYDateFormat: Boolean,
    use24HourClock: Boolean,
    onClick: () -> Unit,
    onVideoClick: () -> Unit,
    onAudioClick: () -> Unit
) {
    val hasImage = item.imageUrl.isNotEmpty()
    val hasVideo = item.videoUrl.isNotEmpty()
    val hasAudio = item.audioUrl.isNotEmpty()
    val plainDescription = remember(item.description) {
        stripHtml(item.description).trim()
    }
    val animColor = animatedColor()
    val audioIconColor = if (isCurrentlyPlaying) animColor else ThemeAccentColor
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(brush = subtleCardGradient(false), shape = RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                brush = borderBrush(isFocused = false),
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column {
            if (hasImage || hasVideo) {
                val mediaUrl = if (hasImage) item.imageUrl else item.videoUrl
                MediaThumbnail(
                    url = mediaUrl,
                    isVideo = !hasImage,
                    isAudio = hasAudio && !hasVideo,
                    isCurrentlyPlaying = isCurrentlyPlaying,
                    onVideoClick = onVideoClick,
                    onAudioClick = onAudioClick
                )
            }

            Column(
                modifier = Modifier.padding(
                    start = 14.dp,
                    end = 14.dp,
                    top = if (hasImage || hasVideo) 10.dp else 14.dp,
                    bottom = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (hasVideo && hasImage) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable(onClick = onVideoClick)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = stringResource(R.string.rss_tab_play_video_cd),
                            tint = ThemeAccentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.rss_tab_video_available),
                            color = ThemeAccentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (hasAudio && !hasVideo && !hasImage) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable(onClick = onAudioClick)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = stringResource(R.string.rss_tab_play_audio_cd),
                            tint = audioIconColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.rss_tab_listen),
                            color = audioIconColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                if (plainDescription.isNotEmpty()) {
                    Text(
                        text = plainDescription,
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }

                if (item.pubDate.isNotEmpty()) {
                    Text(
                        text = formatPubDate(item.pubDate, useDMYDateFormat, use24HourClock),
                        color = ThemeSecondaryColor.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaThumbnail(
    url: String,
    isVideo: Boolean,
    isAudio: Boolean,
    isCurrentlyPlaying: Boolean = false,
    onVideoClick: () -> Unit,
    onAudioClick: () -> Unit
) {
    val context = LocalContext.current
    var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

    val imageAlpha by animateFloatAsState(
        targetValue = if (imageState is AsyncImagePainter.State.Success) 1f else 0f,
        animationSpec = tween(300),
        label = "image_fade"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .background(Color.White.copy(alpha = 0.06f))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onState = { imageState = it },
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (imageAlpha < 1f) Modifier else Modifier
                )
        )

        if (isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(onClick = onVideoClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = stringResource(R.string.rss_tab_play_video_cd),
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        if (isAudio) {
            val thumbnailAudioColor = animatedColor(
                firstSeen = isCurrentlyPlaying,
                fallbackColor = Color.White.copy(alpha = 0.9f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(onClick = onAudioClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = stringResource(R.string.rss_tab_play_audio_cd),
                    tint = thumbnailAudioColor,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = imageState is AsyncImagePainter.State.Loading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = ThemePrimaryColor.copy(alpha = 0.4f),
                    strokeWidth = 2.dp
                )
            }
        }

        if (imageState is AsyncImagePainter.State.Error) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            )
        }
    }
}

@Composable
private fun EmptyRssState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.RssFeed,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(72.dp)
            )
            Text(
                text = stringResource(R.string.rss_tab_empty_title),
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.rss_tab_empty_hint),
                color = Color.White.copy(alpha = 0.2f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun NoItemsState(onRefresh: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.RssFeed,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(72.dp)
            )
            Text(
                text = stringResource(R.string.rss_tab_no_articles),
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(ThemePrimaryColor.copy(alpha = 0.15f))
                    .clickable(onClick = onRefresh)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.rss_tab_refresh_now),
                    color = ThemePrimaryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
internal fun NowPlayingBubble(
    title: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ThemePrimaryColor.copy(alpha = 0.12f))
            .animatedGradientBorder(
                shape = RoundedCornerShape(20.dp),
                borderWidth = 1.dp,
                durationMs = 2500
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.SkipPrevious,
            contentDescription = "Previous",
            tint = ThemePrimaryColor,
            modifier = Modifier
                .size(18.dp)
                .clickable(onClick = onPrevious)
        )
        Icon(
            imageVector = Icons.Default.Headphones,
            contentDescription = null,
            tint = ThemePrimaryColor,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = title,
            color = ThemePrimaryColor,
            fontSize = 11.sp,
            maxLines = 1,
            modifier = Modifier.basicMarquee()
        )
        Icon(
            imageVector = Icons.Default.SkipNext,
            contentDescription = "Next",
            tint = ThemePrimaryColor,
            modifier = Modifier
                .size(18.dp)
                .clickable(onClick = onNext)
        )
    }
}

@Composable
internal fun NowPlayingDialog(
    info: NowPlayingManager.NowPlayingInfo,
    volume: Float,
    currentPosition: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableStateOf(0f) }

    val progressFraction = if (!isSeeking) {
        if (duration > 0L) currentPosition.toFloat() / duration.toFloat() else 0f
    } else {
        seekValue
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1A1A))
                .border(1.dp, ThemePrimaryColor.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = ThemePrimaryColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Now Playing",
                        color = ThemePrimaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = info.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee()
                    )
                    if (info.artist != null) {
                        Text(
                            text = info.artist,
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Progress bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.Slider(
                        value = progressFraction,
                        onValueChange = { value ->
                            isSeeking = true
                            seekValue = value
                        },
                        onValueChangeFinished = {
                            if (duration > 0L) onSeek((seekValue * duration).toLong())
                            isSeeking = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = ThemePrimaryColor,
                            activeTrackColor = ThemePrimaryColor,
                            inactiveTrackColor = ThemePrimaryColor.copy(alpha = 0.25f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val displayPosition = if (isSeeking && duration > 0L) {
                            (seekValue * duration).toLong()
                        } else {
                            currentPosition
                        }
                        Text(
                            text = formatMs(displayPosition),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = formatMs(duration),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ThemePrimaryColor.copy(alpha = 0.15f))
                            .clickable(onClick = onPrevious),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = ThemePrimaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ThemePrimaryColor)
                            .clickable(onClick = onPlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (info.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (info.isPlaying) "Pause" else "Play",
                            tint = OledBackgroundColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ThemePrimaryColor.copy(alpha = 0.15f))
                            .clickable(onClick = onNext),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = ThemePrimaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Volume slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Volume",
                        tint = ThemePrimaryColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                    androidx.compose.material3.Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = ThemePrimaryColor,
                            activeTrackColor = ThemePrimaryColor,
                            inactiveTrackColor = ThemePrimaryColor.copy(alpha = 0.25f)
                        )
                    )
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0L)
    val hours = totalSec / 3600
    val min = (totalSec % 3600) / 60
    val sec = totalSec % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, min, sec)
    else "%d:%02d".format(min, sec)
}

private fun stripHtml(html: String): String {
    if (html.isBlank()) return ""
    return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
}

private val pubDateFormats = listOf(
    "EEE, dd MMM yyyy HH:mm:ss Z",
    "EEE, dd MMM yyyy HH:mm:ss z",
    "yyyy-MM-dd'T'HH:mm:ssZ",
    "yyyy-MM-dd'T'HH:mm:ss'Z'",
    "yyyy-MM-dd'T'HH:mm:ssz",
    "dd MMM yyyy HH:mm:ss Z"
)

private fun parsePubDateMillis(raw: String): Long {
    if (raw.isBlank()) return 0L
    for (fmt in pubDateFormats) {
        runCatching {
            val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.ENGLISH)
            sdf.isLenient = false
            return sdf.parse(raw.trim())!!.time
        }
    }
    return 0L
}

private fun formatPubDate(raw: String, useDMY: Boolean, use24Hour: Boolean): String {
    if (raw.isBlank()) return ""
    val inputFormats = pubDateFormats
    val datePattern = if (useDMY) "d/M/yyyy" else "M/d/yyyy"
    val timePattern = if (use24Hour) "HH:mm" else "h:mm a"
    val output =
        java.text.SimpleDateFormat("$datePattern @ $timePattern", java.util.Locale.getDefault())
    for (fmt in inputFormats) {
        runCatching {
            val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.ENGLISH)
            sdf.isLenient = false
            return output.format(sdf.parse(raw.trim())!!)
        }
    }
    return raw.take(30).trimEnd()
}
