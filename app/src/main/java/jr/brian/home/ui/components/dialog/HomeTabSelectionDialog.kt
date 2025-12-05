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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun HomeTabSelectionDialog(
    currentTabIndex: Int,
    totalPages: Int,
    onTabSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    onDeletePage: (Int) -> Unit,
    onAddPage: (PageType) -> Unit,
    pageTypes: List<PageType> = emptyList(),
    modifier: Modifier = Modifier
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
                Text(
                    text = stringResource(R.string.home_tab_dialog_title),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                repeat(totalPages) { index ->
                    val pageType =
                        if (index < pageTypes.size) pageTypes[index] else PageType.APPS_TAB
                    val pageLabel = when (pageType) {
                        PageType.APPS_TAB -> stringResource(R.string.home_tab_page_type_apps_tab)
                        PageType.APPS_AND_WIDGETS_TAB -> stringResource(R.string.home_tab_page_type_apps_and_widgets_tab)
                    }

                    TabOption(
                        text = "$pageLabel ${index + 1}",
                        isSelected = currentTabIndex == index,
                        showDelete = true,
                        onClick = {
                            onTabSelected(index)
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

    val backgroundColor = when {
        isSelected -> Brush.horizontalGradient(
            colors = listOf(
                ThemePrimaryColor.copy(alpha = 0.3f),
                ThemeSecondaryColor.copy(alpha = 0.2f)
            )
        )

        isFocused -> Brush.horizontalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.1f)
            )
        )

        else -> Brush.horizontalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.05f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    }

    val borderColor = when {
        isSelected -> ThemePrimaryColor.copy(alpha = 0.6f)
        isFocused -> Color.White.copy(alpha = 0.3f)
        else -> Color.White.copy(alpha = 0.1f)
    }

    val textColor = when {
        isSelected -> ThemePrimaryColor
        else -> Color.White
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(brush = backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable { onClick() }
                .padding(vertical = 16.dp, horizontal = 20.dp)
                .focusable()
                .onFocusChanged { isFocused = it.isFocused },
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 17.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }

        if (showDelete) {
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onDelete() }
                    .background(Color.Red.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.home_tab_delete_page_title),
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(20.dp)
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

    val backgroundBrush = when {
        isFocused -> Brush.horizontalGradient(
            colors = listOf(
                ThemePrimaryColor.copy(alpha = 0.4f),
                ThemeSecondaryColor.copy(alpha = 0.3f)
            )
        )

        else -> Brush.horizontalGradient(
            colors = listOf(
                ThemePrimaryColor.copy(alpha = 0.2f),
                ThemeSecondaryColor.copy(alpha = 0.15f)
            )
        )
    }

    val borderColor = when {
        isFocused -> ThemePrimaryColor.copy(alpha = 0.8f)
        else -> ThemePrimaryColor.copy(alpha = 0.4f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(brush = backgroundBrush)
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 20.dp)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = ThemePrimaryColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = stringResource(R.string.home_tab_add_page),
            color = ThemePrimaryColor,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
