package jr.brian.jarnfe.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.library.LibraryHome
import jr.brian.jarnfe.session.LibrarySection
import jr.brian.jarnfe.themekit.LocalThemeLayoutProbe
import jr.brian.jarnfe.themekit.PlacedThemeView
import jr.brian.jarnfe.themekit.ThemeLayoutProbe
import jr.brian.jarnfe.themekit.ThemePxRect
import jr.brian.jarnfe.themes.JarnThemeChoice
import jr.brian.jarnfe.themes.JarnThemePreview

private const val OUTLINE_ALPHA = 0.5f
private const val HELD_FILL_ALPHA = 0.16f

/**
 * The view being edited, live on the player's own library, with every piece outlined and the
 * selected one ringed. The whole app follows the draft while the editor is open.
 */
@Composable
internal fun ThemeEditorCanvas(editor: ThemeEditorState, modifier: Modifier = Modifier) {
    DraftPreviewEffect(editor)
    val probe = remember { ThemeLayoutProbe() }
    LaunchedEffect(probe.placed) { JarnThemeEditor.layoutIssues = probe.placed?.issues.orEmpty() }
    Box(modifier) {
        CompositionLocalProvider(LocalThemeLayoutProbe provides probe) {
            ThemeEditorPreviewScope {
                Box(Modifier.matchParentSize().canvasShape(editor.narrow)) {
                    LibraryHome(
                        section = editor.view.librarySection,
                        library = rememberEditorPreviewLibrary(),
                        onOpenSystem = {},
                        emptyActions = {},
                    )
                }
            }
        }
        val flagged = probe.placed?.issues.orEmpty().mapNotNull { it.layerId }.toSet()
        ThemeEditorSelection(probe, editor.selected, held = editor.held != null, flagged, Modifier.matchParentSize())
    }
}

/** The whole app follows [editor]'s draft while this is in composition: the canvas and Try it. */
@Composable
internal fun DraftPreviewEffect(editor: ThemeEditorState) {
    val choice = JarnThemeChoice(editor.entry)
    DisposableEffect(choice) {
        JarnThemePreview.choice = choice
        onDispose { JarnThemePreview.choice = null }
    }
}

/**
 * The Thor's bottom screen browse area, width over height. Narrower than the narrow-layout cutoff,
 * so a view's narrow patch shows.
 */
internal const val NARROW_CANVAS_ASPECT = 1240f / 860f

/** The full canvas, or a centered box at the bottom screen's shape. */
private fun Modifier.canvasShape(narrow: Boolean): Modifier =
    if (narrow) wrapContentSize().fillMaxHeight().aspectRatio(NARROW_CANVAS_ASPECT, matchHeightConstraintsFirst = true) else this

private val ThemeView.librarySection: LibrarySection
    get() = when (this) {
        ThemeView.Systems -> LibrarySection.Systems
        ThemeView.Games -> LibrarySection.AllGames
    }

/** Every piece's outline, the selected one in the accent, and a tint while it is held. */
@Composable
internal fun ThemeEditorSelection(
    probe: ThemeLayoutProbe,
    selectedTarget: ThemeEditorTarget,
    held: Boolean,
    flagged: Set<String>,
    modifier: Modifier,
) {
    val placed = probe.placed ?: return
    var origin by remember { mutableStateOf(Offset.Zero) }
    val colors = JarnTheme.colors
    Canvas(modifier.onGloballyPositioned { origin = it.positionInRoot() }) {
        val shift = probe.origin - origin
        placed.pieces().forEach { (target, rect) ->
            val selected = target == selectedTarget
            val color = when {
                selected -> colors.accent
                (target as? ThemeEditorTarget.Layer)?.id in flagged -> colors.danger
                else -> colors.textLow.copy(alpha = OUTLINE_ALPHA)
            }
            val width = if (selected) 3.dp.toPx() else 1.dp.toPx()
            if (selected && held) drawPiece(rect, shift, colors.accent.copy(alpha = HELD_FILL_ALPHA), null)
            drawPiece(rect, shift, color, Stroke(width))
        }
    }
}

private fun PlacedThemeView.pieces(): List<Pair<ThemeEditorTarget, ThemePxRect>> =
    listOf<Pair<ThemeEditorTarget, ThemePxRect>>(ThemeEditorTarget.Navigator to primary) +
        (below + above).map { ThemeEditorTarget.Layer(it.layer.id) to it.bounds }

private fun DrawScope.drawPiece(rect: ThemePxRect, shift: Offset, color: Color, stroke: Stroke?) {
    val topLeft = Offset(rect.left + shift.x, rect.top + shift.y)
    val size = Size(rect.width.toFloat(), rect.height.toFloat())
    if (stroke == null) drawRect(color, topLeft, size) else drawRect(color, topLeft, size, style = stroke)
}
