package jr.brian.jarnfe.session

import jr.brian.frontend.look.JarnTapControl
import jr.brian.jarnfe.input.JarnScreen

/**
 * A pinned tap left Android's focus on [touched] while the controller stays on [kept]. Keys are
 * already forwarded to the active window, so no focus handoff runs and the touch is not interrupted.
 * [modalReturn] is where control goes when a modal that took it closes. Without [followsModal] a
 * modal opened on [touched] leaves control where it is.
 */
data class JarnTapHold(
    val kept: JarnScreen,
    val touched: JarnScreen,
    val modalReturn: JarnScreen? = null,
    val followsModal: Boolean = true,
)

/** A swap never moves a window, so the Stage window is always the physical top screen. */
private val JarnTapControl.pinnedHost: JarnScreen?
    get() = when (this) {
        JarnTapControl.FollowTap, JarnTapControl.FollowNoTap -> null
        JarnTapControl.AlwaysTop -> JarnScreen.Stage
        JarnTapControl.AlwaysBottom -> JarnScreen.Deck
    }

private val JarnSessionState.canPinControl: Boolean
    get() = deckDisplayAvailable && deckHostReady && !awaitingDeckLaunch

/** Settings controls live on the Deck and system management on Stage, whatever the choice. */
private val JarnSessionState.routeControlScreen: JarnScreen?
    get() = when {
        stageRoute == StageRoute.Settings && settingsOnDeck -> JarnScreen.Deck
        stageRoute == StageRoute.Systems -> JarnScreen.Stage
        else -> null
    }

/**
 * Control goes to, in order: an open modal on the active screen, an open modal on the touched
 * screen, the screen the current route needs, then the pinned screen. When a game covers either
 * window, [bothWindowsVisible] is false and the tap follows the touch as it always has, which is
 * also how a touch takes a yielded window back from a game. Follow, no tap keeps control where it
 * is, or gives it back to [controlBefore] when Android's focus moved it just ahead of the touch.
 */
fun JarnSessionState.onScreenTapped(
    host: JarnScreen,
    choice: JarnTapControl,
    bothWindowsVisible: Boolean,
    controlBefore: JarnScreen? = null,
    hasModal: (JarnScreen) -> Boolean,
): JarnSessionState {
    val touched = roleOn(host)
    val pinnedHost = choice.pinnedHost
    if (choice == JarnTapControl.FollowNoTap && canPinControl && bothWindowsVisible) {
        return keepingControl(touched, controlBefore ?: activeScreen)
    }
    if (pinnedHost == null || !canPinControl || !bothWindowsVisible) {
        return activate(touched).copy(tapHold = null)
    }
    val pinned = roleOn(pinnedHost)
    val modalOnActive = hasModal(activeScreen)
    val modalOnTouched = hasModal(touched)
    val target = when {
        modalOnActive -> activeScreen
        modalOnTouched -> touched
        else -> routeControlScreen ?: pinned
    }
    val modalReturn = when {
        modalOnActive -> tapHold?.modalReturn
        modalOnTouched && touched != pinned -> pinned
        else -> null
    }
    val focusElsewhere = osFocusedScreen != null && osFocusedScreen != target
    val needsHold = modalReturn != null || touched != target || focusElsewhere
    return copy(
        activeScreen = target,
        tapHold = if (needsHold) JarnTapHold(target, touched, modalReturn) else null,
    )
}

private fun JarnSessionState.keepingControl(touched: JarnScreen, kept: JarnScreen): JarnSessionState {
    val focusElsewhere = osFocusedScreen != null && osFocusedScreen != kept
    val needsHold = touched != kept || focusElsewhere
    return copy(
        activeScreen = kept,
        tapHold = if (needsHold) JarnTapHold(kept, touched, followsModal = false) else null,
    )
}

/** How long after Android moves control a touch still counts as its cause. */
private const val TAP_FOCUS_RACE_MS = 400L

/**
 * Android can move focus to a touched window before the touch itself arrives. Remembers who had
 * control just before, so Follow, no tap can hand it back.
 */
class JarnOsFocusPull {
    private var from: JarnScreen? = null
    private var at = 0L

    fun record(before: JarnScreen, after: JarnScreen, nowMs: Long) {
        if (before == after) return
        from = before
        at = nowMs
    }

    fun take(nowMs: Long): JarnScreen? = from.takeIf { nowMs - at <= TAP_FOCUS_RACE_MS }.also { from = null }
}

/** A modal opened by a tap takes control until it closes, then control returns to the pinned screen. */
fun JarnSessionState.onModalChanged(role: JarnScreen, open: Boolean): JarnSessionState {
    val hold = tapHold ?: return this
    return when {
        open && hold.followsModal && hold.touched == role && hold.kept == activeScreen && role != activeScreen -> copy(
            activeScreen = role,
            tapHold = hold.copy(kept = role, modalReturn = activeScreen),
        )
        !open && hold.modalReturn != null && hold.kept == role && activeScreen == role -> copy(
            activeScreen = hold.modalReturn,
            tapHold = JarnTapHold(kept = hold.modalReturn, touched = role),
        )
        else -> this
    }
}

fun JarnSessionState.releasingTapHold(): JarnSessionState = copy(tapHold = null)

/**
 * A window was covered, usually by a game. Control adopts Android's focus, because keeping it
 * elsewhere would make the released hold start a handoff that pulls Jarn over the game.
 */
fun JarnSessionState.onWindowCovered(): JarnSessionState {
    if (tapHold == null) return this
    return copy(activeScreen = osFocusedScreen ?: activeScreen, tapHold = null)
}
