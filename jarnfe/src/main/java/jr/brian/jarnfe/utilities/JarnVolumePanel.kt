package jr.brian.jarnfe.utilities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnSliderRow
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.tools.ToolsInitialFocus

/** Follows the system volume only while live, so it holds no observer during a game. */
@Composable
internal fun JarnVolumePanel(utilities: JarnUtilities, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val live = rememberUtilityLive()
    var volume by remember { mutableStateOf<JarnVolume?>(null) }
    val slider = remember { FocusRequester() }
    LaunchedEffect(utilities, live) {
        if (live) utilities.volume.collect { volume = it }
    }
    UtilityLayer(onBack = onClose, bindings = { hint(JarnAction.MoveLeft, R.string.jarnfe_utilities_adjust) }) {
        Column(
            modifier = modifier.fillMaxSize().padding(JarnTheme.space.lg),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        ) {
            UtilityTitle(stringResource(R.string.jarnfe_utilities_volume))
            volume?.let { current ->
                ToolsInitialFocus(slider)
                VolumeSlider(current, slider) { level ->
                    volume = current.copy(level = level)
                    utilities.setVolume(level)
                }
            }
        }
    }
}

@Composable
private fun VolumeSlider(volume: JarnVolume, focus: FocusRequester, onLevel: (Int) -> Unit) {
    val steps = jarnVolumeSteps(volume.max)
    JarnSliderRow(
        label = stringResource(R.string.jarnfe_utilities_volume_media),
        value = volume.level.toFloat(),
        valueText = stringResource(R.string.jarnfe_utilities_volume_value, volume.level, steps),
        onValueChange = { onLevel(jarnVolumeLevel(it, volume.max)) },
        valueRange = 0f..steps.toFloat(),
        steps = steps,
        modifier = Modifier.focusRequester(focus),
    )
}
