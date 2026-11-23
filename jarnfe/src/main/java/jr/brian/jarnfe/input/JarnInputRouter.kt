package jr.brian.jarnfe.input

import android.view.KeyEvent
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import jr.brian.core.ui.components.JarnKey
import jr.brian.jarnfe.R

const val JARN_LONG_PRESS_MS = 500L

enum class JarnKeyPhase { Down, Up }

/** A raw key event, separated from [KeyEvent] so routing stays testable on the JVM. */
data class JarnKeyInput(
    val keyCode: Int,
    val phase: JarnKeyPhase,
    val eventTimeMs: Long,
    val repeatCount: Int = 0,
    val isCanceled: Boolean = false,
    val isFallback: Boolean = false,
)

@Immutable
data class JarnInputHint(val key: JarnKey, @param:StringRes val labelRes: Int)

class JarnInputLayerHandle internal constructor(
    internal val screen: JarnScreen?,
    internal val mode: JarnLayerMode,
    internal val depth: Int,
) {
    internal var entries: Map<JarnAction, JarnBinding> = emptyMap()
    internal var labels by mutableStateOf<Map<JarnAction, Int?>>(emptyMap())
    internal var sequence = 0L
    internal var attached = false

    fun update(bindings: JarnBindings) {
        entries = bindings.entries
        labels = bindings.labels
    }
}

/**
 * The single input model for both screens. Share one instance across the Stage and Deck windows
 * and call [dispatch] from each activity's `dispatchKeyEvent`, returning its result.
 *
 * Each physical press is resolved once, on key-down, and that decision also governs its repeats and
 * its key-up. That is what prevents one layer acting on down and another on up.
 */
class JarnInputRouter(private val longPressMs: Long = JARN_LONG_PRESS_MS) {

    var activeScreen by mutableStateOf(JarnScreen.Stage)
        private set

    private val layers = mutableStateListOf<JarnInputLayerHandle>()
    private val globalLayer = JarnInputLayerHandle(screen = null, JarnLayerMode.Passive, depth = -1)
        .apply { attached = true }
    private val presses = HashMap<Int, Press>()
    private var nextSequence = 0L

    /** Runs once per physical press of a button the active screen's hint bar shows. */
    var onHintPressed: (() -> Unit)? = null

    fun activate(screen: JarnScreen) {
        activeScreen = screen
    }

    /** Lowest-priority bindings shared by both screens, such as Menu, Search and Swap. */
    fun setGlobalBindings(bindings: JarnBindings) = globalLayer.update(bindings)

    fun createLayer(
        screen: JarnScreen,
        mode: JarnLayerMode = JarnLayerMode.Passive,
        depth: Int = 0,
    ) = JarnInputLayerHandle(screen, mode, depth)

    fun attach(layer: JarnInputLayerHandle) {
        if (layer.attached) return
        layer.attached = true
        layer.sequence = nextSequence++
        val index = layers.indexOfFirst {
            it.depth > layer.depth || (it.depth == layer.depth && it.sequence > layer.sequence)
        }
        if (index < 0) layers.add(layer) else layers.add(index, layer)
    }

    fun detach(layer: JarnInputLayerHandle) {
        layer.attached = false
        layers.remove(layer)
    }

    fun dispatch(event: KeyEvent): Boolean {
        val phase = when (event.action) {
            KeyEvent.ACTION_DOWN -> JarnKeyPhase.Down
            KeyEvent.ACTION_UP -> JarnKeyPhase.Up
            else -> return false
        }
        return dispatch(
            JarnKeyInput(
                keyCode = event.keyCode,
                phase = phase,
                eventTimeMs = event.eventTime,
                repeatCount = event.repeatCount,
                isCanceled = event.isCanceled,
                isFallback = event.flags and KeyEvent.FLAG_FALLBACK != 0,
            ),
        )
    }

    /** Returns true when the event is consumed and must not reach the focused composable. */
    fun dispatch(input: JarnKeyInput): Boolean {
        val action = JarnButtonMap.actionFor(input.keyCode) ?: return false
        // A's system fallback only exists because A was passed through, so it must not act again.
        if (input.isFallback && action == JarnAction.Confirm) return false
        return when {
            input.phase == JarnKeyPhase.Up -> onKeyUp(input)
            input.repeatCount > 0 -> onKeyRepeat(input)
            else -> onKeyDown(input, action)
        }
    }

    /** For predictive back, which arrives through the back dispatcher instead of a key event. */
    fun dispatchSystemBack() {
        if (presses.values.any { it.action == JarnAction.Back }) return
        when (val route = resolve(JarnAction.Back)) {
            is Route.Bound -> route.layer.entries[JarnAction.Back]?.onPress?.invoke()
            Route.ReturnToStage -> activate(JarnScreen.Stage)
            Route.PassThrough, Route.Swallow -> Unit
        }
    }

