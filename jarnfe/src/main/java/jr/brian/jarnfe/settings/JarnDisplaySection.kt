package jr.brian.jarnfe.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnSliderRow
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.core.ui.theme.JarnSwitchCue
import jr.brian.frontend.look.JarnBrowseView
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.frontend.look.JarnLookStore
import jr.brian.frontend.look.JarnTapControl
import jr.brian.jarnfe.R
import jr.brian.jarnfe.look.jarnLookStoreOrNull
import jr.brian.jarnfe.session.JarnSession
import kotlin.math.roundToInt

private const val PERCENT = 100

@Composable
internal fun JarnDisplaySection() {
    val context = LocalContext.current
    val store = remember(context) { context.jarnLookStoreOrNull() } ?: return
    val display by store.displayState.collectAsState()
    BrowsingGroup(store, display)
    NowPageGroup(store, display)
    TilesGroup(store, display)
    FeedbackGroup(store, display)
    if (JarnSession.state.deckDisplayAvailable) TwoScreensGroup(store, display)
}

@Composable
private fun BrowsingGroup(store: JarnLookStore, display: JarnDisplayPrefs) {
    SettingsGroup(R.string.jarnfe_settings_browsing_title) {
        BrowseViewRow(R.string.jarnfe_settings_system_view, R.string.jarnfe_settings_system_view_hint, display.systemView) {
            store.updateDisplay { it.copy(systemView = it.systemView.next()) }
        }
        BrowseViewRow(R.string.jarnfe_settings_game_view, R.string.jarnfe_settings_game_view_hint, display.gameView) {
            store.updateDisplay { it.copy(gameView = it.gameView.next()) }
        }
        SystemNamesRow(store, display.systemNamesVisible)
    }
}

/** What a system's Now page shows. Each part is on by default. */
@Composable
private fun NowPageGroup(store: JarnLookStore, display: JarnDisplayPrefs) {
    SettingsGroup(R.string.jarnfe_settings_now_page_title) {
        NowPartRow(R.string.jarnfe_settings_show_random, R.string.jarnfe_settings_show_random_hint, display.randomVisible) { show ->
            store.updateDisplay { it.copy(randomVisible = show) }
        }
        NowPartRow(
            R.string.jarnfe_settings_show_last_played,
            R.string.jarnfe_settings_show_last_played_hint,
            display.lastPlayedVisible,
        ) { show -> store.updateDisplay { it.copy(lastPlayedVisible = show) } }
        NowPartRow(R.string.jarnfe_settings_show_system_logo, R.string.jarnfe_settings_show_system_logo_hint, display.systemLogoVisible) { show ->
            store.updateDisplay { it.copy(systemLogoVisible = show) }
        }
        NowPartRow(R.string.jarnfe_settings_show_game_count, R.string.jarnfe_settings_show_game_count_hint, display.gameCountVisible) { show ->
            store.updateDisplay { it.copy(gameCountVisible = show) }
        }
        NowPartRow(
            R.string.jarnfe_settings_show_last_played_text,
            R.string.jarnfe_settings_show_last_played_text_hint,
            display.lastPlayedTextVisible,
        ) { show -> store.updateDisplay { it.copy(lastPlayedTextVisible = show) } }
        NowPartRow(R.string.jarnfe_settings_show_game_row, R.string.jarnfe_settings_show_game_row_hint, display.gameRowVisible) { show ->
            store.updateDisplay { it.copy(gameRowVisible = show) }
        }
    }
}

@Composable
private fun NowPartRow(
    @StringRes labelRes: Int,
    @StringRes hintRes: Int,
    visible: Boolean,
    onChange: (Boolean) -> Unit,
) {
    JarnToggleRow(
        label = stringResource(labelRes),
        checked = visible,
        onCheckedChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(hintRes),
    )
}

@Composable
private fun SystemNamesRow(store: JarnLookStore, visible: Boolean) {
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_settings_system_names),
        checked = visible,
        onCheckedChange = { show -> store.updateDisplay { it.copy(systemNamesVisible = show) } },
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_settings_system_names_hint),
    )
}

@Composable
private fun BrowseViewRow(
    @StringRes labelRes: Int,
    @StringRes hintRes: Int,
    view: JarnBrowseView,
    onClick: () -> Unit,
) {
    JarnValueRow(
        label = stringResource(labelRes),
        value = stringResource(view.labelRes),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(hintRes),
    )
}

@get:StringRes
private val JarnBrowseView.labelRes: Int
    get() = when (this) {
        JarnBrowseView.Grid -> R.string.jarnfe_browse_view_grid
        JarnBrowseView.Carousel -> R.string.jarnfe_browse_view_carousel
    }

private fun JarnBrowseView.next(): JarnBrowseView =
    JarnBrowseView.entries[(ordinal + 1) % JarnBrowseView.entries.size]

/** A carousel ignores tile size, so its row hides, and the group goes when neither view uses it. */
@Composable
private fun TilesGroup(store: JarnLookStore, display: JarnDisplayPrefs) {
    val systemsGrid = display.systemView == JarnBrowseView.Grid
    val gamesGrid = display.gameView == JarnBrowseView.Grid
    if (!systemsGrid && !gamesGrid) return
    SettingsGroup(R.string.jarnfe_settings_tiles_title) {
        if (systemsGrid) {
            TileScaleRow(R.string.jarnfe_settings_system_tile_size, display.systemTileScale) { scale ->
                store.updateDisplay { it.copy(systemTileScale = scale) }
            }
        }
        if (gamesGrid) {
            TileScaleRow(R.string.jarnfe_settings_game_tile_size, display.gameTileScale) { scale ->
                store.updateDisplay { it.copy(gameTileScale = scale) }
            }
        }
    }
}

