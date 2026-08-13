package jr.brian.home.esde.ui.frontend.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import jr.brian.home.R
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.setFrontendFloatIntensity
import jr.brian.home.esde.data.setFrontendFocusBackgroundDim
import jr.brian.home.esde.data.setFrontendFocusBackgroundEnabled
import jr.brian.home.esde.data.setFrontendFocusBackgroundGames
import jr.brian.home.esde.data.setFrontendFocusBackgroundSystems
import jr.brian.home.esde.data.setFrontendFocusHapticEnabled
import jr.brian.home.esde.data.setFrontendHintsVisible
import jr.brian.home.esde.data.setGameLayout
import jr.brian.home.esde.data.setSecondaryMediaEnabled
import jr.brian.home.esde.data.setSystemLayout
import jr.brian.home.esde.model.ESDEPrefsState
import jr.brian.home.esde.model.FrontendLayout
import jr.brian.home.esde.ui.components.SliderSetting
import jr.brian.home.esde.ui.components.ToggleSetting

@Composable
internal fun FrontendSettingsRows(
    category: FrontendSettingsCategory,
    prefsState: ESDEPrefsState,
    prefsManager: ESDEPreferencesManager,
    focusedRow: Int,
    onOpenSystemFilter: () -> Unit
) {
    when (category) {
        FrontendSettingsCategory.LAYOUT -> LayoutRows(
            prefsState = prefsState,
            prefsManager = prefsManager,
            focusedRow = focusedRow
        )
        FrontendSettingsCategory.MEDIA -> MediaRows(
            prefsState = prefsState,
            prefsManager = prefsManager,
            focusedRow = focusedRow
        )
        FrontendSettingsCategory.FEEL -> FeelRows(
            prefsState = prefsState,
            prefsManager = prefsManager,
            focusedRow = focusedRow
        )
        FrontendSettingsCategory.SYSTEMS -> SystemsRows(
            focusedRow = focusedRow,
            onOpenSystemFilter = onOpenSystemFilter
        )
    }
}

@Composable
private fun SettingsRowSlot(
    focused: Boolean,
    onActivate: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .revealWhenFocused(focused)
        .alpha(if (enabled) 1f else 0.4f)
    ) {
        if (enabled) ActivateOnConfirm(focused = focused, onActivate = onActivate)
        content()
    }
}

@Composable
private fun LayoutRows(
    prefsState: ESDEPrefsState,
    prefsManager: ESDEPreferencesManager,
    focusedRow: Int
) {
    val systemLayoutFocused = focusedRow == 0
    val gameLayoutFocused = focusedRow == 1

    val systemIsRow = prefsState.systemLayout == FrontendLayout.Row
    val gameIsRow = prefsState.gameLayout == FrontendLayout.Row

    val toggleSystemLayout: () -> Unit = {
        prefsManager.setSystemLayout(if (systemIsRow) FrontendLayout.Grid else FrontendLayout.Row)
    }
    val toggleGameLayout: () -> Unit = {
        prefsManager.setGameLayout(if (gameIsRow) FrontendLayout.Grid else FrontendLayout.Row)
    }

    SettingsRowSlot(focused = systemLayoutFocused, onActivate = toggleSystemLayout) {
        ToggleSetting(
            title = stringResource(R.string.frontend_layout_systems_row_title),
            description = stringResource(R.string.frontend_layout_systems_row_description),
            checked = systemIsRow,
            onCheckedChange = { toggleSystemLayout() },
            focused = systemLayoutFocused
        )
    }
    SettingsRowSlot(focused = gameLayoutFocused, onActivate = toggleGameLayout) {
        ToggleSetting(
            title = stringResource(R.string.frontend_layout_games_row_title),
            description = stringResource(R.string.frontend_layout_games_row_description),
            checked = gameIsRow,
            onCheckedChange = { toggleGameLayout() },
            focused = gameLayoutFocused
        )
    }
}

