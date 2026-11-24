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
 * opposite-screen tab actions are also unhinted, to keep the hint bar uncrowded.
 */
enum class JarnAction(
    val hintKey: JarnKey?,
    internal val firesOnPress: Boolean = false,
    internal val reachesFocus: Boolean = false,
) {
    Confirm(JarnKey.A, reachesFocus = true),
    Back(JarnKey.B),
    Context(JarnKey.X),
    Search(JarnKey.Y),
    TabPrevious(JarnKey.L1, firesOnPress = true),
    TabNext(JarnKey.R1, firesOnPress = true),
    OppositeTabPrevious(hintKey = null, firesOnPress = true),
    OppositeTabNext(hintKey = null, firesOnPress = true),
    Menu(JarnKey.Start),
    SwitchScreen(JarnKey.Select),
    Swap(hintKey = null, firesOnPress = true),
    MoveUp(JarnKey.Dpad, firesOnPress = true, reachesFocus = true),
    MoveDown(JarnKey.Dpad, firesOnPress = true, reachesFocus = true),
    MoveLeft(JarnKey.Dpad, firesOnPress = true, reachesFocus = true),
    MoveRight(JarnKey.Dpad, firesOnPress = true, reachesFocus = true);

    val isDirection: Boolean
        get() = hintKey == JarnKey.Dpad
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

        KeyEvent.KEYCODE_BUTTON_X -> JarnAction.Context
        KeyEvent.KEYCODE_BUTTON_Y -> JarnAction.Search
        KeyEvent.KEYCODE_BUTTON_START -> JarnAction.Menu
        KeyEvent.KEYCODE_BUTTON_SELECT -> JarnAction.SwitchScreen
        KeyEvent.KEYCODE_BUTTON_L1 -> JarnAction.TabPrevious
        KeyEvent.KEYCODE_BUTTON_R1 -> JarnAction.TabNext
        KeyEvent.KEYCODE_BUTTON_L2 -> JarnAction.OppositeTabPrevious
        KeyEvent.KEYCODE_BUTTON_R2 -> JarnAction.OppositeTabNext

        KeyEvent.KEYCODE_BUTTON_THUMBL,
        KeyEvent.KEYCODE_BUTTON_THUMBR -> JarnAction.Swap

        KeyEvent.KEYCODE_DPAD_UP -> JarnAction.MoveUp
        KeyEvent.KEYCODE_DPAD_DOWN -> JarnAction.MoveDown
        KeyEvent.KEYCODE_DPAD_LEFT -> JarnAction.MoveLeft
        KeyEvent.KEYCODE_DPAD_RIGHT -> JarnAction.MoveRight
        else -> null
    }
}
