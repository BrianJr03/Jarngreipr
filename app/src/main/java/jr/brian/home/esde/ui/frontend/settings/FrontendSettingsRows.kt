package jr.brian.home.esde.ui.frontend.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.setFrontendFloatIntensity
import jr.brian.home.esde.data.setFrontendFocusBackgroundDimGames
import jr.brian.home.esde.data.setFrontendFocusBackgroundDimSystems
import jr.brian.home.esde.data.setFrontendFocusBackgroundEnabled
import jr.brian.home.esde.data.setFrontendFocusBackgroundGames
import jr.brian.home.esde.data.setFrontendFocusBackgroundSystems
import jr.brian.home.esde.data.setFrontendFocusHapticEnabled
import jr.brian.home.esde.data.setFrontendGameRowAlignment
import jr.brian.home.esde.data.setFrontendGameTileScale
import jr.brian.home.esde.data.setFrontendHintsVisible
import jr.brian.home.esde.data.setFrontendSystemRowAlignment
import jr.brian.home.esde.data.setFrontendSystemTileScale
import jr.brian.home.esde.data.setFrontendTransition
import jr.brian.home.esde.data.setFrontendTransitionMs
import jr.brian.home.esde.data.setGameLayout
import jr.brian.home.esde.data.setSecondaryMediaEnabled
import jr.brian.home.esde.data.setSystemLayout
import jr.brian.home.esde.model.ESDEPrefsState
import jr.brian.home.esde.model.FRONTEND_TILE_SCALE_MAX
import jr.brian.home.esde.model.FRONTEND_TILE_SCALE_MIN
import jr.brian.home.esde.model.FRONTEND_TILE_SCALE_STEP
import jr.brian.home.esde.model.FRONTEND_TRANSITION_MS_MAX
import jr.brian.home.esde.model.FRONTEND_TRANSITION_MS_MIN
import jr.brian.home.esde.model.FRONTEND_TRANSITION_MS_STEP
import jr.brian.home.esde.model.FrontendLayout
import jr.brian.home.esde.model.FrontendRowAlignment
import jr.brian.home.esde.model.FrontendTransition
import jr.brian.home.esde.ui.components.SliderSetting
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.esde.ui.components.focusableSettingCard
import jr.brian.home.ui.theme.ThemePrimaryColor

@Composable
internal fun FrontendSettingsRows(
    category: FrontendSettingsCategory,
    prefsState: ESDEPrefsState,
    prefsManager: ESDEPreferencesManager,
    focusedRow: Int,
    onOpenSystemFilter: () -> Unit,
    onOpenAddSystems: () -> Unit,
    refreshRunning: Boolean,
    lastRefreshResult: Pair<Int, Int>?,
    onRefresh: () -> Unit
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
            onOpenAddSystems = onOpenAddSystems,
            onOpenSystemFilter = onOpenSystemFilter
        )
        FrontendSettingsCategory.SCRAPING -> ScrapingRows(
            focusedRow = focusedRow,
            refreshRunning = refreshRunning,
            lastRefreshResult = lastRefreshResult,
            onRefresh = onRefresh
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
    val systemAlignFocused = focusedRow == 1
    val systemSizeFocused = focusedRow == 2
    val gameLayoutFocused = focusedRow == 3
    val gameAlignFocused = focusedRow == 4
    val gameSizeFocused = focusedRow == 5

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
    RowAlignmentChoiceRow(
        focused = systemAlignFocused,
        enabled = systemIsRow,
        title = stringResource(R.string.frontend_settings_system_row_alignment_title),
        description = stringResource(R.string.frontend_settings_system_row_alignment_description),
        current = prefsState.frontendSystemRowAlignment,
        onChange = prefsManager::setFrontendSystemRowAlignment
    )
    TileSizeSliderRow(
        focused = systemSizeFocused,
        title = stringResource(R.string.frontend_settings_system_tile_size_title),
        description = stringResource(R.string.frontend_settings_system_tile_size_description),
        value = prefsState.frontendSystemTileScale,
        onChange = prefsManager::setFrontendSystemTileScale
    )
    SettingsRowSlot(focused = gameLayoutFocused, onActivate = toggleGameLayout) {
        ToggleSetting(
            title = stringResource(R.string.frontend_layout_games_row_title),
            description = stringResource(R.string.frontend_layout_games_row_description),
            checked = gameIsRow,
            onCheckedChange = { toggleGameLayout() },
            focused = gameLayoutFocused
        )
    }
    RowAlignmentChoiceRow(
        focused = gameAlignFocused,
        enabled = gameIsRow,
        title = stringResource(R.string.frontend_settings_game_row_alignment_title),
        description = stringResource(R.string.frontend_settings_game_row_alignment_description),
        current = prefsState.frontendGameRowAlignment,
        onChange = prefsManager::setFrontendGameRowAlignment
    )
    TileSizeSliderRow(
        focused = gameSizeFocused,
        title = stringResource(R.string.frontend_settings_game_tile_size_title),
        description = stringResource(R.string.frontend_settings_game_tile_size_description),
        value = prefsState.frontendGameTileScale,
        onChange = prefsManager::setFrontendGameTileScale
    )
}

