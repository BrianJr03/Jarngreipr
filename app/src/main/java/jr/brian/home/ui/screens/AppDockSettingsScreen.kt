package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.data.DockSize
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.components.settings.ScreenHeader
import jr.brian.home.ui.components.settings.SettingCard
import jr.brian.home.ui.components.dock.ColorOption
import jr.brian.home.ui.components.dock.DockSizeOption
import jr.brian.home.ui.components.dock.MaxDockAppsOption
import jr.brian.home.ui.components.dock.SwappableDockPreview
import jr.brian.home.ui.components.dock.PageVisibilityOption
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalDockManager
import jr.brian.home.ui.util.ColorOptions
import jr.brian.home.ui.theme.managers.LocalPageTypeManager

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppDockSettingsScreen(
    allApps: List<AppInfo>,
    onDismiss: () -> Unit
) {
    val dockManager = LocalDockManager.current
    val pageTypeManager = LocalPageTypeManager.current

    val dockApps by dockManager.dockApps.collectAsStateWithLifecycle()
    val dockColor by dockManager.dockColor.collectAsStateWithLifecycle()
    val dockSize by dockManager.dockSize.collectAsStateWithLifecycle()
    val isDockVisible by dockManager.isDockVisible.collectAsStateWithLifecycle()
    val dockVisiblePages by dockManager.dockVisiblePages.collectAsStateWithLifecycle()
    val maxDockApps by dockManager.maxDockApps.collectAsStateWithLifecycle()
    val pageTypes by pageTypeManager.pageTypes.collectAsStateWithLifecycle()
    val pageCount = pageTypes.size
    
    val currentSlotCount = dockApps.size
    val canAddMoreSlots = currentSlotCount < maxDockApps

    BackHandler {
        onDismiss()
    }

    Scaffold(
        containerColor = OledBackgroundColor,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
                ScreenHeader(onBackClick = onDismiss)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    SettingCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.dock_visibility_title),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.dock_visibility_description),
                                    color = Color.Gray.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = isDockVisible,
                                onCheckedChange = { dockManager.setDockVisibility(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ThemePrimaryColor,
                                    checkedTrackColor = ThemePrimaryColor.copy(alpha = 0.5f),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color.DarkGray.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    if (isDockVisible) {
                        SettingCard {
                            Column {
                                Text(
                                    text = stringResource(R.string.dock_preview_title),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = stringResource(R.string.dock_preview_description),
                                    color = Color.Gray.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = OledBackgroundColor,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(vertical = 16.dp)
                                ) {
                                    SwappableDockPreview(
                                        apps = allApps,
                                        dockColor = dockColor,
                                        dockSize = dockSize
                                    )
                                }
                            }
                        }


                        SettingCard {
                            Column {
                                Text(
                                    text = stringResource(R.string.dock_color_title),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ColorOptions.standardColors().forEach { color ->
                                        ColorOption(
                                            color = color,
                                            isSelected = dockColor == color,
                                            onSelect = { dockManager.setDockColor(color) }
                                        )
                                    }
                                }
                            }
                        }

                        if (canAddMoreSlots) {
                            SettingCard {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            dockManager.addEmptySlot(currentSlotCount)
                                        },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.dock_add_slot_title),
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stringResource(R.string.dock_add_slot_description, currentSlotCount, maxDockApps),
                                            color = Color.Gray.copy(alpha = 0.7f),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        SettingCard {
                            Column {
                                Text(
                                    text = stringResource(R.string.dock_max_apps_title),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = stringResource(R.string.dock_max_apps_description),
                                    color = Color.Gray.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    listOf(2, 3, 4, 5).forEach { count ->
                                        MaxDockAppsOption(
                                            count = count,
                                            isSelected = maxDockApps == count,
                                            onSelect = { dockManager.setMaxDockApps(count) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        SettingCard {
                            Column {
                                Text(
                                    text = stringResource(R.string.dock_size_title),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    DockSize.entries.forEach { size ->
                                        DockSizeOption(
                                            size = size,
                                            isSelected = dockSize == size,
                                            onSelect = { dockManager.setDockSize(size) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        if (pageCount > 1) {
                            SettingCard {
                                Column {
                                    Text(
                                        text = stringResource(R.string.dock_page_visibility_title),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = stringResource(R.string.dock_page_visibility_description),
                                        color = Color.Gray.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        for (pageIndex in 0 until pageCount) {
                                            val isPageVisible = dockVisiblePages.isEmpty() || dockVisiblePages.contains(pageIndex)
                                            PageVisibilityOption(
                                                pageIndex = pageIndex,
                                                isVisible = isPageVisible,
                                                onToggle = { dockManager.togglePageVisibility(pageIndex, pageCount) }
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
    }
}
