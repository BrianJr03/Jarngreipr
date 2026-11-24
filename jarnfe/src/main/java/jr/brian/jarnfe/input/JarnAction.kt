package jr.brian.jarnfe.input

import android.view.KeyEvent
import jr.brian.core.ui.components.JarnKey

/**
 * Every action a controller can perform in Jarn FE.
 *
 * [firesOnPress] actions act on key-down. All others act on key-up, so the release can never land
 * on whatever the action just opened. [reachesFocus] actions fall through to the focused composable
 * when no layer binds them, which keeps D-pad focus movement and A-to-click working.
 *
 * [hintKey] is null for Swap until Swap ships, because it spans both the L3 and R3 key caps. The
 * L2/R2 actions are also unhinted, since their tab hint comes from [JarnInputRouter.tabHintFor].
 *
 * L1/R1 and L2/R2 are tied to physical screens, not to the controller: in the shell L1/R1 step the
 * top screen's tabs and L2/R2 the bottom screen's. A layer can still claim either pair for itself.
 *
 * X switches screens and holding it swaps them (see [heldAction]). Select is the options button.
 */
enum class JarnAction(
    val hintKey: JarnKey?,
    internal val firesOnPress: Boolean = false,
    internal val reachesFocus: Boolean = false,
) {
    Confirm(JarnKey.A, reachesFocus = true),
    Back(JarnKey.B),
    Context(JarnKey.Select),
    Search(JarnKey.Y),
    TabPrevious(JarnKey.L1, firesOnPress = true),
    TabNext(JarnKey.R1, firesOnPress = true),
    BottomTabPrevious(hintKey = null, firesOnPress = true),
    BottomTabNext(hintKey = null, firesOnPress = true),
    Menu(JarnKey.Start),
    SwitchScreen(JarnKey.X),
    Swap(hintKey = null, firesOnPress = true),
    MoveUp(JarnKey.Dpad, firesOnPress = true, reachesFocus = true),
    MoveDown(JarnKey.Dpad, firesOnPress = true, reachesFocus = true),
    MoveLeft(JarnKey.Dpad, firesOnPress = true, reachesFocus = true),
    MoveRight(JarnKey.Dpad, firesOnPress = true, reachesFocus = true);

    val isDirection: Boolean
        get() = hintKey == JarnKey.Dpad

    /**
     * What holding this action's button does in every layer that doesn't give it a long press of its
     * own. Holding X swaps the screens, so a tap switches on release and a hold never also switches.
     */
    internal val heldAction: JarnAction?
        get() = if (this == SwitchScreen) Swap else null
}

/** A shoulder pair and the physical screen whose tabs it steps in the shell. */
enum class JarnShoulderPair(val previous: JarnAction, val next: JarnAction, val key: JarnKey) {
    Top(JarnAction.TabPrevious, JarnAction.TabNext, JarnKey.L1R1),
    Bottom(JarnAction.BottomTabPrevious, JarnAction.BottomTabNext, JarnKey.L2R2),
}

/** Binds both actions of each pair in [pairs] to [step], with -1 for previous and 1 for next. */
fun JarnBindingsBuilder.tabBindings(pairs: List<JarnShoulderPair>, step: (Int) -> Unit) {
    pairs.forEach { pair ->
        on(pair.previous) { step(-1) }
        on(pair.next) { step(1) }
    }
}

/** The fixed button map. The Home key is deliberately absent, so it always reaches the system. */
object JarnButtonMap {
    fun actionFor(keyCode: Int): JarnAction? = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A,
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER -> JarnAction.Confirm

        KeyEvent.KEYCODE_BUTTON_B,
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_ESCAPE -> JarnAction.Back

        KeyEvent.KEYCODE_BUTTON_X -> JarnAction.SwitchScreen
        KeyEvent.KEYCODE_BUTTON_Y -> JarnAction.Search
        KeyEvent.KEYCODE_BUTTON_START -> JarnAction.Menu
        KeyEvent.KEYCODE_BUTTON_SELECT -> JarnAction.Context
        KeyEvent.KEYCODE_BUTTON_L1 -> JarnAction.TabPrevious
        KeyEvent.KEYCODE_BUTTON_R1 -> JarnAction.TabNext
        KeyEvent.KEYCODE_BUTTON_L2 -> JarnAction.BottomTabPrevious
        KeyEvent.KEYCODE_BUTTON_R2 -> JarnAction.BottomTabNext

        KeyEvent.KEYCODE_BUTTON_THUMBL,
        KeyEvent.KEYCODE_BUTTON_THUMBR -> JarnAction.Swap

        KeyEvent.KEYCODE_DPAD_UP -> JarnAction.MoveUp
        KeyEvent.KEYCODE_DPAD_DOWN -> JarnAction.MoveDown
        KeyEvent.KEYCODE_DPAD_LEFT -> JarnAction.MoveLeft
        KeyEvent.KEYCODE_DPAD_RIGHT -> JarnAction.MoveRight
        else -> null
    }
}