    /** Call when a window loses focus. Pending presses end without firing. */
    fun cancelPresses() {
        presses.entries.removeAll { it.value.route != Route.PassThrough }
    }

    fun hintsFor(screen: JarnScreen): List<JarnInputHint> {
        val hints = LinkedHashMap<JarnKey, JarnInputHint>()
        JarnAction.entries.forEach { action ->
            val key = action.hintKey ?: return@forEach
            val label = hintLabel(screen, action) ?: return@forEach
            hints.getOrPut(key) { JarnInputHint(key, label) }
        }
        return hints.values.toList()
    }

    private fun hintLabel(screen: JarnScreen, action: JarnAction): Int? =
        when (val found = lookup(screen, action)) {
            is Lookup.Found -> found.layer.labels[action]
            is Lookup.Blocked -> null
            Lookup.Open -> if (returnsToStage(screen, action)) R.string.jarnfe_hint_back else null
        }

    private fun onKeyDown(input: JarnKeyInput, action: JarnAction): Boolean {
        val isDuplicate = presses.any { (code, press) ->
            code != input.keyCode && press.action == action && press.route != Route.PassThrough
        }
        if (!isDuplicate && action.hintKey != null && hintLabel(activeScreen, action) != null) {
            onHintPressed?.invoke()
        }
        val route = if (isDuplicate) Route.Swallow else resolve(action)
        val press = Press(action, route, input.eventTimeMs)
        presses[input.keyCode] = press
        if (route == Route.PassThrough) return false
        val binding = press.binding()
        if (binding != null && binding.firesOnDown(action)) {
            press.fired = true
            binding.onPress?.invoke()
        }
        return true
    }

    private fun onKeyRepeat(input: JarnKeyInput): Boolean {
        val press = presses[input.keyCode] ?: return true
        if (press.route == Route.PassThrough) return false
        val binding = press.binding() ?: return true
        when {
            binding.repeats -> binding.onPress?.invoke()
            binding.onLongPress != null && !press.fired && press.isLong(input) -> {
                press.fired = true
                binding.onLongPress.invoke()
            }
        }
        return true
    }

    private fun onKeyUp(input: JarnKeyInput): Boolean {
        val press = presses.remove(input.keyCode) ?: return true
        if (press.route == Route.PassThrough) return false
        if (input.isCanceled || press.fired) return true
        if (press.route == Route.ReturnToStage) {
            activate(JarnScreen.Stage)
            return true
        }
        val binding = press.binding() ?: return true
        val longPress = binding.onLongPress
        if (longPress != null && press.isLong(input)) longPress() else binding.onPress?.invoke()
        return true
    }

    private fun resolve(action: JarnAction): Route = when (val found = lookup(activeScreen, action)) {
        is Lookup.Found -> when {
            found.layer.entries[action]?.onPress != null -> Route.Bound(found.layer)
            found.layer.mode == JarnLayerMode.Capture -> Route.Swallow
            else -> Route.PassThrough
        }
        is Lookup.Blocked ->
            if (action.reachesFocus && found.mode != JarnLayerMode.Capture) Route.PassThrough else Route.Swallow
        Lookup.Open -> when {
            returnsToStage(activeScreen, action) -> Route.ReturnToStage
            action.reachesFocus -> Route.PassThrough
            else -> Route.Swallow
        }
    }

    private fun returnsToStage(screen: JarnScreen, action: JarnAction) =
        action == JarnAction.Back && screen == JarnScreen.Deck

    private fun lookup(screen: JarnScreen, action: JarnAction): Lookup {
        for (index in layers.indices.reversed()) {
            val layer = layers[index]
            if (layer.screen != screen) continue
            if (action in layer.labels) return Lookup.Found(layer)
            if (layer.mode != JarnLayerMode.Passive) {
                return if (action == JarnAction.Swap) global(action) else Lookup.Blocked(layer.mode)
            }
        }
        return global(action)
    }

    private fun global(action: JarnAction): Lookup =
        if (action in globalLayer.labels) Lookup.Found(globalLayer) else Lookup.Open

    private fun JarnBinding.firesOnDown(action: JarnAction) =
        repeats || (action.firesOnPress && onLongPress == null)

    private inner class Press(val action: JarnAction, val route: Route, val downTimeMs: Long) {
        var fired = false

        fun binding(): JarnBinding? {
            val layer = (route as? Route.Bound)?.layer ?: return null
            return if (layer.attached) layer.entries[action] else null
        }

        fun isLong(input: JarnKeyInput) = input.eventTimeMs - downTimeMs >= longPressMs
    }

    private sealed interface Route {
        data class Bound(val layer: JarnInputLayerHandle) : Route
        data object ReturnToStage : Route
        data object PassThrough : Route
        data object Swallow : Route
    }

    private sealed interface Lookup {
        data class Found(val layer: JarnInputLayerHandle) : Lookup
        data class Blocked(val mode: JarnLayerMode) : Lookup
        data object Open : Lookup
    }
}
