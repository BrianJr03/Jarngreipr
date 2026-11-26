package jr.brian.jarnfe.library

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.input.LocalJarnInputRouter

internal const val NOW_HIDDEN_UI_TOUCH_TAG = "now_hidden_ui_touch"

/**
 * Whether a Now preview is on screen, and whether the player held Select to watch it with the
 * bottom screen's UI hidden. Both windows read it, so it lives outside either composition. View
 * state only: never saved and never backed up.
 */
internal object NowVideoVisibility {
    var showing by mutableStateOf(false)
        private set
    var uiHidden by mutableStateOf(false)
        private set

    /** A preview that stops, fails, is released or changes game always brings the UI back. */
    fun report(isShowing: Boolean) {
        showing = isShowing
        if (!isShowing) uiHidden = false
    }

    fun toggleUi() {
        uiHidden = showing && !uiHidden
    }

    /** False when the UI was already up, so the press can do its usual job. */
    fun restore(): Boolean {
        if (!uiHidden) return false
        uiHidden = false
        return true
    }

    @get:StringRes
    val holdLabel: Int
        get() = if (uiHidden) R.string.jarnfe_hint_hold_show_ui else R.string.jarnfe_hint_hold_hide_ui
}

/** Called by the preview player with whether its frames are on screen. */
@Composable
internal fun NowVideoVisibilityReport(showing: Boolean) {
    DisposableEffect(showing) {
        NowVideoVisibility.report(showing)
        onDispose { NowVideoVisibility.report(false) }
    }
}

/**
 * Select with Options on a tap. While a preview shows, holding it hides or shows the UI, and a tap
 * with the UI hidden only brings it back. A null [label] binds nothing until a preview shows.
 */
internal fun JarnBindingsBuilder.optionsOrHideUi(@StringRes label: Int?, onOptions: () -> Unit) {
    val visibility = NowVideoVisibility
    if (!visibility.showing) {
        if (label != null) on(JarnAction.Context, label, onPress = onOptions)
        return
    }
    on(
        action = JarnAction.Context,
        label = label,
        onLongPress = visibility::toggleUi,
        longPressLabel = visibility.holdLabel,
    ) { if (!visibility.restore()) onOptions() }
}

internal fun JarnAction.restoresNowUi(): Boolean =
    isDirection || this == JarnAction.Confirm || this == JarnAction.Back

/** While the UI is hidden, the first D-pad, A or B press only brings it back. */
@Composable
internal fun NowHiddenUiInput() {
    val router = LocalJarnInputRouter.current
    val hidden = NowVideoVisibility.uiHidden
    DisposableEffect(router, hidden) {
        if (hidden) router.pressInterceptor = { it.restoresNowUi() && NowVideoVisibility.restore() }
        onDispose { if (hidden) router.pressInterceptor = null }
    }
}

/** While the UI is hidden, a touch anywhere on the screen only brings it back. */
@Composable
internal fun BoxScope.NowHiddenUiTouch() {
    if (!NowVideoVisibility.uiHidden) return
    Box(
        Modifier
            .matchParentSize()
            .testTag(NOW_HIDDEN_UI_TOUCH_TAG)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown().consume()
                    NowVideoVisibility.restore()
                }
            },
    )
}

/** Fades what sits over the preview while the player watches it with the UI hidden. */
@Composable
internal fun Modifier.nowHideableUi(): Modifier {
    val motion = JarnTheme.motion
    val alpha by animateFloatAsState(
        targetValue = if (NowVideoVisibility.uiHidden) 0f else 1f,
        animationSpec = motion.panel.spec(),
        label = "NowHideableUi",
    )
    return graphicsLayer { this.alpha = alpha }
}
