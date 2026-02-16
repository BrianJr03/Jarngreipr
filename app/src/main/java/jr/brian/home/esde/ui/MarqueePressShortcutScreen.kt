package jr.brian.home.esde.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import jr.brian.home.R
import jr.brian.home.esde.preferences.LocalESDEPreferencesManager
import jr.brian.home.model.Shortcut
import jr.brian.home.model.ShortcutOption
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.screens.ShortcutSelectionScreen

@Composable
fun MarqueePressShortcutScreen(
    allApps: List<AppInfo> = emptyList(),
    onDismiss: () -> Unit = {}
) {
    val preferencesManager = LocalESDEPreferencesManager.current
    val prefsState by preferencesManager.state.collectAsState()

    val shortcuts = listOf(
        ShortcutOption(Shortcut.NONE, stringResource(R.string.shortcut_none)),
        ShortcutOption(Shortcut.APP, stringResource(R.string.shortcut_app)),
        ShortcutOption(Shortcut.APP_SEARCH, stringResource(R.string.shortcut_app_search)),
        ShortcutOption(Shortcut.CONTROL_PAD, stringResource(R.string.shortcut_control_pad)),
        ShortcutOption(Shortcut.CUSTOM_THEME, stringResource(R.string.shortcut_custom_theme)),
        ShortcutOption(Shortcut.QUICK_DELETE, stringResource(R.string.shortcut_quick_delete)),
        ShortcutOption(Shortcut.SETTINGS, stringResource(R.string.shortcut_settings)),
        ShortcutOption(Shortcut.MONITOR, stringResource(R.string.shortcut_monitor)),
        ShortcutOption(Shortcut.VOLUME_CONTROLS, stringResource(R.string.shortcut_volume_controls)),
        ShortcutOption(Shortcut.RECENT_APPS, stringResource(R.string.shortcut_recent_apps)),
        ShortcutOption(Shortcut.POWERED_OFF, stringResource(R.string.shortcut_powered_off))
    )

    ShortcutSelectionScreen(
        title = stringResource(R.string.marquee_press_shortcut_screen_title),
        description = stringResource(R.string.marquee_press_shortcut_screen_description),
        currentShortcut = prefsState.marqueePressShortcut,
        currentAppPackage = prefsState.marqueePressShortcutAppPackage,
        allApps = allApps,
        shortcuts = shortcuts,
        onShortcutSelected = { shortcut ->
            preferencesManager.setMarqueePressShortcut(shortcut)
        },
        onAppSelected = { packageName ->
            preferencesManager.setMarqueePressShortcutAppPackage(packageName)
        },
        onDismiss = onDismiss
    )
}
