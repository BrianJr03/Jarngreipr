package jr.brian.jarnfe.settings

import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.jarnfe.R
import jr.brian.jarnfe.audio.JarnAudioSettingsRow
import jr.brian.jarnfe.canvas.jarnCanvasDataOrNull
import jr.brian.jarnfe.canvas.tour.JarnCanvasTour
import jr.brian.jarnfe.firstrun.JarnFirstRun
import jr.brian.jarnfe.rss.JarnRssSettingsPanel
import jr.brian.jarnfe.rss.jarnRssDataOrNull
import jr.brian.jarnfe.shell.JarnNotices
import jr.brian.jarnfe.shell.jarnExperienceSwitchOrNull

@Composable
internal fun JarnGeneralSection(actions: JarnSettingsActions) {
    ExperienceGroup()
    SoundAndToolsGroup(actions)
    FeedsGroup(actions)
    ToursGroup()
    AboutGroup(actions)
}

@Composable
private fun ExperienceGroup() {
    val context = LocalContext.current
    if (remember(context) { context.jarnExperienceSwitchOrNull() } == null) return
    SettingsGroup(R.string.jarnfe_launcher_experience_title) { JarnSwitchToClassicRow() }
}

@Composable
private fun SoundAndToolsGroup(actions: JarnSettingsActions) {
    SettingsGroup(R.string.jarnfe_launcher_sound_tools_title) {
        JarnAudioSettingsRow(onOpen = { actions.openPanel(JarnSettingsPanel.Audio) })
        PanelRow(R.string.jarnfe_tools_title, R.string.jarnfe_tools_settings_hint) {
            actions.openPanel(JarnSettingsPanel.Tools)
        }
        PanelRow(R.string.jarnfe_utilities_title, R.string.jarnfe_utilities_settings_hint) {
            actions.openPanel(JarnSettingsPanel.Utilities)
        }
    }
}

@Composable
private fun FeedsGroup(actions: JarnSettingsActions) {
    val context = LocalContext.current
    val rss = remember(context) { context.jarnRssDataOrNull() } ?: return
    SettingsGroup(R.string.jarnfe_launcher_feeds_title) {
        JarnRssSettingsPanel(
            data = rss,
            onClearPositions = { actions.openOverlay(JarnSettingsOverlay.ClearRssPositions) },
            clearFocus = actions.overlayReturn,
        )
    }
}

@Composable
private fun ToursGroup() {
    SettingsGroup(R.string.jarnfe_launcher_tours_title) {
        PanelRow(R.string.jarnfe_first_run_replay, R.string.jarnfe_first_run_replay_hint) { JarnFirstRun.replay() }
        CanvasTourReplayRow()
        MeetJarnRow()
    }
}

@Composable
private fun MeetJarnRow() {
    val context = LocalContext.current
    val meetJarn = remember(context) { context.jarnMeetJarnOrNull() } ?: return
    var showing by remember { mutableStateOf(false) }
    PanelRow(R.string.jarnfe_meet_jarn, R.string.jarnfe_meet_jarn_hint) { showing = true }
    if (showing) meetJarn.Replay(onClose = { showing = false })
}

@Composable
private fun AboutGroup(actions: JarnSettingsActions) {
    SettingsGroup(R.string.jarnfe_launcher_about_title) {
        PanelRow(R.string.jarnfe_launcher_whats_new, R.string.jarnfe_launcher_whats_new_hint) {
            actions.openPanel(JarnSettingsPanel.WhatsNew)
        }
        SupportRow(JarnSupportLink.Discord, R.string.jarnfe_launcher_support_discord)
        SupportRow(JarnSupportLink.Coffee, R.string.jarnfe_launcher_support_coffee)
        SupportRow(JarnSupportLink.Kofi, R.string.jarnfe_launcher_support_kofi)
    }
}

@Composable
private fun CanvasTourReplayRow() {
    val context = LocalContext.current
    if (remember(context) { context.jarnCanvasDataOrNull() } == null) return
    PanelRow(R.string.jarnfe_canvas_tour_replay, R.string.jarnfe_canvas_tour_replay_hint) {
        JarnCanvasTour.replay(context)
    }
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
