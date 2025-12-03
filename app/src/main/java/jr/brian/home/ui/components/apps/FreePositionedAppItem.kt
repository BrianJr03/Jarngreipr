package jr.brian.home.ui.components.apps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import jr.brian.home.R
import jr.brian.home.model.AppInfo
import jr.brian.home.ui.extensions.handleFullNavigation
import jr.brian.home.ui.theme.ThemePrimaryColor
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
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
    onNavigateUp: () -> Unit = {},
    onNavigateDown: () -> Unit = {},
    onNavigateLeft: () -> Unit = {},
    onNavigateRight: () -> Unit = {},
    onFocusChanged: () -> Unit = {},
    isDraggingEnabled: Boolean = true,
) {
    var isFocused by remember { mutableStateOf(false) }
    var currentOffsetX by remember(offsetX) { mutableStateOf(offsetX) }
    var currentOffsetY by remember(offsetY) { mutableStateOf(offsetY) }

    Box(
        modifier = Modifier
            .offset { IntOffset(currentOffsetX.roundToInt(), currentOffsetY.roundToInt()) }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = rememberAsyncImagePainter(model = app.icon),
                contentDescription = stringResource(R.string.app_icon_description, app.label),
                modifier = Modifier
                    .size(64.dp)
                    .then(
                        if (isDraggingEnabled) {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
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
                    .combinedClickable(
                        onClick = {
                            onClick()
                        },
                        onLongClick = {
                            onLongClick()
                        },
                    )
                    .focusable()
            )

            if (!keyboardVisible) {
                Spacer(Modifier.height(12.dp))
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
}
