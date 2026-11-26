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
    internal var longPressLabels by mutableStateOf<Map<JarnAction, Int>>(emptyMap())
    internal var sequence = 0L
    internal var attached = false

    fun update(bindings: JarnBindings) {
        entries = bindings.entries
        labels = bindings.labels
        longPressLabels = bindings.longPressLabels
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

    /**
     * Runs once per physical press of a button that does something (a binding, a focused item's A,
     * or the hold X swap). Its repeats and duplicates never run it. D-pad presses use
     * [onDirectionPressed] instead, since only a move that lands should be felt.
     */
    var onButtonPressed: (() -> Unit)? = null

    /** Runs for every D-pad press and repeat, whatever handles it. */
    var onDirectionPressed: (() -> Unit)? = null

    /**
     * Sees each fresh press before any layer. Returning true swallows the whole press, for UI that
     * a press only brings back, such as Now hidden over a game preview.
     */
    var pressInterceptor: ((JarnAction) -> Boolean)? = null

    /** The player's "Mute with L3 and R3" setting, set by the shell. */
    var sticksMute = false

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
        val action = JarnButtonMap.actionFor(input.keyCode, sticksMute) ?: return false
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
        if (pressInterceptor?.invoke(JarnAction.Back) == true) return
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

    /** True while a sheet, keyboard, grab or other non-passive layer is open on [screen]. */
    fun hasModalLayer(screen: JarnScreen): Boolean =
        layers.any { it.screen == screen && it.mode != JarnLayerMode.Passive }

    /**
     * The screen whose tabs a shell-level press of [JarnShoulderPair] would step right now, or null
     * when there is nothing to step. Set by the shell, which knows where each role sits.
     */
    var shellTabTarget: (JarnShoulderPair) -> JarnScreen? = { null }

    /** One hint per key, then the hold hints, which share a key with a tap. */
    fun hintsFor(screen: JarnScreen): List<JarnInputHint> {
        val hints = LinkedHashMap<JarnKey, JarnInputHint>()
        val holds = LinkedHashMap<JarnKey, JarnInputHint>()
        JarnAction.entries.forEach { action ->
            val key = action.hintKey ?: return@forEach
            hintLabel(screen, action)?.let { hints.getOrPut(key) { JarnInputHint(key, it) } }
            longPressLabel(screen, action)?.let { holds.getOrPut(key) { JarnInputHint(key, it) } }
        }
        return hints.values.toList() + holds.values
    }

    private fun longPressLabel(screen: JarnScreen, action: JarnAction): Int? =
        (lookup(screen, action) as? Lookup.Found)?.layer?.longPressLabels?.get(action)

    /**
     * The shoulder pair that moves this screen's tabs: L1/R1 on the top screen and L2/R2 on the
     * bottom one. A layer that claims a pair without a label is taken to step the active screen.
     */
    fun tabHintFor(screen: JarnScreen): JarnInputHint? =
        JarnShoulderPair.entries.firstOrNull { tabTarget(it) == screen }
            ?.let { JarnInputHint(it.key, R.string.jarnfe_hint_tabs) }

    private fun tabTarget(pair: JarnShoulderPair): JarnScreen? {
        if (!bindsWithoutLabel(activeScreen, pair.previous) || !bindsWithoutLabel(activeScreen, pair.next)) {
            return null
        }
        val layer = (lookup(activeScreen, pair.previous) as? Lookup.Found)?.layer
        return if (layer === globalLayer) shellTabTarget(pair) else activeScreen
    }

    private fun bindsWithoutLabel(screen: JarnScreen, action: JarnAction): Boolean =
        (lookup(screen, action) as? Lookup.Found)?.layer?.labels?.get(action) == null &&
            lookup(screen, action) is Lookup.Found

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
        val isIntercepted = !isDuplicate && pressInterceptor?.invoke(action) == true
        val route = if (isDuplicate || isIntercepted) Route.Swallow else resolve(action)
        when {
            action.isDirection -> onDirectionPressed?.invoke()
            route != Route.Swallow -> onButtonPressed?.invoke()
        }
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
        if (press.action.isDirection) onDirectionPressed?.invoke()
        if (press.route == Route.PassThrough) return false
        if (press.firesHeldAction(input)) return true
        val binding = press.binding() ?: return true
        when {
            binding.repeats -> (binding.onRepeat ?: binding.onPress)?.invoke()
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
        if (press.firesHeldAction(input)) return true
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
                return if (action.isAppWide) global(action) else Lookup.Blocked(layer.mode)
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

        /** Runs [JarnAction.heldAction] once a press is long, unless the binding has its own long press. */
        fun firesHeldAction(input: JarnKeyInput): Boolean {
            val held = action.heldAction ?: return false
            if (fired || !isLong(input) || binding()?.onLongPress != null) return false
            fired = true
            val route = resolve(held) as? Route.Bound
            route?.layer?.entries?.get(held)?.onPress?.let { swap ->
                onButtonPressed?.invoke()
                swap()
            }
            return true
        }
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
