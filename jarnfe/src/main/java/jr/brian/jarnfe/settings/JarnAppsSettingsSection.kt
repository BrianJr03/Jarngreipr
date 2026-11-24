package jr.brian.jarnfe.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnSliderRow
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.frontend.look.JarnLookStore
import jr.brian.jarnfe.R
import jr.brian.jarnfe.apps.JarnAppOptionsData
import jr.brian.jarnfe.apps.jarnAppsDataOrNull
import jr.brian.jarnfe.look.jarnLookStoreOrNull
import jr.brian.jarnfe.look.rememberJarnDisplay
import jr.brian.jarnfe.session.JarnSession
import kotlin.math.roundToInt

@Composable
internal fun JarnAppsSettingsSection(actions: JarnSettingsActions) {
    val context = LocalContext.current
    val settings = remember(context) { context.jarnLauncherSettingsOrNull() } ?: return
    val apps = remember(context) { context.jarnAppsDataOrNull() }
    val lookStore = remember(context) { context.jarnLookStoreOrNull() }
    val namesVisible = rememberJarnDisplay().appNamesVisible
    val prefs = rememberJarnLauncherPrefs()
    SettingsGroup(R.string.jarnfe_launcher_apps_title) {
        apps?.let { IconPackRow(it, actions) }
        lookStore?.let { ShowAppNamesRow(it, namesVisible) }
        if (namesVisible) AppLabelSizeRow(prefs.appLabelSize, settings::setAppLabelSize)
    }
    if (JarnSession.state.deckDisplayAvailable) DockGroup(settings, prefs, actions)
}

/** The dock only sits under the Deck tabs, so it has nothing to set on a single screen. */
@Composable
private fun DockGroup(settings: JarnLauncherSettings, prefs: JarnLauncherPrefs, actions: JarnSettingsActions) {
    SettingsGroup(R.string.jarnfe_launcher_dock_title) {
        JarnToggleRow(
            label = stringResource(R.string.jarnfe_launcher_dock_show),
            checked = prefs.dockVisible,
            onCheckedChange = settings::setDockVisible,
            modifier = Modifier.fillMaxWidth(),
            supporting = stringResource(R.string.jarnfe_launcher_dock_show_hint),
        )
        if (prefs.dockVisible) DockAppsRow(prefs, actions)
    }
}

@Composable
private fun DockAppsRow(prefs: JarnLauncherPrefs, actions: JarnSettingsActions) {
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_launcher_dock_apps),
        onClick = { actions.openPanel(JarnSettingsPanel.DockApps) },
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_launcher_dock_count, prefs.dockCount, prefs.maxDockApps),
    )
}

@Composable
private fun IconPackRow(data: JarnAppOptionsData, actions: JarnSettingsActions) {
    val selected by data.selectedIconPack.collectAsState(initial = null)
    val packName by produceState<String?>(null, data, selected) {
        value = selected?.let { chosen -> data.iconPacks().firstOrNull { it.packageName == chosen }?.name }
    }
    JarnValueRow(
        label = stringResource(R.string.jarnfe_app_icon_packs_title),
        value = packName ?: stringResource(R.string.jarnfe_app_icon_pack_none),
        onClick = { actions.openOverlay(JarnSettingsOverlay.IconPacks) },
        modifier = Modifier.fillMaxWidth().focusRequester(actions.overlayReturn),
        supporting = stringResource(R.string.jarnfe_launcher_icon_pack_hint),
    )
}

@Composable
private fun ShowAppNamesRow(store: JarnLookStore, visible: Boolean) {
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_launcher_show_app_names),
        checked = visible,
        onCheckedChange = { show -> store.updateDisplay { it.copy(appNamesVisible = show) } },
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_launcher_show_app_names_hint),
    )
}

@Composable
private fun AppLabelSizeRow(size: Int, onChange: (Int) -> Unit) {
    JarnSliderRow(
        label = stringResource(R.string.jarnfe_launcher_label_size),
        value = size.toFloat(),
        valueText = stringResource(R.string.jarnfe_launcher_label_size_value, size),
        onValueChange = { onChange(it.roundToInt()) },
        modifier = Modifier.fillMaxWidth(),
        valueRange = JARN_APP_LABEL_MIN_SIZE.toFloat()..JARN_APP_LABEL_MAX_SIZE.toFloat(),
        steps = JARN_APP_LABEL_MAX_SIZE - JARN_APP_LABEL_MIN_SIZE,
    )
}
