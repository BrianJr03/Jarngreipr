package jr.brian.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import jr.brian.home.model.AppInfo
import jr.brian.home.ui.theme.ThemeAccentColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridItem(
    app: AppInfo,
    keyboardVisible: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onNavigateUp: () -> Unit = {},
    onNavigateDown: () -> Unit = {},
    onNavigateLeft: () -> Unit = {},
    onNavigateRight: () -> Unit = {},
    onFocusChanged: () -> Unit = {},
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = rememberAsyncImagePainter(model = app.icon),
            contentDescription = app.label,
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
                    .combinedClickable(
                        onClick = {
                            onClick()
                        },
                        onLongClick = {
                            onLongClick()
                        },
                    )
                    .focusable()
                    .scale(animatedFocusedScale(isFocused)),
        )

        if (!keyboardVisible) {
            Spacer(Modifier.height(12.dp))
        }

        AnimatedVisibility(isFocused) {
            HorizontalDivider(color = ThemeAccentColor, thickness = 4.dp)
        }
    }
}