package jr.brian.home.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.service.HomeInterceptorService
import jr.brian.home.ui.theme.managers.LocalVolumeChordManager
import jr.brian.home.util.ThorDetection
import jr.brian.home.util.ThorVolume

/**
 * Toggle for the Select + Volume Up/Down chord that adjusts the AYN Thor's
 * bottom-screen volume. Mirrors [HomeInterceptionSettingItem] — Thor-only,
 * warns when either the accessibility service or `WRITE_SETTINGS` special
 * access is missing, and re-checks both on lifecycle resume so returning
 * from the system settings screen refreshes the description immediately.
 */
@Composable
fun VolumeChordSettingItem() {
    // Hidden on non-Thor hardware for the same reason as Home Button
    // Interception: `secondary_screen_volume_level` is a Thor firmware key.
    if (!ThorDetection.isThor()) return

    val context = LocalContext.current
    val volumeChordManager = LocalVolumeChordManager.current
    val chordEnabled by volumeChordManager.chordEnabled.collectAsStateWithLifecycle()

    var accessibilityGranted by remember {
        mutableStateOf(HomeInterceptorService.isAccessibilityEnabled(context))
    }
    var writeSettingsGranted by remember {
        mutableStateOf(ThorVolume.canWriteSettings(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityGranted = HomeInterceptorService.isAccessibilityEnabled(context)
                writeSettingsGranted = ThorVolume.canWriteSettings(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val description = when {
        chordEnabled && !accessibilityGranted ->
            stringResource(R.string.volume_chord_needs_accessibility)
        chordEnabled && !writeSettingsGranted ->
            stringResource(R.string.volume_chord_needs_write_settings)
        else -> stringResource(R.string.volume_chord_description)
    }

    ToggleSetting(
        title = stringResource(R.string.volume_chord_title),
        description = description,
        checked = chordEnabled,
        onCheckedChange = { requested ->
            volumeChordManager.setChordEnabled(requested)
            if (requested) {
                if (!accessibilityGranted) {
                    context.startActivity(HomeInterceptorService.accessibilitySettingsIntent())
                } else if (!writeSettingsGranted) {
                    context.startActivity(ThorVolume.manageWriteSettingsIntent(context))
                }
            }
        }
    )
}
