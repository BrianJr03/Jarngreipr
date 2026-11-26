package jr.brian.jarnfe.input

import androidx.annotation.StringRes

enum class JarnScreen { Stage, Deck }

/**
 * How a layer treats actions it does not bind.
 *
 * - [Passive]: they continue to the layer below.
 * - [Modal]: lower layers are blocked, but D-pad and A still reach the focused composable.
 * - [Capture]: every mapped button belongs to this layer, for modes like Canvas grab.
 *
 * Swap and the L3/R3 mutes are reserved app-wide (see [JarnAction.isAppWide]), so they pass
 * through Modal and Capture unless the layer binds them.
 */
enum class JarnLayerMode { Passive, Modal, Capture }

/** A declared action. A null [onPress] declares a hint for input the focused composable handles. */
class JarnBinding internal constructor(
    @param:StringRes val labelRes: Int?,
    val repeats: Boolean,
    val onLongPress: (() -> Unit)?,
    val onPress: (() -> Unit)?,
    val onRepeat: (() -> Unit)? = null,
    @param:StringRes val longPressLabelRes: Int? = null,
)

class JarnBindings internal constructor(internal val entries: Map<JarnAction, JarnBinding>) {
    internal val labels: Map<JarnAction, Int?> = entries.mapValues { it.value.labelRes }
    internal val longPressLabels: Map<JarnAction, Int> =
        entries.mapNotNull { (action, binding) -> binding.longPressLabelRes?.let { action to it } }.toMap()

    companion object {
        val Empty = JarnBindings(emptyMap())
    }
}

class JarnBindingsBuilder internal constructor() {
    private val entries = LinkedHashMap<JarnAction, JarnBinding>()

    /**
     * Binds [action]. A null [label] handles the action without showing a hint. [onRepeat] replaces
     * [onPress] for key repeats, for actions where only a fresh press may do something.
     * [longPressLabel] adds a second hint for the same button, for what holding it does.
     */
    fun on(
        action: JarnAction,
        @StringRes label: Int? = null,
        repeats: Boolean = action.isDirection,
        onLongPress: (() -> Unit)? = null,
        onRepeat: (() -> Unit)? = null,
        @StringRes longPressLabel: Int? = null,
        onPress: () -> Unit,
    ) {
        require(!(repeats && onLongPress != null)) { "$action cannot both repeat and long press" }
        require(longPressLabel == null || onLongPress != null) { "$action has a hold hint but no long press" }
        require(!(repeats && action == JarnAction.Back)) { "Back pops one layer per press" }
        entries[action] = JarnBinding(label, repeats, onLongPress, onPress, onRepeat, longPressLabel)
    }

    /** Shows a hint for [action] while leaving the press to the focused composable. */
    fun hint(action: JarnAction, @StringRes label: Int) {
        require(action.reachesFocus) { "$action never reaches the focused composable" }
        entries[action] = JarnBinding(label, repeats = false, onLongPress = null, onPress = null)
    }

    internal fun build() = JarnBindings(entries.toMap())
}

fun jarnBindings(declare: JarnBindingsBuilder.() -> Unit): JarnBindings =
    JarnBindingsBuilder().apply(declare).build()
