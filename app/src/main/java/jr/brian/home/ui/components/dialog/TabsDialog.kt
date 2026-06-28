package jr.brian.home.ui.components.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import jr.brian.home.R
import jr.brian.home.data.PageCountManager
import jr.brian.home.model.PageType
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeAccentColor

@Composable
fun TabsDialog(
    currentTabIndex: Int,
    totalPages: Int,
    modifier: Modifier = Modifier,
    onTabSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    onDeletePage: (Int) -> Unit,
    onAddPage: (PageType) -> Unit,
    pageTypes: List<PageType> = emptyList(),
    onNavigateToSearch: () -> Unit = {},
    onReorderPages: (newOrder: List<PageType>, oldIndicesInNewOrder: List<Int>, newCurrentTabIndex: Int) -> Unit = { _, _, _ -> }
) {
    var showDeleteConfirmation by remember { mutableStateOf<Int?>(null) }
    var showPageTypeSelection by remember { mutableStateOf(false) }

    // Local reorderable list — tracks (originalIndex, PageType) so we can compute the
    // new home-tab position after reordering even when multiple tabs share a type.
    // Recreated whenever pageTypes changes (i.e. after add/delete/reorder commits).
    // Tracks (originalIndex, PageType) so we can find where the home tab moved after reorder.
    val localIndexed = remember(pageTypes) {
        mutableStateListOf(*pageTypes.mapIndexed { i, t -> i to t }.toTypedArray())
    }

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val itemHeightsPx = remember { mutableMapOf<Int, Float>() }

    if (showPageTypeSelection) {
        PageTypeSelectionDialog(
            onTypeSelected = { pageType ->
                onAddPage(pageType)
                showPageTypeSelection = false
            },
            onDismiss = { showPageTypeSelection = false }
        )
    }

    showDeleteConfirmation?.let { pageIndex ->
        ConfirmationDialog(
            title = stringResource(R.string.home_tab_delete_page_title),
            message = stringResource(R.string.home_tab_delete_page_message, pageIndex + 1),
            confirmText = stringResource(R.string.home_tab_delete_confirm),
            cancelText = stringResource(R.string.home_tab_delete_cancel),
            onConfirm = {
                onDeletePage(pageIndex)
                showDeleteConfirmation = null
            },
            onDismiss = { showDeleteConfirmation = null }
        )
    }

    DimmedDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            color = OledCardColor,
            shape = RoundedCornerShape(24.dp),
            modifier = modifier
                .fillMaxWidth(0.9f)
                .border(
                    width = 1.dp,
                    brush = borderBrush(isFocused = true),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Header ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.home_tab_dialog_title),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                onNavigateToSearch()
                                onDismiss()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.home_tab_search_apps),
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.dialog_cancel),
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // ── Tab list with drag-to-reorder ────────────────────────
                localIndexed.forEachIndexed { listPos, (originalIndex, pageType) ->
                    val pageLabel = when (pageType) {
                        PageType.APPS_TAB -> stringResource(R.string.home_tab_page_type_apps_tab)
                        PageType.APPS_AND_WIDGETS_TAB -> stringResource(R.string.home_tab_page_type_apps_and_widgets_tab)
                        PageType.APP_DRAWER_TAB -> stringResource(R.string.home_tab_page_type_app_drawer_tab)
                        PageType.RSS_TAB -> stringResource(R.string.home_tab_page_type_rss_tab)
                        PageType.UNIFIED_CANVAS -> stringResource(R.string.home_tab_page_type_unified_canvas)
                    }

                    val isHomeTab = if (totalPages == 1) {
                        listPos == 0
                    } else {
                        currentTabIndex == originalIndex
                    }

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
                        TabOption(
                            text = stringResource(
                                R.string.home_tab_page_type,
                                listPos + 1,
                                pageLabel
                            ),
                            isSelected = isHomeTab,
                            showDelete = totalPages > 1 && !isDragging,
                            showDragHandle = totalPages > 1,
                            isDragging = isDragging,
                            onClick = {
                                onTabSelected(originalIndex)
                                onDismiss()
                            },
                            onDelete = { showDeleteConfirmation = originalIndex },
                            dragHandleModifier = Modifier.pointerInput(listPos) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingIndex = listPos
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { _, dragAmount ->
                                        dragOffsetY += dragAmount.y
                                        val cur = draggingIndex ?: return@detectDragGesturesAfterLongPress

                                        // Swap down
                                        if (cur < localIndexed.size - 1) {
                                            val nextH = itemHeightsPx[cur + 1] ?: 80f
                                            if (dragOffsetY > nextH * 0.5f) {
                                                localIndexed.add(cur + 1, localIndexed.removeAt(cur))
                                                draggingIndex = cur + 1
                                                dragOffsetY -= nextH
                                            }
                                        }
                                        // Swap up
                                        if (cur > 0) {
                                            val prevH = itemHeightsPx[cur - 1] ?: 80f
                                            if (dragOffsetY < -prevH * 0.5f) {
                                                localIndexed.add(cur - 1, localIndexed.removeAt(cur))
                                                draggingIndex = cur - 1
                                                dragOffsetY += prevH
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        val newOrder = localIndexed.map { it.second }
                                        val oldIndicesInNewOrder = localIndexed.map { it.first }
                                        val newCurrentIdx = localIndexed.indexOfFirst {
                                            it.first == currentTabIndex
                                        }.coerceAtLeast(0)
                                        onReorderPages(newOrder, oldIndicesInNewOrder, newCurrentIdx)
                                        draggingIndex = null
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        // Restore original order
                                        localIndexed.clear()
                                        localIndexed.addAll(pageTypes.mapIndexed { i, t -> i to t })
                                        draggingIndex = null
                                        dragOffsetY = 0f
                                    }
                                )
                            }
                        )
                    }
                }

                // ── Add page button ──────────────────────────────────────
                AnimatedVisibility(
                    visible = totalPages < PageCountManager.MAX_PAGE_COUNT + 1,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    AddPageButton(onClick = { showPageTypeSelection = true })
                }
            }
        }
    }
}

