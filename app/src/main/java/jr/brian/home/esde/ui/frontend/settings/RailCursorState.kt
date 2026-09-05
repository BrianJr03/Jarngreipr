package jr.brian.home.esde.ui.frontend.settings

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Shared cursor state for the rail + row-list overlays. Category-agnostic —
 * callers pass their own category enum's [entries] list on each key event so
 * the state class stays free of a type parameter.
 */
@Stable
class RailCursorState<C : Enum<C>> internal constructor(
    private val entries: List<C>,
    initial: C
) {
    var focusOnRail: Boolean by mutableStateOf(true)
        private set
    var selectedCategory: C by mutableStateOf(initial)
        private set
    var focusedRow: Int by mutableIntStateOf(0)
        private set
    var activationTick: Int by mutableIntStateOf(0)
        private set
    var horizontalStep: Int by mutableIntStateOf(0)
        private set

    private var horizontalClaimCount: Int by mutableIntStateOf(0)

    fun registerHorizontal(claims: Boolean) {
        horizontalClaimCount = (horizontalClaimCount + if (claims) 1 else -1).coerceAtLeast(0)
    }

    private fun focusedRowClaimsHorizontal(): Boolean =
        !focusOnRail && horizontalClaimCount > 0

    fun handleKey(keyCode: Int, rowCount: Int, onClose: () -> Unit): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (focusOnRail) {
                    val current = entries.indexOf(selectedCategory)
                    val prev = (current - 1 + entries.size) % entries.size
                    selectedCategory = entries[prev]
                    focusedRow = 0
                } else {
                    focusedRow = (focusedRow - 1).coerceAtLeast(0)
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (focusOnRail) {
                    val current = entries.indexOf(selectedCategory)
                    val next = (current + 1) % entries.size
                    selectedCategory = entries[next]
                    focusedRow = 0
                } else {
                    val maxIndex = (rowCount - 1).coerceAtLeast(0)
                    focusedRow = (focusedRow + 1).coerceAtMost(maxIndex)
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (focusOnRail) {
                    if (rowCount > 0) {
                        focusOnRail = false
                        focusedRow = 0
                    }
                } else if (focusedRowClaimsHorizontal()) {
                    horizontalStep += 1
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (focusOnRail) {
                    // consume, do nothing
                } else if (focusedRowClaimsHorizontal()) {
                    horizontalStep -= 1
                } else {
                    focusOnRail = true
                    focusedRow = 0
                }
                true
            }
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                if (focusOnRail) {
                    if (rowCount > 0) {
                        focusOnRail = false
                        focusedRow = 0
                    }
                } else {
                    activationTick += 1
                }
                true
            }
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BACK -> {
                if (focusOnRail) {
                    onClose()
                } else {
                    focusOnRail = true
                    focusedRow = 0
                }
                true
            }
            else -> false
        }
    }
}

@Composable
fun <C : Enum<C>> rememberRailCursorState(
    entries: List<C>,
    initial: C = entries.first()
): RailCursorState<C> = remember(entries) { RailCursorState(entries, initial) }
