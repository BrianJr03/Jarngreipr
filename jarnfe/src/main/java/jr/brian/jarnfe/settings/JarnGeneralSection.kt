package jr.brian.jarnfe.settings

import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.jarnfe.R
import jr.brian.jarnfe.audio.JarnAudioSettingsRow
import jr.brian.jarnfe.firstrun.JarnFirstRun
import jr.brian.jarnfe.rss.JarnRssSettingsPanel
import jr.brian.jarnfe.rss.jarnRssDataOrNull
import jr.brian.jarnfe.shell.JarnNotices

@Composable
internal fun JarnGeneralSection(actions: JarnSettingsActions) {
    JarnSwitchToClassicRow()
    JarnAudioSettingsRow(onOpen = { actions.openPanel(JarnSettingsPanel.Audio) })
    ToolsGroup(actions)
    FeedsGroup(actions)
    AboutGroup(actions)
}

@Composable
private fun ToolsGroup(actions: JarnSettingsActions) {
    SettingsSectionTitle(R.string.jarnfe_launcher_tools_title)
    PanelRow(R.string.jarnfe_tools_title, R.string.jarnfe_tools_settings_hint) {
        actions.openPanel(JarnSettingsPanel.Tools)
    }
    PanelRow(R.string.jarnfe_utilities_title, R.string.jarnfe_utilities_settings_hint) {
        actions.openPanel(JarnSettingsPanel.Utilities)
    }
}

@Composable
private fun FeedsGroup(actions: JarnSettingsActions) {
    val context = LocalContext.current
    val rss = remember(context) { context.jarnRssDataOrNull() } ?: return
    SettingsSectionTitle(R.string.jarnfe_launcher_feeds_title)
    JarnRssSettingsPanel(
        data = rss,
        onClearPositions = { actions.openOverlay(JarnSettingsOverlay.ClearRssPositions) },
        firstFocus = actions.overlayReturn,
    )
}

@Composable
private fun AboutGroup(actions: JarnSettingsActions) {
    SettingsSectionTitle(R.string.jarnfe_launcher_about_title)
    PanelRow(R.string.jarnfe_launcher_whats_new, R.string.jarnfe_launcher_whats_new_hint) {
        actions.openPanel(JarnSettingsPanel.WhatsNew)
    }
    PanelRow(R.string.jarnfe_first_run_replay, R.string.jarnfe_first_run_replay_hint) { JarnFirstRun.replay() }
    SupportRow(JarnSupportLink.Discord, R.string.jarnfe_launcher_support_discord)
    SupportRow(JarnSupportLink.Coffee, R.string.jarnfe_launcher_support_coffee)
    SupportRow(JarnSupportLink.Kofi, R.string.jarnfe_launcher_support_kofi)
}

@Composable
private fun PanelRow(@StringRes labelRes: Int, @StringRes hintRes: Int, onClick: () -> Unit) {
    JarnNavigateRow(
        label = stringResource(labelRes),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(hintRes),
    )
}

@Composable
private fun SupportRow(link: JarnSupportLink, @StringRes labelRes: Int) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val settings = remember(context) { context.jarnLauncherSettingsOrNull() } ?: return
    JarnNavigateRow(
        label = stringResource(labelRes),
        onClick = {
            val opened = activity?.let { settings.openSupportLink(it, link) } == true
            if (!opened) JarnNotices.show(R.string.jarnfe_launcher_link_failed)
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
