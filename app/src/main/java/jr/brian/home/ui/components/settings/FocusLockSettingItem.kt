package jr.brian.home.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import jr.brian.home.R
import jr.brian.home.data.FocusLockPrefsManager
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.util.ThorDetection

/**
 * Extras-section toggle for the "Auto-manage Focus Lock" preference.
 * When on (default), [jr.brian.home.data.ScreenFocusLockWriter] pins
 * AYN's gamepad focus lock to whichever display the user is actively
 * on — the frontend after a screen swap, the launched app on tap, the
 * primary display while a game is running. When off, we leave AYN's
 * Focus Lock alone and the user manages it manually via the Dual
 * Screen Assistant tile.
 *
 * Thor-only — AYN's Focus Lock doesn't exist on other hardware, so
 * surfacing the toggle there would just confuse.
 */
@Composable
fun FocusLockSettingItem() {
    if (!ThorDetection.isThor()) return

    val context = LocalContext.current
    val prefs = remember(context) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, FocusLockPrefsEntryPoint::class.java)
            .focusLockPrefsManager()
    }
    val enabled by prefs.autoManageEnabled.collectAsStateWithLifecycle()

    ToggleSetting(
        title = stringResource(R.string.focus_lock_auto_manage_title),
        description = stringResource(R.string.focus_lock_auto_manage_description),
        checked = enabled,
        onCheckedChange = { prefs.setAutoManageEnabled(it) },
    )
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface FocusLockPrefsEntryPoint {
    fun focusLockPrefsManager(): FocusLockPrefsManager
}
