package jr.brian.home.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.data.AppLayoutManager
import jr.brian.home.data.PageType
import jr.brian.home.model.AppLayout
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalAppLayoutManager
import jr.brian.home.ui.theme.managers.LocalAppPositionManager
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalPageTypeManager

@Composable
fun AppLayoutScreen() {
    val context = LocalContext.current
    val appLayoutManager = LocalAppLayoutManager.current
    val appPositionManager = LocalAppPositionManager.current
    val pageTypeManager = LocalPageTypeManager.current
    val homeTabManager = LocalHomeTabManager.current

    val layoutsByPage by appLayoutManager.layoutsByPage.collectAsStateWithLifecycle()
    val activeLayoutIdByPage by appLayoutManager.activeLayoutIdByPage.collectAsStateWithLifecycle()
    val freeModeByPage by appPositionManager.isFreeModeByPage.collectAsStateWithLifecycle()
    val pageTypes by pageTypeManager.pageTypes.collectAsStateWithLifecycle()
    val currentHomeTabIndex by homeTabManager.homeTabIndex.collectAsStateWithLifecycle()

    var showSaveDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedPageIndex by remember { mutableIntStateOf(0) }
    var selectedLayout by remember { mutableStateOf<AppLayout?>(null) }

    val pagesWithFreeMode = freeModeByPage.filter { it.value }.keys.sorted()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(color = OledBackgroundColor)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.app_layout_screen_title),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (pagesWithFreeMode.isEmpty()) {
                item {
                    EmptyLayoutsState(
                        message = stringResource(R.string.app_layout_not_available)
                    )
                }
            } else {
                pagesWithFreeMode.forEach { pageIndex ->
                    item {
                        val pageType = pageTypes.getOrNull(pageIndex)
                        val pageTypeLabel = when (pageType) {
                            PageType.APPS_TAB ->
                                stringResource(R.string.home_tab_page_type_apps_tab)

                            PageType.APPS_AND_WIDGETS_TAB ->
                                stringResource(R.string.home_tab_page_type_apps_and_widgets_tab)

                            null ->
                                stringResource(R.string.home_tab_page_type_apps_tab)
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.app_layout_tab_section_title,
                                                pageIndex + 1
                                            ),
                                            color = ThemePrimaryColor,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (pageIndex == currentHomeTabIndex) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = ThemePrimaryColor.copy(alpha = 0.3f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "Current",
                                                    color = ThemePrimaryColor,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = pageTypeLabel,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 14.sp
                                    )
                                }

                                SaveLayoutButton(
                                    onClick = {
                                        selectedPageIndex = pageIndex
                                        showSaveDialog = true
                                    },
                                    enabled = (layoutsByPage[pageIndex]?.size
                                        ?: 0) < AppLayoutManager.MAX_LAYOUTS_PER_PAGE
                                )
                            }

                            val layouts = layoutsByPage[pageIndex] ?: emptyList()
                            val activeLayoutId = activeLayoutIdByPage[pageIndex]

                            if (layouts.isEmpty()) {
                                EmptyLayoutsState(
                                    message = stringResource(R.string.app_layout_no_layouts_description)
                                )
                            } else {
                                layouts.forEach { layout ->
                                    LayoutItem(
                                        layout = layout,
                                        isActive = layout.id == activeLayoutId,
                                        onSelect = {
                                            if (layout.id == activeLayoutId) {
                                                appLayoutManager.setActiveLayout(pageIndex, null)
                                                appPositionManager.clearAllPositions(pageIndex)
                                            } else {
                                                appPositionManager.clearAllPositions(pageIndex)
                                                layout.positions.forEach { (_, position) ->
                                                    appPositionManager.savePosition(
                                                        pageIndex,
                                                        position
                                                    )
                                                }
                                                appLayoutManager.setActiveLayout(
                                                    pageIndex,
                                                    layout.id
                                                )
                                            }
                                        },
                                        onRename = {
                                            selectedPageIndex = pageIndex
                                            selectedLayout = layout
                                            showRenameDialog = true
                                        },
                                        onDelete = {
                                            selectedPageIndex = pageIndex
                                            selectedLayout = layout
                                            showDeleteDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showSaveDialog) {
        SaveLayoutDialog(
            pageIndex = selectedPageIndex,
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                val positions = appPositionManager.getPositions(selectedPageIndex)
                val success = appLayoutManager.saveLayout(selectedPageIndex, name, positions)
                if (success) {
                    showSaveDialog = false
                }
            },
            maxLayoutsReached = (layoutsByPage[selectedPageIndex]?.size
                ?: 0) >= AppLayoutManager.MAX_LAYOUTS_PER_PAGE
        )
    }

    if (showRenameDialog && selectedLayout != null) {
        RenameLayoutDialog(
            currentName = selectedLayout!!.name,
            onDismiss = {
                showRenameDialog = false
                selectedLayout = null
            },
            onRename = { newName ->
                appLayoutManager.updateLayoutName(
                    selectedPageIndex,
                    selectedLayout!!.id,
                    newName
                )
                showRenameDialog = false
                selectedLayout = null
            }
        )
    }

    if (showDeleteDialog && selectedLayout != null) {
        DeleteLayoutDialog(
            layoutName = selectedLayout!!.name,
            onDismiss = {
                showDeleteDialog = false
                selectedLayout = null
            },
            onConfirm = {
                appLayoutManager.deleteLayout(selectedPageIndex, selectedLayout!!.id)
                showDeleteDialog = false
                selectedLayout = null
            }
        )
    }
}

@Composable
private fun SaveLayoutButton(
    onClick: () -> Unit,
    enabled: Boolean
) {
    var isFocused by remember { mutableStateOf(false) }

    val cardGradient = Brush.linearGradient(
        colors = if (enabled) {
            if (isFocused) {
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
        } else {
            listOf(
                Color.Gray.copy(alpha = 0.3f),
                Color.Gray.copy(alpha = 0.2f)
            )
        }
    )

    Box(
        modifier = Modifier
            .scale(animatedFocusedScale(isFocused && enabled))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused && enabled) 3.dp else 2.dp,
                brush = if (isFocused && enabled) {
                    borderBrush(
                        isFocused = true,
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.8f),
                            ThemeSecondaryColor.copy(alpha = 0.6f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = if (enabled) {
                            listOf(
                                ThemePrimaryColor.copy(alpha = 0.6f),
                                ThemeSecondaryColor.copy(alpha = 0.4f)
                            )
                        } else {
                            listOf(
                                Color.Gray.copy(alpha = 0.4f),
                                Color.Gray.copy(alpha = 0.3f)
                            )
                        }
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }
            .focusable(enabled = enabled)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.app_layout_save_current),
            color = if (enabled) Color.White else Color.Gray,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LayoutItem(
    layout: AppLayout,
    isActive: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val cardGradient = Brush.linearGradient(
        colors = if (isActive) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.6f),
                ThemeSecondaryColor.copy(alpha = 0.5f)
            )
        } else if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.3f),
                ThemeSecondaryColor.copy(alpha = 0.2f)
            )
        } else {
            listOf(
                Color(0xFF2a2a3e).copy(alpha = 0.6f),
                Color(0xFF1f2937).copy(alpha = 0.5f)
            )
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 3.dp else if (isActive) 2.dp else 1.dp,
                brush = if (isFocused) {
                    borderBrush(
                        isFocused = true,
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.8f),
                            ThemeSecondaryColor.copy(alpha = 0.6f)
                        )
                    )
                } else if (isActive) {
                    Brush.linearGradient(
                        colors = listOf(
                            ThemePrimaryColor,
                            ThemeSecondaryColor
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Gray.copy(alpha = 0.3f),
                            Color.Gray.copy(alpha = 0.2f)
                        )
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .focusable()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (isActive) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.app_layout_active),
                        tint = ThemePrimaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = layout.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${layout.positions.size} apps",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    icon = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.app_layout_rename),
                    onClick = onRename
                )

                IconButton(
                    icon = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.app_layout_delete),
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun IconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                color = if (isFocused) {
                    ThemePrimaryColor.copy(alpha = 0.3f)
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) {
                    ThemePrimaryColor
                } else {
                    Color.White.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EmptyLayoutsState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SaveLayoutDialog(
    pageIndex: Int,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    maxLayoutsReached: Boolean
) {
    val defaultName = stringResource(
        R.string.app_layout_default_name,
        System.currentTimeMillis() % 1000
    )
    var layoutName by remember { mutableStateOf(defaultName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.app_layout_save_dialog_title),
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.app_layout_save_dialog_message),
                    color = Color.White.copy(alpha = 0.8f)
                )

                TextField(
                    value = layoutName,
                    onValueChange = { layoutName = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.app_layout_save_dialog_hint),
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF2a2a3e),
                        unfocusedContainerColor = Color(0xFF2a2a3e),
                        cursorColor = ThemePrimaryColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (maxLayoutsReached) {
                    Text(
                        text = stringResource(
                            R.string.app_layout_max_reached,
                            AppLayoutManager.MAX_LAYOUTS_PER_PAGE
                        ),
                        color = Color.Red.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            var isFocused by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .scale(animatedFocusedScale(isFocused))
                    .onFocusChanged { isFocused = it.isFocused }
                    .background(
                        color = if (isFocused) {
                            ThemePrimaryColor.copy(alpha = 0.8f)
                        } else {
                            ThemePrimaryColor.copy(alpha = 0.6f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !maxLayoutsReached && layoutName.isNotBlank()) {
                        if (layoutName.isNotBlank()) {
                            onSave(layoutName)
                        }
                    }
                    .focusable(enabled = !maxLayoutsReached && layoutName.isNotBlank())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_layout_save_dialog_save),
                    color = Color.White
                )
            }
        },
        dismissButton = {
            var isFocused by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .scale(animatedFocusedScale(isFocused))
                    .onFocusChanged { isFocused = it.isFocused }
                    .background(
                        color = if (isFocused) {
                            Color.Gray.copy(alpha = 0.4f)
                        } else {
                            Color.Gray.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDismiss() }
                    .focusable()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_layout_save_dialog_cancel),
                    color = Color.White
                )
            }
        },
        containerColor = Color(0xFF1a1a2e)
    )
}

