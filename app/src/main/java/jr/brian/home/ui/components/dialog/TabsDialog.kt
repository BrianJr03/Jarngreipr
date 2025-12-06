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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import jr.brian.home.R
import jr.brian.home.data.PageCountManager
import jr.brian.home.data.PageType
import jr.brian.home.model.AppInfo
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

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
    onNavigateToSearch: () -> Unit = {}
) {
    var showDeleteConfirmation by remember { mutableStateOf<Int?>(null) }
    var showPageTypeSelection by remember { mutableStateOf(false) }

    if (showPageTypeSelection) {
        PageTypeSelectionDialog(
            onTypeSelected = { pageType ->
                onAddPage(pageType)
                showPageTypeSelection = false
            },
            onDismiss = {
                showPageTypeSelection = false
            }
        )
    }

    if (showDeleteConfirmation != null) {
        ConfirmationDialog(
            title = stringResource(R.string.home_tab_delete_page_title),
            message = stringResource(
                R.string.home_tab_delete_page_message,
                showDeleteConfirmation!! + 1
            ),
            confirmText = stringResource(R.string.home_tab_delete_confirm),
            cancelText = stringResource(R.string.home_tab_delete_cancel),
            onConfirm = {
                onDeletePage(showDeleteConfirmation!!)
                showDeleteConfirmation = null
            },
            onDismiss = {
                showDeleteConfirmation = null
            }
        )
    }

    Dialog(
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
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.5f),
                            ThemeSecondaryColor.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

                repeat(totalPages) { index ->
                    val pageType =
                        if (index < pageTypes.size) pageTypes[index] else PageType.APPS_TAB
                    val pageLabel = when (pageType) {
                        PageType.APPS_TAB -> stringResource(R.string.home_tab_page_type_apps_tab)
                        PageType.APPS_AND_WIDGETS_TAB -> stringResource(R.string.home_tab_page_type_apps_and_widgets_tab)
                    }

                    // When there's only one page, it's always the home tab (index 0)
                    // Otherwise, show home badge for the current tab
                    val isHomeTab = if (totalPages == 1) {
                        index == 0
                    } else {
                        currentTabIndex == index
                    }

                    TabOption(
                        text = stringResource(R.string.home_tab_page_type, index + 1, pageLabel),
                        isSelected = isHomeTab,
                        showDelete = totalPages > 1,
                        onClick = {
                            // Only set home tab if user explicitly clicks and it's different from current
                            if (index != currentTabIndex) {
                                onTabSelected(index)
                            }
                            onDismiss()
                        },
                        onDelete = {
                            showDeleteConfirmation = index
                        }
                    )
                }

                AnimatedVisibility(
                    visible = totalPages < PageCountManager.MAX_PAGE_COUNT + 1,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    AddPageButton(
                        onClick = {
                            showPageTypeSelection = true
                        }
                    )
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
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val cardGradient = Brush.linearGradient(
        colors = if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.9f),
                ThemeSecondaryColor.copy(alpha = 0.9f)
            )
        } else {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.4f),
                ThemeSecondaryColor.copy(alpha = 0.3f)
            )
        }
    )

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
                    brush = cardGradient,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = if (isFocused) 3.dp else 2.dp,
                    brush = if (isFocused) {
                        borderBrush(
                            isFocused = true,
                            colors = listOf(
                                ThemePrimaryColor.copy(alpha = 0.8f),
                                ThemeSecondaryColor.copy(alpha = 0.6f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                ThemePrimaryColor.copy(alpha = 0.6f),
                                ThemeSecondaryColor.copy(alpha = 0.4f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .focusable(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

        // Home badge overlapping the top-left corner
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
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                ThemeAccentColor,
                                ThemeAccentColor.copy(alpha = 0.8f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = Color.White,
                        shape = CircleShape
                    ),
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

    val cardGradient = Brush.linearGradient(
        colors = if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.9f),
                ThemeSecondaryColor.copy(alpha = 0.9f)
            )
        } else {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.4f),
                ThemeSecondaryColor.copy(alpha = 0.3f)
            )
        }
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isFocused) 3.dp else 2.dp,
                brush = if (isFocused) {
                    borderBrush(
                        isFocused = true,
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.8f),
                            ThemeSecondaryColor.copy(alpha = 0.6f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.6f),
                            ThemeSecondaryColor.copy(alpha = 0.4f)
                        )
                    )
                },
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
    allApps: List<AppInfo> = emptyList(),
    modifier: Modifier = Modifier,
    onTabSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    onDeletePage: (Int) -> Unit,
    onAddPage: (PageType) -> Unit,
    pageTypes: List<PageType> = emptyList(),
    onNavigateToSearch: () -> Unit = {}
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
        onNavigateToSearch = onNavigateToSearch
    )
}