@Composable
private fun RowAlignmentChoiceRow(
    focused: Boolean,
    enabled: Boolean,
    title: String,
    description: String,
    current: FrontendRowAlignment,
    onChange: (FrontendRowAlignment) -> Unit
) {
    val entries = FrontendRowAlignment.entries
    val advance: (Int) -> Unit = { delta ->
        val currentIndex = entries.indexOf(current).let { if (it < 0) 0 else it }
        val size = entries.size
        val nextIndex = ((currentIndex + delta) % size + size) % size
        val next = entries[nextIndex]
        if (next != current) onChange(next)
    }

    RegisterForHorizontalSteps(focused && enabled)
    StepOnHorizontal(focused && enabled) { delta -> if (delta != 0) advance(delta) }
    ActivateOnConfirm(focused = focused && enabled) { advance(1) }

    Box(modifier = Modifier
        .fillMaxWidth()
        .revealWhenFocused(focused)
        .alpha(if (enabled) 1f else 0.4f)
    ) {
        ChoiceRowCard(
            title = title,
            description = description,
            valueLabel = stringResource(rowAlignmentLabelResFor(current)),
            focused = focused,
            enabled = enabled
        )
    }
}

private fun rowAlignmentLabelResFor(alignment: FrontendRowAlignment): Int = when (alignment) {
    FrontendRowAlignment.Top -> R.string.frontend_row_alignment_top
    FrontendRowAlignment.Center -> R.string.frontend_row_alignment_center
    FrontendRowAlignment.Bottom -> R.string.frontend_row_alignment_bottom
}

