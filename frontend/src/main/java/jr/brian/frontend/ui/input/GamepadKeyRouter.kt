package jr.brian.frontend.ui.input

import android.view.KeyEvent

/**
 * Pure translation layer: raw [KeyEvent] key codes to semantic
 * [GamepadAction]s. Lives apart from the Compose focus layer so
 * tests can exercise it without a running UI, and so a future
 * remapping UI only has to replace this one object.
 *
 * Returns null for keys that aren't bound to an action — the
 * input layer propagates those to the system for normal
 * KeyEvent dispatch (D-pad navigation, text input, etc.).
 */
object GamepadKeyRouter {

    fun actionFor(keyCode: Int): GamepadAction? = when (keyCode) {
        // Confirm — A button, numpad/enter, DPAD_CENTER.
        KeyEvent.KEYCODE_BUTTON_A,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_DPAD_CENTER -> GamepadAction.Confirm

        // Back — B button, Escape, hardware back.
        KeyEvent.KEYCODE_BUTTON_B,
        KeyEvent.KEYCODE_ESCAPE,
        KeyEvent.KEYCODE_BACK -> GamepadAction.Back

        // Search — Y button, forward slash.
        KeyEvent.KEYCODE_BUTTON_Y,
        KeyEvent.KEYCODE_SLASH -> GamepadAction.Search

        // Sort — X button.
        KeyEvent.KEYCODE_BUTTON_X -> GamepadAction.Sort

        // Settings — START, F10.
        KeyEvent.KEYCODE_BUTTON_START,
        KeyEvent.KEYCODE_F10 -> GamepadAction.Settings

        // Page shoulders — L1/R1.
        KeyEvent.KEYCODE_BUTTON_L1 -> GamepadAction.PageLeft
        KeyEvent.KEYCODE_BUTTON_R1 -> GamepadAction.PageRight

        // Jump triggers — L2/R2.
        KeyEvent.KEYCODE_BUTTON_L2 -> GamepadAction.JumpStart
        KeyEvent.KEYCODE_BUTTON_R2 -> GamepadAction.JumpEnd

        else -> null
    }
}
