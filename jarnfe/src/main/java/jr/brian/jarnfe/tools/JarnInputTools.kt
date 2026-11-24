package jr.brian.jarnfe.tools

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import jr.brian.jarnfe.R
import kotlinx.coroutines.flow.Flow

/** Names match the values already stored and backed up, so the setting is shared with Classic. */
enum class JarnBackShortcut(@param:StringRes internal val labelRes: Int) {
    NONE(R.string.jarnfe_tools_shortcut_none),
    APP(R.string.jarnfe_tools_shortcut_app),
    APP_SEARCH(R.string.jarnfe_tools_shortcut_app_search),
    ROM_SEARCH(R.string.jarnfe_tools_shortcut_rom_search),
    CONTROL_PAD(R.string.jarnfe_tools_shortcut_control_pad),
    CUSTOM_THEME(R.string.jarnfe_tools_shortcut_custom_theme),
    QUICK_DELETE(R.string.jarnfe_tools_shortcut_quick_delete),
    SETTINGS(R.string.jarnfe_tools_shortcut_settings),
    MONITOR(R.string.jarnfe_tools_shortcut_monitor),
    VOLUME_CONTROLS(R.string.jarnfe_tools_shortcut_volume),
    RECENT_APPS(R.string.jarnfe_tools_shortcut_recents),
    POWERED_OFF(R.string.jarnfe_tools_shortcut_powered_off),
}

enum class JarnBackSlot { Primary, Secondary }

/** Only the second slot can be empty, as in Classic. */
internal fun jarnBackShortcutOptions(slot: JarnBackSlot): List<JarnBackShortcut> =
    if (slot == JarnBackSlot.Secondary) {
        JarnBackShortcut.entries
    } else {
        JarnBackShortcut.entries - JarnBackShortcut.NONE
    }

@Immutable
data class JarnBackShortcuts(
    val enabled: Boolean = false,
    val primary: JarnBackShortcut = JarnBackShortcut.NONE,
    val primaryApp: String? = null,
    val secondary: JarnBackShortcut = JarnBackShortcut.NONE,
    val secondaryApp: String? = null,
) {
    fun shortcut(slot: JarnBackSlot) = if (slot == JarnBackSlot.Primary) primary else secondary

    fun app(slot: JarnBackSlot) = if (slot == JarnBackSlot.Primary) primaryApp else secondaryApp
}

@Immutable
data class JarnPointerStatus(
    val available: Boolean = false,
    val permissionGranted: Boolean = false,
    val connected: Boolean = false,
) {
    val ready: Boolean get() = available && permissionGranted && connected
}

/** The size in pixels of the display that injected pointer events land on. */
@Immutable
data class JarnPointerBounds(val width: Float, val height: Float)

sealed interface JarnPointerCommand {
    data class Move(val x: Float, val y: Float) : JarnPointerCommand
    data class Click(val x: Float, val y: Float, val secondary: Boolean) : JarnPointerCommand
    data class Scroll(val x: Float, val y: Float, val amount: Float) : JarnPointerCommand
}

/** Host services behind the input tools. Pointer sends may block, so call them off the main thread. */
interface JarnInputTools {
    val pointerStatus: Flow<JarnPointerStatus>
    val backShortcuts: Flow<JarnBackShortcuts>

    fun connectPointer()

    fun requestPointerPermission()

    fun pointerBounds(): JarnPointerBounds

    fun sendPointer(command: JarnPointerCommand)

    /** Turning shortcuts off also clears both slots, as in Classic. */
    fun setBackShortcutsEnabled(enabled: Boolean)

    fun setBackShortcut(slot: JarnBackSlot, shortcut: JarnBackShortcut)

    fun setBackShortcutApp(slot: JarnBackSlot, packageName: String)
}

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnInputToolsProvider {
    val jarnInputTools: JarnInputTools
}

internal fun Context.jarnInputToolsOrNull(): JarnInputTools? =
    (applicationContext as? JarnInputToolsProvider)?.jarnInputTools
