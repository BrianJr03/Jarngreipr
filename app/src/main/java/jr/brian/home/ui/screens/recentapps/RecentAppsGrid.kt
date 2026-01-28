package jr.brian.home.ui.screens.recentapps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.model.app.RecentAppInfo
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

/**
 * Grid layout for displaying recent apps
 */
@Composable
fun RecentAppsGrid(
    recentApps: List<RecentAppInfo>,
    appDisplayPreferenceManager: AppDisplayPreferenceManager,
    onAppClick: (RecentAppInfo) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 170.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(recentApps, key = { _, app -> app.packageName }) { index, app ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300, delayMillis = index * 30)) +
                        slideInVertically(tween(300, delayMillis = index * 30)) { it / 4 }
            ) {
                RecentAppCard(
                    app = app,
                    appDisplayPreferenceManager = appDisplayPreferenceManager,
                    onClick = { onAppClick(app) }
                )
            }
        }
    }
}

/**
 * Card component for displaying individual recent app with display preferences
 */
@Composable
fun RecentAppCard(
    app: RecentAppInfo,
    appDisplayPreferenceManager: AppDisplayPreferenceManager,
    onClick: () -> Unit
) {
    var currentPreference by remember {
        mutableStateOf(appDisplayPreferenceManager.getAppDisplayPreference(app.packageName))
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(100),
        label = "cardScale"
    )

    val cardGradient = Brush.verticalGradient(
        colors = listOf(
            OledCardColor.copy(alpha = 0.9f),
            OledCardColor.copy(alpha = 0.6f)
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .background(cardGradient)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = app.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                AppIconContainer(
                    icon = app.icon,
                    label = app.label
                )

                Spacer(modifier = Modifier.height(8.dp))

                UsageTimeBadge(usageTimeMs = app.usageTimeMs)

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DisplayButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.KeyboardArrowUp,
                        label = stringResource(R.string.recent_apps_display_top),
                        isSelected = currentPreference == DisplayPreference.PRIMARY_DISPLAY,
                        selectedColor = ThemePrimaryColor,
                        onClick = {
                            appDisplayPreferenceManager.setAppDisplayPreference(
                                app.packageName,
                                DisplayPreference.PRIMARY_DISPLAY
                            )
                            currentPreference = DisplayPreference.PRIMARY_DISPLAY
                        }
                    )

                    DisplayButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.KeyboardArrowDown,
                        label = stringResource(R.string.recent_apps_display_bottom),
                        isSelected = currentPreference == DisplayPreference.CURRENT_DISPLAY,
                        selectedColor = ThemeSecondaryColor,
                        onClick = {
                            appDisplayPreferenceManager.setAppDisplayPreference(
                                app.packageName,
                                DisplayPreference.CURRENT_DISPLAY
                            )
                            currentPreference = DisplayPreference.CURRENT_DISPLAY
                        }
                    )
                }
            }
        }
    }
}
