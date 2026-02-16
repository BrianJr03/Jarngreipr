package jr.brian.home.ui.components.dock

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.data.DockSize
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.animations.animatedDockItemAlpha
import jr.brian.home.ui.animations.animatedDockItemScale
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.animations.dockItemEnterAnimation
import jr.brian.home.ui.animations.onPressScaleAndOffset
import jr.brian.home.ui.extensions.pressWithHaptic
import jr.brian.home.ui.components.apps.AppIconImage
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.managers.LocalCustomIconManager
import jr.brian.home.ui.theme.managers.LocalDockManager

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun AppDock(
    apps: List<AppInfo>,
    modifier: Modifier = Modifier,
    onAppClick: (AppInfo) -> Unit,
    onAppDoubleClick: (AppInfo) -> Unit = {},
    onAppLongClick: (AppInfo) -> Unit,
    onEmptySlotClick: (Int) -> Unit,
    onEmptySlotLongClick: (Int) -> Unit,
    onDockPositioned: ((Float) -> Unit)? = null
) {
    val dockManager = LocalDockManager.current
    val dockPackageNames by dockManager.dockApps.collectAsStateWithLifecycle()
    val dockColor by dockManager.dockColor.collectAsStateWithLifecycle()
    val dockSize by dockManager.dockSize.collectAsStateWithLifecycle()

    val totalSlots = dockPackageNames.size
    val currentSlotCount = maxOf(1, totalSlots)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .onGloballyPositioned { coordinates ->
                onDockPositioned?.invoke(coordinates.positionInWindow().y)
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = dockColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = dockSize.padding, vertical = dockSize.padding)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until currentSlotCount) {
                    if (i > 0) {
                        Spacer(Modifier.width(dockSize.spacing))
                    }

                    val packageName = dockPackageNames.getOrNull(i)?.takeIf { it.isNotEmpty() }
                    val app = packageName?.let { pkgName ->
                        apps.find { it.packageName == pkgName }
                    }

                    key(packageName ?: "empty_$i") {
                        if (app != null) {
                            DockAppItem(
                                app = app,
                                size = dockSize,
                                onClick = { onAppClick(app) },
                                onDoubleClick = { onAppDoubleClick(app) },
                                onLongClick = { onAppLongClick(app) }
                            )
                        } else {
                            DockEmptySlot(
                                size = dockSize,
                                onClick = { onEmptySlotClick(i) },
                                onLongClick = { onEmptySlotLongClick(i) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DockAppItem(
    app: AppInfo,
    size: DockSize,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit = {},
    onLongClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    val enterScale = animatedDockItemScale()
    val enterAlpha = animatedDockItemAlpha()
    val customIconManager = LocalCustomIconManager.current
    val haptic = LocalHapticFeedback.current
    val (pressScale, pressOffsetY) = onPressScaleAndOffset(isPressed)

    Box(
        modifier = Modifier
            .size(size.containerSize)
            .offset(y = pressOffsetY)
            .scale(pressScale)
            .dockItemEnterAnimation(
                scale = enterScale,
                alpha = enterAlpha
            )
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White.copy(alpha = 0.8f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .pressWithHaptic(
                onClick, onDoubleClick, onLongClick,
                haptic = haptic,
                onPressChange = { isPressed = it }
            )
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick,
                onLongClick = onLongClick
            )
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        AppIconImage(
            defaultIcon = app.icon,
            packageName = app.packageName,
            contentDescription = stringResource(R.string.app_icon_description, app.label),
            customIconManager = customIconManager,
            modifier = Modifier.size(size.iconSize)
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DockEmptySlot(
    size: DockSize,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    val enterScale = animatedDockItemScale()
    val enterAlpha = animatedDockItemAlpha()
    val haptic = LocalHapticFeedback.current
    val (pressScale, pressOffsetY) = onPressScaleAndOffset(isPressed)

    Box(
        modifier = Modifier
            .size(size.containerSize)
            .offset(y = pressOffsetY)
            .scale(pressScale)
            .dockItemEnterAnimation(
                scale = enterScale,
                alpha = enterAlpha
            )
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                color = OledCardColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.5.dp,
                color = if (isFocused) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .pressWithHaptic(
                onClick, onLongClick,
                haptic = haptic,
                onPressChange = { isPressed = it }
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .focusable(),
        contentAlignment = Alignment.Center
    ) {}
}
