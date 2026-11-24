package jr.brian.jarnfe.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnHint
import jr.brian.core.ui.components.JarnHintBar

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

/** The hint bar for the current screen, dimmed while the other screen is active. */
@Composable
fun JarnScreenHintBar(modifier: Modifier = Modifier) {
    val router = LocalJarnInputRouter.current
    val screen = LocalJarnScreen.current
    val hints = (router.hintsFor(screen) + listOfNotNull(router.tabHintFor(screen)))
        .map { JarnHint(it.key, stringResource(it.labelRes)) }
    JarnHintBar(hints, modifier, active = router.activeScreen == screen)
}
