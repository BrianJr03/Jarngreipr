package jr.brian.home.esde.ui.sections

import androidx.compose.runtime.Composable
import jr.brian.home.esde.model.ESDEPrefsState
import jr.brian.home.esde.model.ScreensaverBehavior
import jr.brian.home.esde.ui.components.ScreensaverBehaviorSelector

@Composable
fun ScreensaverSectionContent(
    prefsState: ESDEPrefsState,
    onScreensaverBehaviorChange: (ScreensaverBehavior) -> Unit
) {
    ScreensaverBehaviorSelector(
        selectedBehavior = prefsState.screensaverBehavior,
        onBehaviorSelected = { behavior ->
            onScreensaverBehaviorChange(behavior)
        }
    )
}
