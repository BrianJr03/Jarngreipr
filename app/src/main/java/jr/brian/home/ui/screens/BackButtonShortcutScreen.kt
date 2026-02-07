package jr.brian.home.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.BackButtonShortcut
import jr.brian.home.model.Shortcut
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager

@Composable
fun BackButtonShortcutScreen(
    allApps: List<AppInfo> = emptyList(),
    onDismiss: () -> Unit = {}
) {
    val powerSettingsManager = LocalPowerSettingsManager.current
    val currentShortcut by powerSettingsManager.backButtonShortcut.collectAsStateWithLifecycle()
    val currentAppPackage by powerSettingsManager.backButtonShortcutAppPackage.collectAsStateWithLifecycle()

    val shortcuts = listOf(
        ShortcutOption(Shortcut.APP, stringResource(R.string.back_button_shortcut_app)),
        ShortcutOption(Shortcut.APP_SEARCH, stringResource(R.string.back_button_shortcut_app_search)),
        ShortcutOption(Shortcut.CONTROL_PAD, stringResource(R.string.back_button_shortcut_control_pad)),
        ShortcutOption(Shortcut.CUSTOM_THEME, stringResource(R.string.back_button_shortcut_custom_theme)),
        ShortcutOption(Shortcut.QUICK_DELETE, stringResource(R.string.back_button_shortcut_quick_delete)),
        ShortcutOption(Shortcut.SETTINGS, stringResource(R.string.back_button_shortcut_settings)),
        ShortcutOption(Shortcut.MONITOR, stringResource(R.string.back_button_shortcut_monitor)),
        ShortcutOption(Shortcut.VOLUME_CONTROLS, stringResource(R.string.back_button_shortcut_volume_controls)),
        ShortcutOption(Shortcut.RECENT_APPS, stringResource(R.string.back_button_shortcut_recent_apps)),
        ShortcutOption(Shortcut.POWERED_OFF, stringResource(R.string.back_button_shortcut_powered_off))
    )

    ShortcutSelectionScreen(
        title = stringResource(R.string.back_button_shortcut_screen_title),
        description = stringResource(R.string.back_button_shortcut_screen_description),
        currentShortcut = currentShortcut.toShortcut(),
        currentAppPackage = currentAppPackage,
        allApps = allApps,
        shortcuts = shortcuts,
        onShortcutSelected = { shortcut ->
            powerSettingsManager.setBackButtonShortcut(shortcut.toBackButtonShortcut())
        },
        onAppSelected = { packageName ->
            powerSettingsManager.setBackButtonShortcutAppPackage(packageName)
        },
        onDismiss = onDismiss
    )
}

/**
 * Extension function to convert BackButtonShortcut to generic Shortcut
 */
fun BackButtonShortcut.toShortcut(): Shortcut = when (this) {
    BackButtonShortcut.NONE -> Shortcut.NONE
    BackButtonShortcut.SETTINGS -> Shortcut.SETTINGS
    BackButtonShortcut.APP_SEARCH -> Shortcut.APP_SEARCH
    BackButtonShortcut.POWERED_OFF -> Shortcut.POWERED_OFF
    BackButtonShortcut.QUICK_DELETE -> Shortcut.QUICK_DELETE
    BackButtonShortcut.CUSTOM_THEME -> Shortcut.CUSTOM_THEME
    BackButtonShortcut.MONITOR -> Shortcut.MONITOR
    BackButtonShortcut.CONTROL_PAD -> Shortcut.CONTROL_PAD
    BackButtonShortcut.VOLUME_CONTROLS -> Shortcut.VOLUME_CONTROLS
    BackButtonShortcut.RECENT_APPS -> Shortcut.RECENT_APPS
    BackButtonShortcut.APP -> Shortcut.APP
}

/**
 * Extension function to convert generic Shortcut to BackButtonShortcut
 */
fun Shortcut.toBackButtonShortcut(): BackButtonShortcut = when (this) {
    Shortcut.NONE -> BackButtonShortcut.NONE
    Shortcut.SETTINGS -> BackButtonShortcut.SETTINGS
    Shortcut.APP_SEARCH -> BackButtonShortcut.APP_SEARCH
    Shortcut.POWERED_OFF -> BackButtonShortcut.POWERED_OFF
    Shortcut.QUICK_DELETE -> BackButtonShortcut.QUICK_DELETE
    Shortcut.CUSTOM_THEME -> BackButtonShortcut.CUSTOM_THEME
    Shortcut.MONITOR -> BackButtonShortcut.MONITOR
    Shortcut.CONTROL_PAD -> BackButtonShortcut.CONTROL_PAD
    Shortcut.VOLUME_CONTROLS -> BackButtonShortcut.VOLUME_CONTROLS
    Shortcut.RECENT_APPS -> BackButtonShortcut.RECENT_APPS
    Shortcut.APP -> BackButtonShortcut.APP
}
