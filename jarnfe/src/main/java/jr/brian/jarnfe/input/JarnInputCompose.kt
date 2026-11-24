package jr.brian.jarnfe.input

import android.view.Display
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnHint
import jr.brian.core.ui.components.JarnHintBar
import jr.brian.core.ui.components.JarnKey
import java.util.Locale

val LocalJarnInputRouter = staticCompositionLocalOf<JarnInputRouter> {
    error("No JarnInputRouter provided. Wrap the screen in ProvideJarnInput.")
}

val LocalJarnScreen = staticCompositionLocalOf { JarnScreen.Stage }

private val LocalJarnLayerDepth = staticCompositionLocalOf { 0 }

@Composable
fun ProvideJarnInput(
    router: JarnInputRouter,
    screen: JarnScreen,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalJarnInputRouter provides router,
        LocalJarnScreen provides screen,
        LocalJarnLayerDepth provides 0,
        content = content,
    )
}

/**
 * Registers an input layer while in composition. Layers nested in [content] sit above this one,
 * whatever order their effects run in. Among siblings, the most recently enabled is on top.
 */
@Composable
fun JarnInputLayer(
    mode: JarnLayerMode = JarnLayerMode.Passive,
    enabled: Boolean = true,
    bindings: JarnBindingsBuilder.() -> Unit,
    content: @Composable () -> Unit = {},
) {
    val router = LocalJarnInputRouter.current
    val screen = LocalJarnScreen.current
    val depth = LocalJarnLayerDepth.current
    val layer = remember(router, screen, mode, depth) { router.createLayer(screen, mode, depth) }
    val declared = jarnBindings(bindings)
    SideEffect { layer.update(declared) }
    DisposableEffect(router, layer, enabled) {
        layer.update(declared)
        if (enabled) router.attach(layer)
        onDispose { router.detach(layer) }
    }
    CompositionLocalProvider(LocalJarnLayerDepth provides depth + 1, content = content)
}

/**
 * The hint bar for the current screen, dimmed while the other screen is active. On the top
 * screen, the system and tab hints sit on the right.
 */
@Composable
fun JarnScreenHintBar(modifier: Modifier = Modifier) {
    val router = LocalJarnInputRouter.current
    val screen = LocalJarnScreen.current
    val hints = (router.hintsFor(screen) + listOfNotNull(router.tabHintFor(screen)))
        .map { JarnHint(it.key, jarnHintCase(stringResource(it.labelRes))) }
    val (trailing, leading) =
        if (isOnTopScreen()) jarnSplitTrailingHints(hints) else emptyList<JarnHint>() to hints
    JarnHintBar(leading, modifier, active = router.activeScreen == screen, trailing = trailing)
}

private val TrailingHintKeys = listOf(JarnKey.Start, JarnKey.X, JarnKey.L1R1, JarnKey.L1, JarnKey.R1)

/**
 * The system keys on the right: Start, X (switch screen), then the tab keys, in that order.
 * Everything else, including Select's options, stays on the left as given.
 */
internal fun jarnSplitTrailingHints(hints: List<JarnHint>): Pair<List<JarnHint>, List<JarnHint>> {
    val trailing = hints.filter { it.key in TrailingHintKeys }.sortedBy { TrailingHintKeys.indexOf(it.key) }
    return trailing to hints.filterNot { it.key in TrailingHintKeys }
}

@Composable
private fun isOnTopScreen(): Boolean =
    (LocalView.current.display?.displayId ?: Display.DEFAULT_DISPLAY) == Display.DEFAULT_DISPLAY

/**
 * Hint labels capitalize every word ("Switch Screen"), whatever case the string has, so a string
 * shared with a title or a button keeps its sentence case there.
 */
internal fun jarnHintCase(label: String): String =
    label.split(' ').joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(Locale.getDefault()) } }