@Composable
private fun RenameLayoutDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var layoutName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.app_layout_rename_dialog_title),
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.app_layout_rename_dialog_message),
                    color = Color.White.copy(alpha = 0.8f)
                )

                TextField(
                    value = layoutName,
                    onValueChange = { layoutName = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.app_layout_save_dialog_hint),
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF2a2a3e),
                        unfocusedContainerColor = Color(0xFF2a2a3e),
                        cursorColor = ThemePrimaryColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            var isFocused by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .scale(animatedFocusedScale(isFocused))
                    .onFocusChanged { isFocused = it.isFocused }
                    .background(
                        color = if (isFocused) {
                            ThemePrimaryColor.copy(alpha = 0.8f)
                        } else {
                            ThemePrimaryColor.copy(alpha = 0.6f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = layoutName.isNotBlank()) {
                        if (layoutName.isNotBlank()) {
                            onRename(layoutName)
                        }
                    }
                    .focusable(enabled = layoutName.isNotBlank())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_layout_rename_dialog_save),
                    color = Color.White
                )
            }
        },
        dismissButton = {
            var isFocused by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .scale(animatedFocusedScale(isFocused))
                    .onFocusChanged { isFocused = it.isFocused }
                    .background(
                        color = if (isFocused) {
                            Color.Gray.copy(alpha = 0.4f)
                        } else {
                            Color.Gray.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDismiss() }
                    .focusable()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_layout_save_dialog_cancel),
                    color = Color.White
                )
            }
        },
        containerColor = Color(0xFF1a1a2e)
    )
}

@Composable
private fun DeleteLayoutDialog(
    layoutName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.app_layout_delete_dialog_title),
                color = Color.White
            )
        },
        text = {
            Text(
                text = stringResource(R.string.app_layout_delete_dialog_message, layoutName),
                color = Color.White.copy(alpha = 0.8f)
            )
        },
        confirmButton = {
            var isFocused by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .scale(animatedFocusedScale(isFocused))
                    .onFocusChanged { isFocused = it.isFocused }
                    .background(
                        color = if (isFocused) {
                            Color.Red.copy(alpha = 0.8f)
                        } else {
                            Color.Red.copy(alpha = 0.6f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onConfirm() }
                    .focusable()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_layout_delete_dialog_confirm),
                    color = Color.White
                )
            }
        },
        dismissButton = {
            var isFocused by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .scale(animatedFocusedScale(isFocused))
                    .onFocusChanged { isFocused = it.isFocused }
                    .background(
                        color = if (isFocused) {
                            Color.Gray.copy(alpha = 0.4f)
                        } else {
                            Color.Gray.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDismiss() }
                    .focusable()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_layout_delete_dialog_cancel),
                    color = Color.White
                )
            }
        },
        containerColor = Color(0xFF1a1a2e)
    )
}