@Composable
private fun FeedbackGroup(store: JarnLookStore, display: JarnDisplayPrefs) {
    SettingsGroup(R.string.jarnfe_settings_feedback_title) {
        JarnToggleRow(
            label = stringResource(R.string.jarnfe_settings_show_hints),
            checked = display.hintsVisible,
            onCheckedChange = { visible -> store.updateDisplay { it.copy(hintsVisible = visible) } },
            modifier = Modifier.fillMaxWidth(),
            supporting = stringResource(R.string.jarnfe_settings_show_hints_hint),
        )
        UndoCountdownRow(store, display.undoCountdownVisible)
        JarnToggleRow(
            label = stringResource(R.string.jarnfe_settings_haptics),
            checked = display.buttonHaptics,
            onCheckedChange = { enabled -> store.updateDisplay { it.copy(buttonHaptics = enabled) } },
            modifier = Modifier.fillMaxWidth(),
            supporting = stringResource(R.string.jarnfe_settings_haptics_hint),
        )
        PageTransitionRow(store, display.pageTransition)
        ReducedMotionRow(store)
    }
}

@Composable
private fun TwoScreensGroup(store: JarnLookStore, display: JarnDisplayPrefs) {
    SettingsGroup(R.string.jarnfe_settings_two_screens_title) {
        SwitchCueRow(store, display.switchCue)
        TapControlRow(store, display.tapControl)
    }
}

@Composable
private fun ReducedMotionRow(store: JarnLookStore) {
    val reducedMotion by store.reducedMotionState.collectAsState()
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_look_reduced_motion),
        checked = reducedMotion,
        onCheckedChange = store::setReducedMotion,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_look_reduced_motion_hint),
    )
}

@Composable
private fun UndoCountdownRow(store: JarnLookStore, visible: Boolean) {
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_settings_undo_countdown),
        checked = visible,
        onCheckedChange = { show -> store.updateDisplay { it.copy(undoCountdownVisible = show) } },
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_settings_undo_countdown_hint),
    )
}

@Composable
private fun SwitchCueRow(store: JarnLookStore, cue: JarnSwitchCue) {
    JarnValueRow(
        label = stringResource(R.string.jarnfe_settings_switch_cue),
        value = stringResource(cue.cueLabelRes),
        onClick = { store.updateDisplay { it.copy(switchCue = cue.nextCue()) } },
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(cue.cueHintRes),
    )
}

@get:StringRes
internal val JarnSwitchCue.cueLabelRes: Int
    get() = when (this) {
        JarnSwitchCue.Pulse -> R.string.jarnfe_switch_cue_pulse
        JarnSwitchCue.Dim -> R.string.jarnfe_switch_cue_dim
        JarnSwitchCue.TabMark -> R.string.jarnfe_switch_cue_tab_mark
        JarnSwitchCue.JarnLabel -> R.string.jarnfe_switch_cue_jarn_label
    }

@get:StringRes
internal val JarnSwitchCue.cueHintRes: Int
    get() = when (this) {
        JarnSwitchCue.Pulse -> R.string.jarnfe_switch_cue_pulse_hint
        JarnSwitchCue.Dim -> R.string.jarnfe_switch_cue_dim_hint
        JarnSwitchCue.TabMark -> R.string.jarnfe_switch_cue_tab_mark_hint
        JarnSwitchCue.JarnLabel -> R.string.jarnfe_switch_cue_jarn_label_hint
    }

private fun JarnSwitchCue.nextCue(): JarnSwitchCue =
    JarnSwitchCue.entries[(ordinal + 1) % JarnSwitchCue.entries.size]

@Composable
private fun TapControlRow(store: JarnLookStore, choice: JarnTapControl) {
    JarnValueRow(
        label = stringResource(R.string.jarnfe_settings_tap_control),
        value = stringResource(choice.labelRes),
        onClick = { store.updateDisplay { it.copy(tapControl = choice.next()) } },
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_settings_tap_control_hint),
    )
}

@get:StringRes
private val JarnTapControl.labelRes: Int
    get() = when (this) {
        JarnTapControl.FollowTap -> R.string.jarnfe_settings_tap_control_follow
        JarnTapControl.AlwaysTop -> R.string.jarnfe_settings_tap_control_top
        JarnTapControl.AlwaysBottom -> R.string.jarnfe_settings_tap_control_bottom
    }

private fun JarnTapControl.next(): JarnTapControl =
    JarnTapControl.entries[(ordinal + 1) % JarnTapControl.entries.size]

@Composable
private fun TileScaleRow(@StringRes labelRes: Int, scale: Float, onChange: (Float) -> Unit) {
    JarnSliderRow(
        label = stringResource(labelRes),
        value = scale,
        valueText = stringResource(R.string.jarnfe_settings_percent, (scale * PERCENT).roundToInt()),
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        valueRange = JarnDisplayPrefs.MIN_TILE_SCALE..JarnDisplayPrefs.MAX_TILE_SCALE,
        steps = JarnDisplayPrefs.TILE_SCALE_STEPS,
    )
}
