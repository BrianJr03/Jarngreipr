package jr.brian.jarnfe.library

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.focus.JarnFocusStyle
import jr.brian.core.ui.focus.jarnFocusBorderReach
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R

internal const val NOW_SHORTCUTS_TAG = "now_shortcuts"
private const val SHORTCUT_BUTTON_DP = 44f
private const val SHORTCUT_CLEARANCE_DP = 2f
private val ShortcutIconSize = 24.dp

/** What Now's system view offers the Deck header. Null while Now shows anything but a system. */
internal data class NowShortcutStatus(val actions: NowSystemActions, val focus: NowSystemFocus)

internal object NowShortcutBoard {
    var status by mutableStateOf<NowShortcutStatus?>(null)
}

/** Offers Random and Last Played to the header while this system view is on screen. */
@Composable
internal fun PublishNowShortcuts(actions: NowSystemActions, focus: NowSystemFocus) {
    SideEffect {
        NowShortcutBoard.status = NowShortcutStatus(actions, focus).takeIf { actions.buttons.isNotEmpty() }
    }
    DisposableEffect(focus) {
        onDispose { if (NowShortcutBoard.status?.focus === focus) NowShortcutBoard.status = null }
    }
}

/**
 * Sits opposite the Deck tabs, like the Canvas controls. Fades and scales in for a system and out
 * for anything else, and takes no focus while leaving.
 */
@Composable
internal fun NowShortcutControls(modifier: Modifier = Modifier) {
    val status = NowShortcutBoard.status
    var shown by remember { mutableStateOf(status) }
    if (status != null) SideEffect { shown = status }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(status == null) {
        val held = shown?.focus?.focused
        if (status == null && (held == NowFocusTarget.Random || held == NowFocusTarget.LastPlayed)) {
            focusManager.clearFocus(force = true)
        }
    }
    val spec = JarnTheme.motion.small
    AnimatedVisibility(
        visible = status != null,
        modifier = modifier,
        enter = fadeIn(spec.spec()) + scaleIn(spec.spec()),
        exit = fadeOut(spec.spec()) + scaleOut(spec.spec()),
    ) {
        (status ?: shown)?.let { NowShortcutRow(it, canFocus = status != null) }
    }
}

@Composable
private fun NowShortcutRow(status: NowShortcutStatus, canFocus: Boolean) {
    val actions = status.actions
    val focus = status.focus
    Row(
        modifier = Modifier.testTag(NOW_SHORTCUTS_TAG),
        horizontalArrangement = Arrangement.spacedBy(nowShortcutGap(JarnTheme.space.md.value).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.onRandom?.let { random ->
            NowShortcutIcon(R.drawable.ic_jarn_dice, R.string.jarnfe_now_random, random, focus.random, NowFocusTarget.Random, focus, canFocus)
        }
        actions.onLastPlayed?.let { lastPlayed ->
            NowShortcutIcon(
                R.drawable.ic_jarn_replay,
                R.string.jarnfe_now_last_played,
                lastPlayed,
                focus.lastPlayed,
                NowFocusTarget.LastPlayed,
                focus,
                canFocus,
            )
        }
    }
}

@Composable
private fun NowShortcutIcon(
    @DrawableRes iconRes: Int,
    @StringRes labelRes: Int,
    onClick: () -> Unit,
    requester: FocusRequester,
    target: NowFocusTarget,
    focus: NowSystemFocus,
    canFocus: Boolean,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(SHORTCUT_BUTTON_DP.dp)
            .focusProperties { this.canFocus = canFocus }
            .nowActionFocus(requester, target, focus)
            .jarnInteractive(CircleShape, source, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = stringResource(labelRes),
            modifier = Modifier.size(ShortcutIconSize),
            colorFilter = ColorFilter.tint(JarnTheme.colors.textHigh),
        )
    }
}

/** Wide enough that two focused icons' borders, each at full reach, never touch. */
internal fun nowShortcutGap(spacing: Float): Float = maxOf(spacing, 2 * nowShortcutReach() + SHORTCUT_CLEARANCE_DP)

private fun nowShortcutReach(): Float =
    jarnFocusBorderReach(SHORTCUT_BUTTON_DP, SHORTCUT_BUTTON_DP, JarnFocusStyle.Ring).sides
