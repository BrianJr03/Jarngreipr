package jr.brian.home.ui.components.apps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.util.rememberAutoFocus

@Composable
fun rememberAppOptionsMenuFocusRequesters(
    hasResizeOption: Boolean,
    hasExternalDisplay: Boolean,
    isInDock: Boolean = false
): List<FocusRequester> {
    val firstFocusRequester = rememberAutoFocus()

    return remember(
        firstFocusRequester,
        hasResizeOption,
        hasExternalDisplay,
        isInDock
    ) {
        buildList {
            add(firstFocusRequester)
            add(FocusRequester())
            add(FocusRequester())
            if (isInDock) {
                add(FocusRequester())
            }
            if (hasResizeOption) {
                add(FocusRequester())
            }
            if (hasExternalDisplay) {
                add(FocusRequester())
                add(FocusRequester())
            }
        }
    }
}

/**
 * Convenience overload that determines hasResizeOption from app being non-null
 */
@Composable
fun rememberAppOptionsMenuFocusRequesters(
    app: AppInfo?,
    hasExternalDisplay: Boolean
): List<FocusRequester> {
    return rememberAppOptionsMenuFocusRequesters(
        hasResizeOption = app != null,
        hasExternalDisplay = hasExternalDisplay
    )
}