@Composable
private fun TabOption(
    text: String,
    isSelected: Boolean,
    showDelete: Boolean,
    showDragHandle: Boolean,
    isDragging: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    dragHandleModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.TopStart,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .scale(animatedFocusedScale(isFocused))
                .onFocusChanged { isFocused = it.isFocused }
                .background(
                    brush = cardGradient(isFocused = isFocused || isDragging),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = if (isFocused || isDragging) 3.dp else 2.dp,
                    brush = borderBrush(isFocused = isFocused || isDragging),
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .focusable(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tap area → selects the tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() }
                    .padding(20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Drag handle
            if (showDragHandle) {
                Box(
                    modifier = Modifier
                        .padding(end = if (showDelete) 0.dp else 4.dp)
                        .size(48.dp)
                        .then(dragHandleModifier),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag to reorder",
                        tint = Color.White.copy(alpha = if (isDragging) 1f else 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Delete button
            if (showDelete) {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.home_tab_delete_page_title),
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Home indicator badge
        if (isSelected) {
            val offset = Pair(
                first = if (isFocused) (-18).dp else (-10).dp,
                second = if (isFocused) (-12).dp else (-8).dp
            )
            Box(
                modifier = Modifier
                    .offset(x = offset.first, y = offset.second)
                    .scale(animatedFocusedScale(isFocused))
                    .size(28.dp)
                    .background(color = ThemeAccentColor, shape = CircleShape)
                    .border(width = 2.dp, color = Color.White, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home Tab",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AddPageButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient(isFocused = isFocused),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isFocused) 3.dp else 2.dp,
                brush = borderBrush(isFocused = isFocused),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .focusable()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = stringResource(R.string.home_tab_add_page),
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Backward compatibility wrapper for HomeTabSelectionDialog.
 * Forwards all calls to TabsDialog with the same behavior.
 */
@Composable
fun HomeTabSelectionDialog(
    currentTabIndex: Int,
    totalPages: Int,
    modifier: Modifier = Modifier,
    onTabSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    onDeletePage: (Int) -> Unit,
    onAddPage: (PageType) -> Unit,
    pageTypes: List<PageType> = emptyList(),
    onNavigateToSearch: () -> Unit = {},
    onReorderPages: (newOrder: List<PageType>, oldIndicesInNewOrder: List<Int>, newCurrentTabIndex: Int) -> Unit = { _, _, _ -> }
) {
    TabsDialog(
        currentTabIndex = currentTabIndex,
        totalPages = totalPages,
        modifier = modifier,
        onTabSelected = onTabSelected,
        onDismiss = onDismiss,
        onDeletePage = onDeletePage,
        onAddPage = onAddPage,
        pageTypes = pageTypes,
        onNavigateToSearch = onNavigateToSearch,
        onReorderPages = onReorderPages
    )
}
