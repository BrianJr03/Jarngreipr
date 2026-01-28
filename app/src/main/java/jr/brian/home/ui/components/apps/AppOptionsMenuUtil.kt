package jr.brian.home.ui.components.apps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.util.rememberAutoFocus

@Composable
fun rememberAppOptionsMenuFocusRequesters(
    hasResizeOption: Boolean,
    hasExternalDisplay: Boolean
): List<FocusRequester> {
    val firstFocusRequester = rememberAutoFocus()

    return remember(
        firstFocusRequester,
        hasResizeOption,
        hasExternalDisplay
    ) {
        buildList {
            add(firstFocusRequester)  // Info (auto-focuses)
            add(FocusRequester())  // Hide
            add(FocusRequester())  // Icon
            if (hasResizeOption) {
                add(FocusRequester())  // Resize
            }
            if (hasExternalDisplay) {
                add(FocusRequester())  // Top Display
                add(FocusRequester())  // Bottom Display
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
