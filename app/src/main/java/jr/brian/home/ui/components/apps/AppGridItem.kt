package jr.brian.home.ui.components.apps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.data.CustomIconManager
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.extensions.handleFullNavigation
import jr.brian.home.ui.theme.ThemePrimaryColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridItem(
    app: AppInfo,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onNavigateUp: () -> Unit = {},
    onNavigateDown: () -> Unit = {},
    onNavigateLeft: () -> Unit = {},
    onNavigateRight: () -> Unit = {},
    onFocusChanged: () -> Unit = {},
    customIconManager: CustomIconManager? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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