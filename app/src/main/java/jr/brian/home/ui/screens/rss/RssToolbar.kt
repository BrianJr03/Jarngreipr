package jr.brian.home.ui.screens.rss

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.data.NowPlayingManager
import jr.brian.home.model.rss.RssFeed
import jr.brian.home.ui.theme.ThemePrimaryColor

@Composable
fun RssToolbar(
    feeds: List<RssFeed>,
    nowPlaying: NowPlayingManager.NowPlayingInfo?,
    selectedFeedUrls: Set<String>,
    isRefreshing: Boolean,
    isMixedMode: Boolean,
    isHistoryMode: Boolean,
    isAudioOnly: Boolean,
    isKeyboardVisible: Boolean,
    searchQuery: String,
    onMixedModeChange: (Boolean) -> Unit,
    onHistoryModeChange: (Boolean) -> Unit,
    onFeedFilterChange: (Set<String>) -> Unit,
    onAudioOnlyChange: (Boolean) -> Unit,
    onKeyboardToggle: () -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    onNowPlayingClick: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RssTitleSection(isRefreshing = isRefreshing)

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (nowPlaying != null) {
                NowPlayingBubble(
                    title = nowPlaying.title,
                    isBuffering = nowPlaying.isBuffering,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onClick = onNowPlayingClick
                )
            }
        }

        if (feeds.size > 1) {
            RssFeedFilterMenu(
                feeds = feeds,
                selectedFeedUrls = selectedFeedUrls,
                isMixedMode = isMixedMode,
                isHistoryMode = isHistoryMode,
                onMixedModeChange = onMixedModeChange,
                onHistoryModeChange = onHistoryModeChange,
                onFeedFilterChange = onFeedFilterChange
            )
        }

        AudioOnlyToggle(
            isAudioOnly = isAudioOnly,
            onToggle = onAudioOnlyChange
        )

        RssOptionsMenu(
            isKeyboardVisible = isKeyboardVisible,
            searchQuery = searchQuery,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            onKeyboardToggle = onKeyboardToggle,
            onSettingsClick = onSettingsClick
        )
    }
}

@Composable
private fun RssTitleSection(isRefreshing: Boolean) {
    Row(
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
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = ThemePrimaryColor,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun AudioOnlyToggle(
    isAudioOnly: Boolean,
    onToggle: (Boolean) -> Unit
) {
    AnimatedVisibility(isAudioOnly) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(ThemePrimaryColor.copy(alpha = 0.15f))
                .clickable { onToggle(false) }
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = stringResource(R.string.rss_tab_audio_only_cd),
                tint = ThemePrimaryColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = stringResource(R.string.rss_tab_audio_only),
                color = ThemePrimaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    AnimatedVisibility(!isAudioOnly) {
        IconButton(onClick = { onToggle(true) }) {
            Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = stringResource(R.string.rss_tab_audio_only_cd),
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun RssFeedFilterMenu(
    feeds: List<RssFeed>,
    selectedFeedUrls: Set<String>,
    isMixedMode: Boolean,
    isHistoryMode: Boolean,
    onMixedModeChange: (Boolean) -> Unit,
    onHistoryModeChange: (Boolean) -> Unit,
    onFeedFilterChange: (Set<String>) -> Unit,
) {
    val isDefaultMode = selectedFeedUrls.isEmpty() && !isMixedMode && !isHistoryMode
    var showMenu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = stringResource(R.string.rss_tab_filter_cd),
                tint = if (isDefaultMode) Color.White.copy(alpha = 0.7f) else ThemePrimaryColor,
                modifier = Modifier.size(22.dp)
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
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
                        color = if (isDefaultMode) ThemePrimaryColor else Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (isDefaultMode) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = if (isDefaultMode) {
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
                    onMixedModeChange(false)
                    onHistoryModeChange(false)
                    onFeedFilterChange(emptySet())
                    showMenu = false
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
                    onMixedModeChange(true)
                    onHistoryModeChange(false)
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.rss_tab_history),
                        color = if (isHistoryMode) ThemePrimaryColor else Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (isHistoryMode) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = if (isHistoryMode) {
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
                    onHistoryModeChange(true)
                    onMixedModeChange(false)
                    showMenu = false
                }
            )
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f),
                thickness = 1.dp
            )
            feeds.forEach { feed ->
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
                        onMixedModeChange(false)
                        onHistoryModeChange(false)
                        val next = if (isSelected) selectedFeedUrls - feed.url
                        else selectedFeedUrls + feed.url
                        onFeedFilterChange(next)
                    }
                )
            }
        }
    }
}

@Composable
private fun RssOptionsMenu(
    isKeyboardVisible: Boolean,
    searchQuery: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onKeyboardToggle: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.rss_tab_options_cd),
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = DpOffset(x = 0.dp, y = (-4).dp),
            modifier = Modifier.background(Color(0xFF1A1A1A))
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.rss_tab_refresh_cd),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                },
                enabled = !isRefreshing,
                onClick = {
                    showMenu = false
                    onRefresh()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(
                            if (isKeyboardVisible) R.string.rss_tab_hide_search_cd
                            else R.string.rss_tab_search_feeds_cd
                        ),
                        color = if (isKeyboardVisible || searchQuery.isNotEmpty()) ThemePrimaryColor
                        else Color.White,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = if (isKeyboardVisible || searchQuery.isNotEmpty()) ThemePrimaryColor
                        else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    showMenu = false
                    onKeyboardToggle()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.rss_tab_settings_cd),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    showMenu = false
                    onSettingsClick()
                }
            )
        }
    }
}
