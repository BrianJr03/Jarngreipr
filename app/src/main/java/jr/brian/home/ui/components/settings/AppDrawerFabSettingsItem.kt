package jr.brian.home.ui.components.settings

import jr.brian.home.esde.data.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.esde.data.LocalESDEPreferencesManager
import jr.brian.home.ui.animations.animatedRotation
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.data.FabPosition
import jr.brian.home.ui.components.dock.ColorOption
import jr.brian.home.ui.components.dock.PageVisibilityOption
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalAppDrawerFabManager
import jr.brian.home.ui.theme.managers.LocalPageTypeManager
import jr.brian.home.ui.util.ColorOptions

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppDrawerFabSettingsItem(
    isExpanded: Boolean = false,
    onExpandChanged: (Boolean) -> Unit = {}
) {
    val fabManager = LocalAppDrawerFabManager.current
    val pageTypeManager = LocalPageTypeManager.current
    val esdePrefsManager = LocalESDEPreferencesManager.current
    val isFabEnabled by fabManager.isFabEnabled.collectAsStateWithLifecycle()
    val currentFabColor by fabManager.fabColor.collectAsStateWithLifecycle()
    val fabVisiblePages by fabManager.fabVisiblePages.collectAsStateWithLifecycle()
    val fabExplicitPages by fabManager.fabExplicitPages.collectAsStateWithLifecycle()
    val fabPosition by fabManager.fabPosition.collectAsStateWithLifecycle()
    val pageTypes by pageTypeManager.pageTypes.collectAsStateWithLifecycle()
    val esdePrefsState by esdePrefsManager.state.collectAsStateWithLifecycle()
    val pageCount = pageTypes.size
    val appDrawerOpacity = esdePrefsState.appDrawerOpacity

    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        AnimatedVisibility(
            visible = !isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
                    .background(
                        brush = subtleCardGradient(isFocused),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .border(
                        width = if (isFocused) 2.dp else 0.dp,
                        brush = borderBrush(
                            isFocused = isFocused,
                            colors = listOf(
                                ThemePrimaryColor.copy(alpha = 0.8f),
                                ThemeSecondaryColor.copy(alpha = 0.6f),
                            ),
                        ),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onExpandChanged(true) }
                    .focusable()
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .rotate(animatedRotation(isFocused)),
                        tint = Color.White,
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_app_drawer_fab_title),
                            color = Color.White,
                            fontSize = if (isFocused) 18.sp else 16.sp,
                            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.settings_app_drawer_fab_description),
                            color = if (isFocused) Color.White.copy(alpha = 0.9f) else Color.Gray,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            OledCardColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.esde_settings_app_drawer_opacity),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$appDrawerOpacity%",
                            color = ThemePrimaryColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = appDrawerOpacity.toFloat(),
                        onValueChange = { esdePrefsManager.setAppDrawerOpacity(it.toInt()) },
                        valueRange = 0f..100f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = ThemePrimaryColor,
                            activeTrackColor = ThemePrimaryColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            OledCardColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.settings_app_drawer_fab_enable),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = isFabEnabled,
                        onCheckedChange = { fabManager.setFabEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ThemePrimaryColor,
                            checkedTrackColor = ThemePrimaryColor.copy(alpha = 0.5f)
                        )
                    )
                }

                if (isFabEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                OledCardColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_app_drawer_fab_color),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ColorOptions.standardColors()
                                .filterNot { it == Color.Transparent }
                                .forEach { color ->
                                    ColorOption(
                                        color = color,
                                        isSelected = currentFabColor == color,
                                        onSelect = { fabManager.setFabColor(color) }
                                    )
                                }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                OledCardColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_app_drawer_fab_position),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (fabPosition == FabPosition.LEFT) ThemePrimaryColor.copy(alpha = 0.3f)
                                        else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = if (fabPosition == FabPosition.LEFT) 2.dp else 1.dp,
                                        color = if (fabPosition == FabPosition.LEFT) ThemePrimaryColor else Color.Gray.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { fabManager.setFabPosition(FabPosition.LEFT) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_app_drawer_fab_position_left),
                                    color = if (fabPosition == FabPosition.LEFT) ThemePrimaryColor else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (fabPosition == FabPosition.LEFT) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (fabPosition == FabPosition.RIGHT) ThemePrimaryColor.copy(alpha = 0.3f)
                                        else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = if (fabPosition == FabPosition.RIGHT) 2.dp else 1.dp,
                                        color = if (fabPosition == FabPosition.RIGHT) ThemePrimaryColor else Color.Gray.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { fabManager.setFabPosition(FabPosition.RIGHT) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_app_drawer_fab_position_right),
                                    color = if (fabPosition == FabPosition.RIGHT) ThemePrimaryColor else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (fabPosition == FabPosition.RIGHT) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    if (pageCount > 1) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    OledCardColor,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_app_drawer_fab_page_visibility),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.settings_app_drawer_fab_page_visibility_description),
                                color = Color.Gray.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (pageIndex in 0 until pageCount) {
                                    val isPageVisible =
                                        !fabExplicitPages || fabVisiblePages.contains(pageIndex)
                                    PageVisibilityOption(
                                        pageIndex = pageIndex,
                                        isVisible = isPageVisible,
                                        onToggle = {
                                            fabManager.togglePageVisibility(
                                                pageIndex,
                                                pageCount
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            ThemePrimaryColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onExpandChanged(false) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.settings_grid_done),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