@Composable
private fun TileSizeSliderRow(
    focused: Boolean,
    title: String,
    description: String,
    value: Float,
    onChange: (Float) -> Unit
) {
    val range = FRONTEND_TILE_SCALE_MIN..FRONTEND_TILE_SCALE_MAX
    val intervals = kotlin.math.round(
        (FRONTEND_TILE_SCALE_MAX - FRONTEND_TILE_SCALE_MIN) / FRONTEND_TILE_SCALE_STEP
    ).toInt()
    val steps = (intervals - 1).coerceAtLeast(0)
    Box(modifier = Modifier
        .fillMaxWidth()
        .revealWhenFocused(focused)
    ) {
        SliderSetting(
            title = title,
            description = description,
            value = value,
            valueRange = range,
            steps = steps,
            valueText = stringResource(
                R.string.frontend_settings_tile_size_value,
                (value * 100).toInt()
            ),
            onValueChange = onChange,
            focused = focused
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
    val systemsDimFocused = focusedRow == 3
    val useGamesFocused = focusedRow == 4
    val gamesDimFocused = focusedRow == 5
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

    val systemsDimEnabled = focusBgEnabled && useSystems
    SettingsRowSlot(
        focused = systemsDimFocused,
        enabled = systemsDimEnabled,
        onActivate = { /* no-op: A on slider */ }
    ) {
        SliderSetting(
            title = stringResource(R.string.frontend_settings_focus_background_dim_systems_title),
            description = stringResource(R.string.frontend_settings_focus_background_dim_systems_description),
            value = prefsState.frontendFocusBackgroundDimSystems,
            valueRange = 0f..1f,
            steps = 19,
            enabled = systemsDimEnabled,
            valueText = stringResource(
                R.string.frontend_settings_percent_value,
                (prefsState.frontendFocusBackgroundDimSystems * 100).toInt()
            ),
            onValueChange = prefsManager::setFrontendFocusBackgroundDimSystems,
            focused = systemsDimFocused
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

    val gamesDimEnabled = focusBgEnabled && useGames
    SettingsRowSlot(
        focused = gamesDimFocused,
        enabled = gamesDimEnabled,
        onActivate = { /* no-op: A on slider */ }
    ) {
        SliderSetting(
            title = stringResource(R.string.frontend_settings_focus_background_dim_games_title),
            description = stringResource(R.string.frontend_settings_focus_background_dim_games_description),
            value = prefsState.frontendFocusBackgroundDimGames,
            valueRange = 0f..1f,
            steps = 19,
            enabled = gamesDimEnabled,
            valueText = stringResource(
                R.string.frontend_settings_percent_value,
                (prefsState.frontendFocusBackgroundDimGames * 100).toInt()
            ),
            onValueChange = prefsManager::setFrontendFocusBackgroundDimGames,
            focused = gamesDimFocused
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
    val transitionFocused = focusedRow == 2
    val speedFocused = focusedRow == 3
    val hintsFocused = focusedRow == 4

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

    TransitionChoiceRow(
        focused = transitionFocused,
        current = prefsState.frontendTransition,
        onChange = prefsManager::setFrontendTransition
    )

    val transitionAnimated = prefsState.frontendTransition != FrontendTransition.None
    TransitionSpeedRow(
        focused = speedFocused,
        enabled = transitionAnimated,
        value = prefsState.frontendTransitionMs,
        onChange = prefsManager::setFrontendTransitionMs
    )

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
private fun TransitionChoiceRow(
    focused: Boolean,
    current: FrontendTransition,
    onChange: (FrontendTransition) -> Unit
) {
    val entries = FrontendTransition.entries
    val advance: (Int) -> Unit = { delta ->
        val currentIndex = entries.indexOf(current).let { if (it < 0) 0 else it }
        val size = entries.size
        val nextIndex = ((currentIndex + delta) % size + size) % size
        val next = entries[nextIndex]
        if (next != current) onChange(next)
    }

    RegisterForHorizontalSteps(focused)
    StepOnHorizontal(focused) { delta -> if (delta != 0) advance(delta) }
    ActivateOnConfirm(focused = focused) { advance(1) }

    Box(modifier = Modifier
        .fillMaxWidth()
        .revealWhenFocused(focused)
    ) {
        ChoiceRowCard(
            title = stringResource(R.string.frontend_settings_transition_title),
            description = stringResource(R.string.frontend_settings_transition_description),
            valueLabel = stringResource(labelResFor(current)),
            focused = focused,
            enabled = true
        )
    }
}

@Composable
private fun TransitionSpeedRow(
    focused: Boolean,
    enabled: Boolean,
    value: Int,
    onChange: (Int) -> Unit
) {
    // SliderSetting handles horizontal-step registration itself when focused+enabled.
    // Passing focused=false when disabled prevents it from consuming Left/Right,
    // matching the row-is-inert-when-disabled requirement without needing a wrapper
    // register/step here (which would double-fire per press).
    Box(modifier = Modifier
        .fillMaxWidth()
        .revealWhenFocused(focused)
        .alpha(if (enabled) 1f else 0.4f)
    ) {
        SliderSetting(
            title = stringResource(R.string.frontend_settings_transition_speed_title),
            description = stringResource(R.string.frontend_settings_transition_speed_description),
            value = value.toFloat(),
            valueRange = FRONTEND_TRANSITION_MS_MIN.toFloat()..FRONTEND_TRANSITION_MS_MAX.toFloat(),
            steps = ((FRONTEND_TRANSITION_MS_MAX - FRONTEND_TRANSITION_MS_MIN) / FRONTEND_TRANSITION_MS_STEP) - 1,
            enabled = enabled,
            valueText = stringResource(R.string.frontend_settings_transition_speed_value, value),
            onValueChange = { newValue ->
                if (enabled) onChange(newValue.toInt())
            },
            focused = if (enabled) focused else false
        )
    }
}

private fun labelResFor(transition: FrontendTransition): Int = when (transition) {
    FrontendTransition.None -> R.string.frontend_transition_none
    FrontendTransition.Fade -> R.string.frontend_transition_fade
    FrontendTransition.Slide -> R.string.frontend_transition_slide
    FrontendTransition.Zoom -> R.string.frontend_transition_zoom
    FrontendTransition.SlideUp -> R.string.frontend_transition_slide_up
}

@Composable
private fun ChoiceRowCard(
    title: String,
    description: String,
    valueLabel: String,
    focused: Boolean,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusableSettingCard(focused)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (focused && enabled) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = ThemePrimaryColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(2.dp))
            }
            Text(
                text = valueLabel,
                color = if (enabled) ThemePrimaryColor else ThemePrimaryColor.copy(alpha = 0.4f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (focused && enabled) {
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = ThemePrimaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SystemsRows(
    focusedRow: Int,
    onOpenAddSystems: () -> Unit,
    onOpenSystemFilter: () -> Unit
) {
    val addFocused = focusedRow == 0
    val filterFocused = focusedRow == 1
    SettingsRowSlot(focused = addFocused, onActivate = onOpenAddSystems) {
        ToggleSetting(
            title = stringResource(R.string.frontend_settings_add_systems_title),
            description = stringResource(R.string.frontend_settings_add_systems_description),
            checked = false,
            showToggle = false,
            onClick = onOpenAddSystems,
            focused = addFocused
        )
    }
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

@Composable
private fun ScrapingRows(
    focusedRow: Int,
    refreshRunning: Boolean,
    lastRefreshResult: Pair<Int, Int>?,
    onRefresh: () -> Unit
) {
    val refreshFocused = focusedRow == 0
    val screenScraperFocused = focusedRow == 1
    val steamGridDbFocused = focusedRow == 2

    val trailing = when {
        refreshRunning -> stringResource(R.string.frontend_settings_refresh_running)
        lastRefreshResult != null -> stringResource(
            R.string.frontend_settings_refresh_result,
            lastRefreshResult.first,
            lastRefreshResult.second
        )
        else -> null
    }
    SettingsRowSlot(focused = refreshFocused, onActivate = onRefresh) {
        ActionRowCard(
            title = stringResource(R.string.frontend_settings_refresh_library_title),
            description = stringResource(R.string.frontend_settings_refresh_library_description),
            trailingLabel = trailing,
            showProgress = refreshRunning,
            focused = refreshFocused
        )
    }

    SettingsRowSlot(focused = screenScraperFocused, enabled = false, onActivate = {}) {
        ComingSoonRow(
            title = stringResource(R.string.frontend_settings_screenscraper_title),
            description = stringResource(R.string.frontend_settings_screenscraper_description),
            focused = screenScraperFocused
        )
    }
    SettingsRowSlot(focused = steamGridDbFocused, enabled = false, onActivate = {}) {
        ComingSoonRow(
            title = stringResource(R.string.frontend_settings_steamgriddb_title),
            description = stringResource(R.string.frontend_settings_steamgriddb_description),
            focused = steamGridDbFocused
        )
    }
}

@Composable
private fun ActionRowCard(
    title: String,
    description: String,
    trailingLabel: String?,
    showProgress: Boolean,
    focused: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusableSettingCard(focused)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
        if (showProgress || trailingLabel != null) {
            Spacer(Modifier.width(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showProgress) {
                    CircularProgressIndicator(
                        color = ThemePrimaryColor,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                if (trailingLabel != null) {
                    Text(
                        text = trailingLabel,
                        color = ThemePrimaryColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ComingSoonRow(
    title: String,
    description: String,
    focused: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusableSettingCard(focused)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.frontend_settings_coming_soon),
            color = ThemePrimaryColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