@Composable
private fun MediaRows(
    prefsState: ESDEPrefsState,
    prefsManager: ESDEPreferencesManager,
    focusedRow: Int
) {
    val secondaryFocused = focusedRow == 0
    val focusBgFocused = focusedRow == 1
    val useSystemsFocused = focusedRow == 2
    val useGamesFocused = focusedRow == 3
    val focusBgDimFocused = focusedRow == 4
    val secondaryEnabled = prefsState.secondaryMediaEnabled
    val focusBgEnabled = prefsState.frontendFocusBackgroundEnabled

    SettingsRowSlot(
        focused = secondaryFocused,
        onActivate = { prefsManager.setSecondaryMediaEnabled(!secondaryEnabled) }
    ) {
        ToggleSetting(
            title = stringResource(R.string.secondary_media_title),
            description = stringResource(R.string.secondary_media_description),
            checked = secondaryEnabled,
            onCheckedChange = prefsManager::setSecondaryMediaEnabled,
            focused = secondaryFocused
        )
    }

    SettingsRowSlot(
        focused = focusBgFocused,
        onActivate = { prefsManager.setFrontendFocusBackgroundEnabled(!focusBgEnabled) }
    ) {
        ToggleSetting(
            title = stringResource(R.string.frontend_settings_focus_background_title),
            description = stringResource(R.string.frontend_settings_focus_background_description),
            checked = focusBgEnabled,
            onCheckedChange = prefsManager::setFrontendFocusBackgroundEnabled,
            focused = focusBgFocused
        )
    }

    val useSystems = prefsState.frontendFocusBackgroundSystems
    SettingsRowSlot(
        focused = useSystemsFocused,
        enabled = focusBgEnabled,
        onActivate = { prefsManager.setFrontendFocusBackgroundSystems(!useSystems) }
    ) {
        ToggleSetting(
            title = stringResource(R.string.frontend_settings_focus_background_systems_title),
            description = stringResource(R.string.frontend_settings_focus_background_systems_description),
            checked = useSystems,
            onCheckedChange = prefsManager::setFrontendFocusBackgroundSystems,
            focused = useSystemsFocused
        )
    }

    val useGames = prefsState.frontendFocusBackgroundGames
    SettingsRowSlot(
        focused = useGamesFocused,
        enabled = focusBgEnabled,
        onActivate = { prefsManager.setFrontendFocusBackgroundGames(!useGames) }
    ) {
        ToggleSetting(
            title = stringResource(R.string.frontend_settings_focus_background_games_title),
            description = stringResource(R.string.frontend_settings_focus_background_games_description),
            checked = useGames,
            onCheckedChange = prefsManager::setFrontendFocusBackgroundGames,
            focused = useGamesFocused
        )
    }

    SettingsRowSlot(
        focused = focusBgDimFocused,
        enabled = focusBgEnabled,
        onActivate = { /* no-op: A on slider */ }
    ) {
        SliderSetting(
            title = stringResource(R.string.frontend_settings_focus_background_dim_title),
            description = stringResource(R.string.frontend_settings_focus_background_dim_description),
            value = prefsState.frontendFocusBackgroundDim,
            valueRange = 0f..1f,
            steps = 19,
            enabled = focusBgEnabled,
            valueText = stringResource(
                R.string.frontend_settings_float_intensity_value,
                (prefsState.frontendFocusBackgroundDim * 100).toInt()
            ),
            onValueChange = prefsManager::setFrontendFocusBackgroundDim,
            focused = focusBgDimFocused
        )
    }
}

@Composable
private fun FeelRows(
    prefsState: ESDEPrefsState,
    prefsManager: ESDEPreferencesManager,
    focusedRow: Int
) {
    val sliderFocused = focusedRow == 0
    val hapticFocused = focusedRow == 1
    val hintsFocused = focusedRow == 2

    SettingsRowSlot(focused = sliderFocused, onActivate = { /* no-op: A on slider */ }) {
        SliderSetting(
            title = stringResource(R.string.frontend_settings_float_intensity_title),
            description = stringResource(R.string.frontend_settings_float_intensity_description),
            value = prefsState.frontendFloatIntensity,
            valueRange = 0f..2f,
            steps = 19,
            valueText = stringResource(
                R.string.frontend_settings_float_intensity_value,
                (prefsState.frontendFloatIntensity * 100).toInt()
            ),
            onValueChange = prefsManager::setFrontendFloatIntensity,
            focused = sliderFocused
        )
    }

    val hapticEnabled = prefsState.frontendFocusHapticEnabled
    SettingsRowSlot(
        focused = hapticFocused,
        onActivate = { prefsManager.setFrontendFocusHapticEnabled(!hapticEnabled) }
    ) {
        ToggleSetting(
            title = stringResource(R.string.frontend_settings_focus_haptic_title),
            description = stringResource(R.string.frontend_settings_focus_haptic_description),
            checked = hapticEnabled,
            onCheckedChange = prefsManager::setFrontendFocusHapticEnabled,
            focused = hapticFocused
        )
    }

    val hintsVisible = prefsState.frontendHintsVisible
    SettingsRowSlot(
        focused = hintsFocused,
        onActivate = { prefsManager.setFrontendHintsVisible(!hintsVisible) }
    ) {
        ToggleSetting(
            title = stringResource(R.string.frontend_settings_show_hints_title),
            description = stringResource(R.string.frontend_settings_show_hints_description),
            checked = hintsVisible,
            onCheckedChange = prefsManager::setFrontendHintsVisible,
            focused = hintsFocused
        )
    }
}

@Composable
private fun SystemsRows(
    focusedRow: Int,
    onOpenSystemFilter: () -> Unit
) {
    val filterFocused = focusedRow == 0
    SettingsRowSlot(focused = filterFocused, onActivate = onOpenSystemFilter) {
        ToggleSetting(
            title = stringResource(R.string.frontend_settings_filter_systems_title),
            description = stringResource(R.string.frontend_settings_filter_systems_description),
            checked = false,
            showToggle = false,
            onClick = onOpenSystemFilter,
            focused = filterFocused
        )
    }
}
