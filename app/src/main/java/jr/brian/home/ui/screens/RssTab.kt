package jr.brian.home.ui.screens

import android.content.Intent
import android.net.Uri
import android.text.Html
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayCircle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import jr.brian.home.model.rss.RssFeed
import jr.brian.home.model.rss.RssItem
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
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
    val selectedFeedUrls = uiState.selectedFeedUrls

    val itemsByFeed = remember(uiState.items) {
        uiState.items.groupBy { it.feedUrl }
    }

    val filteredItemsByFeed = remember(itemsByFeed, selectedFeedUrls) {
        if (selectedFeedUrls.isEmpty()) itemsByFeed
        else itemsByFeed.filterKeys { it in selectedFeedUrls }
    }

    val filteredFeeds = remember(uiState.feeds, selectedFeedUrls) {
        if (selectedFeedUrls.isEmpty()) uiState.feeds
        else uiState.feeds.filter { it.url in selectedFeedUrls }
    }

    LaunchedEffect(selectedFeedUrls) {
        if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
            listState.animateScrollToItem(0)
        }
    }

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RssFeed,
                        contentDescription = null,
                        tint = ThemePrimaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "RSS",
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
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    IconButton(
                        onClick = { viewModel.refreshAllFeeds() },
                        enabled = !uiState.isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh all",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (uiState.feeds.size > 1) {
                        Box {
                            IconButton(onClick = { showFilterMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter feeds",
                                    tint = if (selectedFeedUrls.isEmpty()) Color.White.copy(alpha = 0.7f)
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
                                            text = "All Feeds",
                                            color = if (selectedFeedUrls.isEmpty()) ThemePrimaryColor
                                                    else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = if (selectedFeedUrls.isEmpty()) FontWeight.Bold
                                                         else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = if (selectedFeedUrls.isEmpty()) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = ThemePrimaryColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    onClick = { viewModel.setSelectedFeedUrls(emptySet()) }
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
                            contentDescription = "Settings",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
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
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 4.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        filteredFeeds.forEach { feed ->
                            val feedItems = filteredItemsByFeed[feed.url]
                            if (!feedItems.isNullOrEmpty()) {
                                item(key = "header_${feed.url}") {
                                    FeedSectionHeader(feed = feed)
                                }
                                items(feedItems, key = { "${feed.url}_${it.id}" }) { item ->
                                    RssItemCard(
                                        item = item,
                                        onClick = {
                                            val target = item.link.ifEmpty { item.audioUrl }
                                            if (target.isNotEmpty()) {
                                                runCatching {
                                                    context.startActivity(
                                                        Intent(Intent.ACTION_VIEW, Uri.parse(target))
                                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    )
                                                }
                                            }
                                        },
                                        onVideoClick = {
                                            if (item.videoUrl.isNotEmpty()) {
                                                runCatching {
                                                    context.startActivity(
                                                        Intent(Intent.ACTION_VIEW, Uri.parse(item.videoUrl))
                                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    )
                                                }
                                            }
                                        },
                                        onAudioClick = {
                                            if (item.audioUrl.isNotEmpty()) {
                                                runCatching {
                                                    context.startActivity(
                                                        Intent(Intent.ACTION_VIEW, Uri.parse(item.audioUrl))
                                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    )
                                                }
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

@Composable
private fun FeedSectionHeader(feed: RssFeed) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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

    Box(
        modifier = Modifier
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
                    isVideo = !hasImage && hasVideo,
                    isAudio = hasAudio && !hasVideo,
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
                            contentDescription = "Play video",
                            tint = ThemeAccentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Video available",
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
                            contentDescription = "Play audio",
                            tint = ThemeAccentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Listen",
                            color = ThemeAccentColor,
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
                        text = formatPubDate(item.pubDate),
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
                    contentDescription = "Play video",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        if (isAudio) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(onClick = onAudioClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = "Play audio",
                    tint = Color.White.copy(alpha = 0.9f),
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
            Box(modifier = Modifier.fillMaxSize().background(Color.Transparent))
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
                text = "No RSS feeds",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Add feeds in Settings → RSS",
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
                text = "No articles yet",
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
                    text = "Refresh Now",
                    color = ThemePrimaryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun stripHtml(html: String): String {
    if (html.isBlank()) return ""
    return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
}

private fun formatPubDate(raw: String): String {
    return raw.take(30).trimEnd()
}
