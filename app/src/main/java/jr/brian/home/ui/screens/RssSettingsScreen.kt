package jr.brian.home.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.model.rss.RssFeed
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.ui.components.settings.CollapsibleSettingsSection
import jr.brian.home.ui.components.settings.ScreenHeader
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.viewmodels.RssViewModel

private val REFRESH_INTERVAL_MINUTES = listOf(0, 15, 30, 60, 120, 360, 720)

@Composable
fun RssSettingsScreen(
    onDismiss: () -> Unit,
    viewModel: RssViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddFeedDialog by remember { mutableStateOf(false) }
    var displayExpanded by remember { mutableStateOf(false) }

    val localFeeds = remember(uiState.feeds) {
        mutableStateListOf(*uiState.feeds.toTypedArray())
    }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val itemHeightsPx = remember { mutableMapOf<Int, Float>() }

    if (showAddFeedDialog) {
        AddFeedDialog(
            isLoading = uiState.isLoading,
            onAdd = { url ->
                viewModel.addFeed(url)
                showAddFeedDialog = false
            },
            onDismiss = { showAddFeedDialog = false }
        )
    }

    Scaffold(containerColor = OledBackgroundColor) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
        ) {
            ScreenHeader(onBackClick = onDismiss)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RssFeed,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = stringResource(R.string.rss_settings_title),
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = ThemePrimaryColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.refreshAllFeeds() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.rss_settings_refresh_all_cd),
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    IconButton(onClick = { showAddFeedDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.rss_settings_add_feed_cd),
                            tint = ThemePrimaryColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.rss_settings_save_cd),
                            tint = ThemePrimaryColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.error != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                uiState.error?.let { error ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp)
                            .background(
                                color = Color(0xFFB00020).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFFB00020).copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                color = Color(0xFFFF6B6B),
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text(stringResource(R.string.rss_settings_error_dismiss), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            if (uiState.feeds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RssFeed,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = stringResource(R.string.rss_settings_empty_title),
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.rss_settings_empty_hint),
                            color = Color.White.copy(alpha = 0.25f),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        CollapsibleSettingsSection(
                            title = stringResource(R.string.rss_settings_display_section),
                            icon = Icons.Default.Tune,
                            isExpanded = displayExpanded,
                            onToggle = { displayExpanded = !displayExpanded }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.rss_settings_date_format),
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(true to stringResource(R.string.rss_settings_date_dmy), false to stringResource(R.string.rss_settings_date_mdy)).forEach { (isDMY, label) ->
                                        val selected = uiState.useDMYDateFormat == isDMY
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (selected) ThemePrimaryColor.copy(alpha = 0.2f)
                                                    else Color.White.copy(alpha = 0.07f)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (selected) ThemePrimaryColor.copy(alpha = 0.5f)
                                                            else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { if (!selected) viewModel.setUseDMYDateFormat(isDMY) }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (selected) ThemePrimaryColor else Color.White.copy(alpha = 0.5f),
                                                fontSize = 12.sp,
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.rss_settings_time_format),
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(true to stringResource(R.string.rss_settings_time_24h), false to stringResource(R.string.rss_settings_time_12h)).forEach { (is24, label) ->
                                        val selected = uiState.use24HourClock == is24
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (selected) ThemePrimaryColor.copy(alpha = 0.2f)
                                                    else Color.White.copy(alpha = 0.07f)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (selected) ThemePrimaryColor.copy(alpha = 0.5f)
                                                            else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { if (!selected) viewModel.setUse24HourClock(is24) }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (selected) ThemePrimaryColor else Color.White.copy(alpha = 0.5f),
                                                fontSize = 12.sp,
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item(key = "feed_list") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            localFeeds.forEachIndexed { listPos, feed ->
                                val isDragging = draggingIndex == listPos
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .zIndex(if (isDragging) 1f else 0f)
                                        .graphicsLayer {
                                            translationY = if (isDragging) dragOffsetY else 0f
                                            shadowElevation = if (isDragging) 24f else 0f
                                            scaleX = if (isDragging) 1.03f else 1f
                                            scaleY = if (isDragging) 1.03f else 1f
                                            alpha = if (isDragging) 0.92f else 1f
                                        }
                                        .onGloballyPositioned { coords ->
                                            itemHeightsPx[listPos] = coords.size.height.toFloat()
                                        }
                                ) {
                                    FeedCard(
                                        feed = feed,
                                        isRefreshing = uiState.isRefreshing,
                                        isDragging = isDragging,
                                        onRefresh = { viewModel.refreshFeed(feed.url) },
                                        onDelete = { viewModel.removeFeed(feed.url) },
                                        onIntervalSelected = { minutes ->
                                            viewModel.setRefreshInterval(feed.url, minutes)
                                        },
                                        dragHandleModifier = Modifier.pointerInput(listPos) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggingIndex = listPos
                                                    dragOffsetY = 0f
                                                },
                                                onDrag = { _, dragAmount ->
                                                    dragOffsetY += dragAmount.y
                                                    val cur = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                                    if (cur < localFeeds.size - 1) {
                                                        val nextH = itemHeightsPx[cur + 1] ?: 80f
                                                        if (dragOffsetY > nextH * 0.5f) {
                                                            localFeeds.add(cur + 1, localFeeds.removeAt(cur))
                                                            draggingIndex = cur + 1
                                                            dragOffsetY -= nextH
                                                        }
                                                    }
                                                    if (cur > 0) {
                                                        val prevH = itemHeightsPx[cur - 1] ?: 80f
                                                        if (dragOffsetY < -prevH * 0.5f) {
                                                            localFeeds.add(cur - 1, localFeeds.removeAt(cur))
                                                            draggingIndex = cur - 1
                                                            dragOffsetY += prevH
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    viewModel.reorderFeeds(localFeeds.map { it.url })
                                                    draggingIndex = null
                                                    dragOffsetY = 0f
                                                },
                                                onDragCancel = {
                                                    localFeeds.clear()
                                                    localFeeds.addAll(uiState.feeds)
                                                    draggingIndex = null
                                                    dragOffsetY = 0f
                                                }
                                            )
                                        }
                                    )
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
private fun FeedCard(
    feed: RssFeed,
    isRefreshing: Boolean,
    isDragging: Boolean = false,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
    onIntervalSelected: (Int) -> Unit,
    dragHandleModifier: Modifier = Modifier
) {
    var showIntervalMenu by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val intervalLabels = mapOf(
        0 to stringResource(R.string.rss_settings_interval_manual),
        15 to stringResource(R.string.rss_settings_interval_15m),
        30 to stringResource(R.string.rss_settings_interval_30m),
        60 to stringResource(R.string.rss_settings_interval_1h),
        120 to stringResource(R.string.rss_settings_interval_2h),
        360 to stringResource(R.string.rss_settings_interval_6h),
        720 to stringResource(R.string.rss_settings_interval_12h)
    )
    val intervalLabel = intervalLabels[feed.refreshIntervalMinutes]
        ?: "${feed.refreshIntervalMinutes} min"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = subtleCardGradient(false), shape = RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                brush = borderBrush(isFocused = false),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = feed.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (feed.title != feed.url) {
                        Text(
                            text = feed.url,
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.rss_settings_feed_refresh_cd),
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.rss_settings_feed_delete_cd),
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .then(dragHandleModifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = if (isDragging) 1f else 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .clickable { showIntervalMenu = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = ThemeAccentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = intervalLabel,
                            color = ThemeAccentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    DropdownMenu(
                        expanded = showIntervalMenu,
                        onDismissRequest = { showIntervalMenu = false },
                        containerColor = OledCardColor
                    ) {
                        REFRESH_INTERVAL_MINUTES.forEach { minutes ->
                            val label = intervalLabels[minutes] ?: "$minutes min"
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        color = if (minutes == feed.refreshIntervalMinutes) {
                                            ThemeAccentColor
                                        } else {
                                            Color.White
                                        },
                                        fontWeight = if (minutes == feed.refreshIntervalMinutes) {
                                            FontWeight.SemiBold
                                        } else {
                                            FontWeight.Normal
                                        }
                                    )
                                },
                                onClick = {
                                    onIntervalSelected(minutes)
                                    showIntervalMenu = false
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = Color.White
                                )
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .clickable { clipboard.setText(AnnotatedString(feed.url)) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.rss_settings_copy_url_cd),
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddFeedDialog(
    isLoading: Boolean,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(color = OledCardColor, shape = RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    brush = borderBrush(isFocused = true),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RssFeed,
                        contentDescription = null,
                        tint = ThemePrimaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.rss_settings_dialog_title),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.rss_settings_dialog_url_placeholder),
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    },
                    label = { Text(stringResource(R.string.rss_settings_dialog_url_label), color = Color.White.copy(alpha = 0.6f)) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            clipboard.getText()?.text?.let { url = it }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = stringResource(R.string.rss_settings_dialog_paste_cd),
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (url.isNotBlank()) onAdd(url.trim()) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ThemePrimaryColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = ThemePrimaryColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.rss_settings_dialog_hint),
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.rss_settings_dialog_cancel), color = Color.White.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                color = if (url.isNotBlank() && !isLoading) {
                                    ThemePrimaryColor
                                } else {
                                    ThemePrimaryColor.copy(alpha = 0.4f)
                                }
                            )
                            .clickable(enabled = url.isNotBlank() && !isLoading) {
                                onAdd(url.trim())
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.rss_settings_dialog_add),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
