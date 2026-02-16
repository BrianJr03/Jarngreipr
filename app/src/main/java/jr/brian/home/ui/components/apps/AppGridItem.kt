package jr.brian.home.ui.components.apps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.animations.onPressScaleAndOffset
import jr.brian.home.ui.components.settings.AppName
import jr.brian.home.ui.extensions.handleFullNavigation
import jr.brian.home.ui.extensions.pressWithHaptic
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalCustomIconManager

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridItem(
    app: AppInfo,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onDoubleClick: () -> Unit = {},
    onNavigateUp: () -> Unit = {},
    onNavigateDown: () -> Unit = {},
    onNavigateLeft: () -> Unit = {},
    onNavigateRight: () -> Unit = {},
    onFocusChanged: () -> Unit = {},
) {
    var isFocused by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    val appVisibilityManager = LocalAppVisibilityManager.current
    val customIconManager = LocalCustomIconManager.current
    val haptic = LocalHapticFeedback.current
    val (pressScale, pressOffsetY) = onPressScaleAndOffset(isPressed)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset(y = pressOffsetY)
            .scale(pressScale)
    ) {
        Box {
            AppIconImage(
                defaultIcon = app.icon,
                packageName = app.packageName,
                contentDescription = stringResource(R.string.app_icon_description, app.label),
                customIconManager = customIconManager,
                modifier =
                    Modifier
                        .size(64.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            if (it.isFocused && !isFocused) {
                                onFocusChanged()
                            }
                            isFocused = it.isFocused
                        }
                        .handleFullNavigation(
                            onNavigateUp = onNavigateUp,
                            onNavigateDown = onNavigateDown,
                            onNavigateLeft = onNavigateLeft,
                            onNavigateRight = onNavigateRight,
                            onEnterPress = {
                                onClick()
                            },
                            onMenuPress = {
                                onLongClick()
                            }
                        )
                        .pressWithHaptic(
                            onClick, onDoubleClick, onLongClick,
                            haptic = haptic,
                            onPressChange = { isPressed = it }
                        )
                        .combinedClickable(
                            onClick = {
                                onClick()
                            },
                            onDoubleClick = {
                                onDoubleClick()
                            },
                            onLongClick = {
                                onLongClick()
                            },
                        )
                        .focusable()
            )
            
            NotificationBadge(
                packageName = app.packageName,
                offsetX = 4.dp,
                offsetY = (-4).dp
            )
        }

        Spacer(Modifier.height(4.dp))

        if (appVisibilityManager.showAppNames) {
            app.AppName()
        }

        val dividerAlpha by animateFloatAsState(
            targetValue = if (isFocused) 1f else 0f,
            label = "dividerAlpha"
        )

        HorizontalDivider(
            color = ThemePrimaryColor,
            thickness = 4.dp,
            modifier = Modifier.alpha(dividerAlpha)
        )
    }
}