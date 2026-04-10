package jr.brian.home.ui.screens

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.ui.components.NotificationShade
import jr.brian.home.ui.components.QwertyKeyboard
import jr.brian.home.ui.screens.rss.EmptyRssState
import jr.brian.home.ui.screens.rss.RssListKeys
import jr.brian.home.ui.screens.rss.FeedSectionHeader
import jr.brian.home.ui.screens.rss.NoItemsState
import jr.brian.home.ui.screens.rss.NowPlayingBubble
import jr.brian.home.ui.screens.rss.NowPlayingDialog
import jr.brian.home.ui.screens.rss.RssItemCard
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.util.parsePubDateMillis
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

    val currentMixedFeedTitle by remember(mixedItems, currentlyPlayingItem, uiState.feeds) {
        derivedStateOf {
            val mixedItemsStartIndex = if (currentlyPlayingItem != null) 3 else 1
            val itemIndex = (listState.firstVisibleItemIndex - mixedItemsStartIndex).coerceAtLeast(0)
            mixedItems.getOrNull(itemIndex)?.feedUrl?.let { url ->
                uiState.feeds.find { it.url == url }?.title
            }
        }
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                                text = stringResource(R.string.rss_tab_mixed),
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
                                contentDescription = if (isKeyboardVisible) stringResource(R.string.rss_tab_hide_search_cd) else stringResource(R.string.rss_tab_search_feeds_cd),
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
                                    text = stringResource(R.string.rss_tab_filtering_by, searchQuery),
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
                                        contentDescription = stringResource(R.string.rss_tab_clear_search_cd),
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
                                stickyHeader(key = RssListKeys.NOW_PLAYING_HEADER) {
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
                                            text = stringResource(R.string.rss_tab_now_playing),
                                            color = ThemePrimaryColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                item(key = RssListKeys.NOW_PLAYING_PINNED) {
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
                                stickyHeader(key = RssListKeys.MIXED_HEADER) {
                                    val mixedLabel = stringResource(R.string.rss_tab_mixed)
                                    val headerText = currentMixedFeedTitle
                                        ?.let { "$mixedLabel \u2022 $it" }
                                        ?: mixedLabel
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
                                            text = headerText,
                                            color = ThemePrimaryColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                items(
                                    items = mixedItems,
                                    key = { RssListKeys.mixedItem(it.feedUrl, it.id) }
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
                                        stickyHeader(key = RssListKeys.feedHeader(feed.url)) {
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
                                            key = { RssListKeys.feedItem(feed.url, it.id) }
                                        ) { item ->
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
                                        item(key = RssListKeys.feedSpacer(feed.url)) {
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
