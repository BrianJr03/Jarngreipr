package jr.brian.home.esde.ui.frontend.settings

import androidx.compose.runtime.Composable

typealias FrontendSettingsState = RailCursorState<FrontendSettingsCategory>

@Composable
fun rememberFrontendSettingsState(
    initialCategory: FrontendSettingsCategory = FrontendSettingsCategory.LAYOUT
): FrontendSettingsState = rememberRailCursorState(
    entries = FrontendSettingsCategory.entries,
    initial = initialCategory
)
