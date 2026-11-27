package jr.brian.jarnfe.themekit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import jr.brian.core.model.theme.ViewSpec
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.look.rememberJarnDisplay

/**
 * Draws a theme view on the current screen: each child sits at its layer's rect, sized against
 * this screen alone, so every screen keeps its own aspect. The layout is meant to fill the whole
 * screen; [insets] mark the hint bar and system bars that Jarn draws over it.
 * [primary] draws the browsable list in its box; [layer] draws one decor layer at its bounds.
 * [onIssues] gets what the guardrails changed whenever the placement changes.
 */
@Composable
internal fun JarnThemeLayout(
    view: ViewSpec,
    primary: @Composable () -> Unit,
    layer: @Composable (PlacedThemeLayer) -> Unit,
    modifier: Modifier = Modifier,
    insets: ThemeInsets = rememberJarnThemeInsets(),
    onIssues: (List<ThemeLayoutIssue>) -> Unit = {},
) {
    val probe = LocalThemeLayoutProbe.current
    val probed = if (probe != null) Modifier.onGloballyPositioned { probe.origin = it.positionInRoot() } else Modifier
    BoxWithConstraints(modifier.then(probed)) {
        val screen = ThemeScreen(constraints.maxWidth, constraints.maxHeight, insets)
        val placed = remember(view, screen) { resolveThemeView(view, screen) }
        ThemeIssuesEffect(placed.issues, onIssues)
        if (probe != null) {
            SideEffect {
                probe.placed = placed
                probe.screen = screen
            }
        }
        PlacedThemeChildren(placed, primary, layer)
    }
}

@Composable
private fun ThemeIssuesEffect(issues: List<ThemeLayoutIssue>, onIssues: (List<ThemeLayoutIssue>) -> Unit) {
    val latest = rememberUpdatedState(onIssues)
    LaunchedEffect(issues) { latest.value(issues) }
}

@Composable
private fun PlacedThemeChildren(
    placed: PlacedThemeView,
    primary: @Composable () -> Unit,
    layer: @Composable (PlacedThemeLayer) -> Unit,
) {
    Layout(
        content = {
            placed.below.forEach { key(it.layer.id) { ThemeSlot(it.bounds) { layer(it) } } }
            key(PRIMARY_KEY) { ThemeSlot(placed.primary, primary) }
            placed.above.forEach { key(it.layer.id) { ThemeSlot(it.bounds) { layer(it) } } }
        },
    ) { measurables, constraints ->
        val placeables = measurables.mapNotNull { measurable ->
            val rect = measurable.layoutId as? ThemePxRect ?: return@mapNotNull null
            measurable.measure(Constraints.fixed(rect.width, rect.height)) to rect
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { (placeable, rect) -> placeable.place(rect.left, rect.top) }
        }
    }
}

/** One node per child, carrying its rect, so a child that draws nothing or several nodes can't shift the others. */
@Composable
private fun ThemeSlot(bounds: ThemePxRect, content: @Composable () -> Unit) {
    Box(Modifier.layoutId(bounds), propagateMinConstraints = true) { content() }
}

private const val PRIMARY_KEY = "jarn-theme-primary"

/** The system bars plus the hint bar along the bottom, when hints are showing. */
@Composable
internal fun rememberJarnThemeInsets(): ThemeInsets {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val bars = WindowInsets.systemBars
    val hintBar = if (rememberJarnDisplay().hintsVisible) with(density) { JarnTheme.space.hintBarHeight.roundToPx() } else 0
    return ThemeInsets(
        left = bars.getLeft(density, direction),
        top = bars.getTop(density),
        right = bars.getRight(density, direction),
        bottom = bars.getBottom(density) + hintBar,
    )
}
