package jr.brian.home.esde.ui.frontend

import jr.brian.home.esde.data.*
import android.view.KeyEvent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.esde.data.LocalESDEPreferencesManager
import jr.brian.home.esde.model.FrontendLayout
import jr.brian.home.esde.ui.components.SliderSetting
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeAccentColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val GAMEPAD_SCROLL_STEP: Dp = 96.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrontendSettingsDialog(onDismiss: () -> Unit) {
    val prefsManager = LocalESDEPreferencesManager.current
    val state by prefsManager.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OledCardColor
    ) {
        SheetBody(
            systemLayout = state.systemLayout,
            gameLayout = state.gameLayout,
            hintsVisible = state.frontendHintsVisible,
            secondaryMediaEnabled = state.secondaryMediaEnabled,
            floatIntensity = state.frontendFloatIntensity,
            focusHapticEnabled = state.frontendFocusHapticEnabled,
            onSystemLayoutChange = prefsManager::setSystemLayout,
            onGameLayoutChange = prefsManager::setGameLayout,
            onHintsVisibleChange = prefsManager::setFrontendHintsVisible,
            onSecondaryMediaChange = prefsManager::setSecondaryMediaEnabled,
            onFloatIntensityChange = prefsManager::setFrontendFloatIntensity,
            onFocusHapticEnabledChange = prefsManager::setFrontendFocusHapticEnabled,
            onClose = onDismiss
        )
    }
}

@Composable
private fun SheetBody(
    systemLayout: FrontendLayout,
    gameLayout: FrontendLayout,
    hintsVisible: Boolean,
    secondaryMediaEnabled: Boolean,
    floatIntensity: Float,
    focusHapticEnabled: Boolean,
    onSystemLayoutChange: (FrontendLayout) -> Unit,
    onGameLayoutChange: (FrontendLayout) -> Unit,
    onHintsVisibleChange: (Boolean) -> Unit,
    onSecondaryMediaChange: (Boolean) -> Unit,
    onFloatIntensityChange: (Float) -> Unit,
    onFocusHapticEnabledChange: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val firstToggleFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { firstToggleFocus.requestFocus() }
    }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val stepPx = with(LocalDensity.current) { GAMEPAD_SCROLL_STEP.toPx() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .onPreviewKeyEvent { event ->
                handleGamepadScroll(event, scrollState, stepPx, scope)
            }
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SheetTitle()
        FrontendSettingsContent(
            systemLayout = systemLayout,
            gameLayout = gameLayout,
            hintsVisible = hintsVisible,
            secondaryMediaEnabled = secondaryMediaEnabled,
            floatIntensity = floatIntensity,
            focusHapticEnabled = focusHapticEnabled,
            onSystemLayoutChange = onSystemLayoutChange,
            onGameLayoutChange = onGameLayoutChange,
            onHintsVisibleChange = onHintsVisibleChange,
            onSecondaryMediaChange = onSecondaryMediaChange,
            onFloatIntensityChange = onFloatIntensityChange,
            onFocusHapticEnabledChange = onFocusHapticEnabledChange,
            firstToggleFocus = firstToggleFocus
        )
        DialogCloseButton(onClick = onClose)
    }
}

private fun handleGamepadScroll(
    event: androidx.compose.ui.input.key.KeyEvent,
    scrollState: ScrollState,
    stepPx: Float,
    scope: CoroutineScope
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    return when (event.nativeKeyEvent.keyCode) {
        KeyEvent.KEYCODE_DPAD_DOWN -> {
            if (scrollState.value < scrollState.maxValue) {
                scope.launch { scrollState.animateScrollBy(stepPx) }
            }
            false
        }
        KeyEvent.KEYCODE_DPAD_UP -> {
            if (scrollState.value > 0) {
                scope.launch { scrollState.animateScrollBy(-stepPx) }
            }
            false
        }
        else -> false
    }
}

private suspend fun ScrollState.animateScrollBy(delta: Float) {
    val target = (value + delta).toInt().coerceIn(0, maxValue)
    animateScrollTo(target)
}

@Composable
private fun SheetTitle() {
    Text(
        text = stringResource(R.string.frontend_settings_title),
        color = Color.White.copy(alpha = 0.9f),
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun DialogCloseButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = stringResource(R.string.frontend_settings_close),
            color = ThemeAccentColor
        )
    }
}

@Composable
private fun FrontendSettingsContent(
    systemLayout: FrontendLayout,
    gameLayout: FrontendLayout,
    hintsVisible: Boolean,
    secondaryMediaEnabled: Boolean,
    floatIntensity: Float,
    focusHapticEnabled: Boolean,
    onSystemLayoutChange: (FrontendLayout) -> Unit,
    onGameLayoutChange: (FrontendLayout) -> Unit,
    onHintsVisibleChange: (Boolean) -> Unit,
    onSecondaryMediaChange: (Boolean) -> Unit,
    onFloatIntensityChange: (Float) -> Unit,
    onFocusHapticEnabledChange: (Boolean) -> Unit,
    firstToggleFocus: FocusRequester
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(modifier = Modifier.focusRequester(firstToggleFocus)) {
            ToggleSetting(
                title = stringResource(R.string.frontend_layout_systems_row_title),
                description = stringResource(R.string.frontend_layout_systems_row_description),
                checked = systemLayout == FrontendLayout.Row,
                onCheckedChange = {
                    onSystemLayoutChange(if (it) FrontendLayout.Row else FrontendLayout.Grid)
                }
            )
        }
        ToggleSetting(
            title = stringResource(R.string.frontend_layout_games_row_title),
            description = stringResource(R.string.frontend_layout_games_row_description),
            checked = gameLayout == FrontendLayout.Row,
            onCheckedChange = {
                onGameLayoutChange(if (it) FrontendLayout.Row else FrontendLayout.Grid)
            }
        )
        ToggleSetting(
            title = stringResource(R.string.frontend_settings_show_hints_title),
            description = stringResource(R.string.frontend_settings_show_hints_description),
            checked = hintsVisible,
            onCheckedChange = onHintsVisibleChange
        )
        ToggleSetting(
            title = stringResource(R.string.frontend_settings_focus_haptic_title),
            description = stringResource(R.string.frontend_settings_focus_haptic_description),
            checked = focusHapticEnabled,
            onCheckedChange = onFocusHapticEnabledChange
        )
        ToggleSetting(
            title = stringResource(R.string.secondary_media_title),
            description = stringResource(R.string.secondary_media_description),
            checked = secondaryMediaEnabled,
            onCheckedChange = onSecondaryMediaChange
        )
        FloatIntensitySlider(
            intensity = floatIntensity,
            onIntensityChange = onFloatIntensityChange
        )
    }
}

@Composable
private fun FloatIntensitySlider(
    intensity: Float,
    onIntensityChange: (Float) -> Unit
) {
    SliderSetting(
        title = stringResource(R.string.frontend_settings_float_intensity_title),
        description = stringResource(R.string.frontend_settings_float_intensity_description),
        value = intensity,
        valueRange = 0f..2f,
        steps = 19,
        valueText = stringResource(
            R.string.frontend_settings_float_intensity_value,
            (intensity * 100).toInt()
        ),
        onValueChange = onIntensityChange
    )
}
