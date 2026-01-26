package jr.brian.home.model

import android.view.KeyEvent

enum class PhysicalButton(val displayName: String, val keyCode: Int) {
    // Navigation
    BACK("Back", KeyEvent.KEYCODE_BACK),
    HOME("Home", KeyEvent.KEYCODE_HOME),
    
    // Controller buttons
    A("A", KeyEvent.KEYCODE_BUTTON_A),
    B("B", KeyEvent.KEYCODE_BUTTON_B),
    X("X", KeyEvent.KEYCODE_BUTTON_X),
    Y("Y", KeyEvent.KEYCODE_BUTTON_Y),
    L1("L1", KeyEvent.KEYCODE_BUTTON_L1),
    L2("L2", KeyEvent.KEYCODE_BUTTON_L2),
    R1("R1", KeyEvent.KEYCODE_BUTTON_R1),
    R2("R2", KeyEvent.KEYCODE_BUTTON_R2),
    L3("L3", KeyEvent.KEYCODE_BUTTON_THUMBL),
    R3("R3", KeyEvent.KEYCODE_BUTTON_THUMBR),
    START("Start", KeyEvent.KEYCODE_BUTTON_START),
    SELECT("Select", KeyEvent.KEYCODE_BUTTON_SELECT)
}

data class ControlPadItem(
    val label: String,
    val mappedButton: PhysicalButton? = null
)
