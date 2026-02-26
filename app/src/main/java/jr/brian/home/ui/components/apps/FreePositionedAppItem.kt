package jr.brian.home.ui.components.apps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.data.CustomIconManager
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.components.settings.AppName
import jr.brian.home.ui.extensions.combinedClickWithHaptic
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun FreePositionedAppItem(
    app: AppInfo,
    keyboardVisible: Boolean,
    focusRequester: FocusRequester,
    offsetX: Float,
    offsetY: Float,
    onOffsetChanged: (Float, Float) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onDoubleClick: () -> Unit = {},
    onFocusChanged: () -> Unit = {},
    isDraggingEnabled: Boolean = true,
    iconSize: Float = 64f,
    isFocusable: Boolean = false,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    customIconManager: CustomIconManager? = null,
    bubblePopEnabled: Boolean = false,
    onBubblePopTap: (x: Float, y: Float) -> Unit = { _, _ -> },
    onBubblePopComplete: () -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }
    val appVisibilityManager = LocalAppVisibilityManager.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    var currentOffsetX by remember(offsetX) { mutableStateOf(offsetX) }
    var currentOffsetY by remember(offsetY) { mutableStateOf(offsetY) }
    var isPopping by remember { mutableStateOf(false) }
    val popScale by animateFloatAsState(
        targetValue = if (isPopping) 0.1f else 1f,
        animationSpec = tween(durationMillis = 260),
        label = "popScale"
    )
    val popAlpha by animateFloatAsState(
        targetValue = if (isPopping) 0f else 1f,
        animationSpec = tween(durationMillis = 260),
        label = "popAlpha"
    )
    LaunchedEffect(isPopping) {
        if (!isPopping) return@LaunchedEffect
        delay(260)
        onBubblePopComplete()
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(currentOffsetX.roundToInt(), currentOffsetY.roundToInt()) }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                AppIconImage(
                    defaultIcon = app.icon,
                    packageName = app.packageName,
                    contentDescription = stringResource(R.string.app_icon_description, app.label),
                    customIconManager = customIconManager,
                    modifier = Modifier
                        .scale(popScale)
                        .alpha(popAlpha)
                        .size(iconSize.dp)
                        .then(
                            if (isDraggingEnabled) {
                                Modifier.pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { onDragStart() },
                                        onDragEnd = { onDragEnd() },
                                        onDragCancel = { onDragEnd() }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        currentOffsetX += dragAmount.x
                                        currentOffsetY += dragAmount.y
                                        onOffsetChanged(currentOffsetX, currentOffsetY)
                                    }
                                }
                            } else {
                                Modifier
                            }
                        )
                        .then(
                            if (isFocusable) {
                                Modifier
                                    .focusRequester(focusRequester)
                                    .onFocusChanged {
                                        if (it.isFocused && !isFocused) {
                                            onFocusChanged()
                                        }
                                        isFocused = it.isFocused
                                    }
                                    .focusable()
                            } else {
                                Modifier
                            }
                        )
                        .then(
                            if (bubblePopEnabled) {
                                Modifier.pointerInput(isPopping) {
                                    detectTapGestures(
                                        onPress = {
                                            if (!isPopping) {
                                                onBubblePopTap(
                                                    currentOffsetX + (with(density) { iconSize.dp.toPx() } / 2f),
                                                    currentOffsetY + (with(density) { iconSize.dp.toPx() } / 2f)
                                                )
                                                isPopping = true
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                            tryAwaitRelease()
                                        }
                                    )
                                }
                            } else {
                                Modifier.combinedClickWithHaptic(
                                    haptic = haptic,
                                    onClick = onClick,
                                    onDoubleClick = onDoubleClick,
                                    onLongClick = onLongClick
                                )
                            }
                        )
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

            if (!keyboardVisible && isFocusable) {
                Spacer(Modifier.height(12.dp))

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
    }
}
