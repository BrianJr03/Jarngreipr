package jr.brian.jarnfe.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jr.brian.core.model.theme.ThemeAlign
import jr.brian.core.model.theme.ThemeColorRole
import jr.brian.core.model.theme.ThemeCorner
import jr.brian.core.model.theme.ThemeDateField
import jr.brian.core.model.theme.ThemeDateFormat
import jr.brian.core.model.theme.ThemeEdge
import jr.brian.core.model.theme.ThemeFade
import jr.brian.core.model.theme.ThemeFit
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePicture
import jr.brian.core.model.theme.ThemeText
import jr.brian.core.model.theme.ThemeTextRole
import jr.brian.jarnfe.R

/** The stack every editor-made stack shares, so turning it on joins the others. */
internal const val EDITOR_STACK = "info"

private val LinesRange = 1..6
private val TextColors = ThemeColorRole.entries
private val Edges: List<ThemeEdge?> = listOf(null) + ThemeEdge.entries
private val Fallbacks: List<ThemePicture?> = listOf(null) + PictureChoices

/** The rows that belong to [layer]'s kind. Each change is one undo step. */
@Composable
internal fun LayerKindRows(layer: ThemeLayer) {
    when (layer) {
        is ThemeLayer.Picture -> PictureRows(layer)
        is ThemeLayer.Text -> TextRows(layer)
        is ThemeLayer.Date -> DateRows(layer)
        is ThemeLayer.Rating -> RatingRows(layer)
        is ThemeLayer.Panel -> PanelRows(layer)
        is ThemeLayer.Video -> VideoRows(layer)
    }
}

private inline fun <reified T : ThemeLayer> editLayer(id: String, crossinline transform: (T) -> T) =
    JarnThemeEditor.edit { withLayer(id) { (it as? T)?.let(transform) ?: it } }

@Composable
private fun PictureRows(layer: ThemeLayer.Picture) {
    InspectChoiceRow(R.string.jarnfe_editor_shows, layer.source, PictureChoices, { it.labelRes }) { next ->
        editLayer<ThemeLayer.Picture>(layer.id) { it.copy(source = next) }
    }
    FallbackRow(layer.fallbacks.firstOrNull()) { next -> editLayer<ThemeLayer.Picture>(layer.id) { it.copy(fallbacks = listOfNotNull(next)) } }
    InspectChoiceRow(R.string.jarnfe_editor_fit, layer.fit, ThemeFit.entries, { it.labelRes }) { next ->
        editLayer<ThemeLayer.Picture>(layer.id) { it.copy(fit = next) }
    }
    CornerRow(layer.corner) { next -> editLayer<ThemeLayer.Picture>(layer.id) { it.copy(corner = next) } }
}

@Composable
private fun TextRows(layer: ThemeLayer.Text) {
    val choices = if (layer.content is ThemeText.Static) listOf(layer.content) + TextChoices else TextChoices
    InspectChoiceRow(R.string.jarnfe_editor_shows, layer.content, choices, { it.labelRes }) { next ->
        editLayer<ThemeLayer.Text>(layer.id) { it.copy(content = next) }
    }
    TextLookRows(layer.style, layer.color, layer.align, layer.verticalAlign) { style, color, align, vertical ->
        editLayer<ThemeLayer.Text>(layer.id) { it.copy(style = style, color = color, align = align, verticalAlign = vertical) }
    }
    InspectCountRow(R.string.jarnfe_editor_lines, layer.maxLines, LinesRange) { n -> editLayer<ThemeLayer.Text>(layer.id) { it.copy(maxLines = n) } }
    StackRow(layer.stack) { stack -> editLayer<ThemeLayer.Text>(layer.id) { it.copy(stack = stack) } }
}

@Composable
private fun DateRows(layer: ThemeLayer.Date) {
    InspectChoiceRow(R.string.jarnfe_editor_shows, layer.field, ThemeDateField.entries, { it.labelRes }) { next ->
        editLayer<ThemeLayer.Date>(layer.id) { it.copy(field = next) }
    }
    InspectChoiceRow(R.string.jarnfe_editor_date_format, layer.format, ThemeDateFormat.entries, { it.labelRes }) { next ->
        editLayer<ThemeLayer.Date>(layer.id) { it.copy(format = next) }
    }
    TextLookRows(layer.style, layer.color, layer.align, layer.verticalAlign) { style, color, align, vertical ->
        editLayer<ThemeLayer.Date>(layer.id) { it.copy(style = style, color = color, align = align, verticalAlign = vertical) }
    }
    StackRow(layer.stack) { stack -> editLayer<ThemeLayer.Date>(layer.id) { it.copy(stack = stack) } }
}

@Composable
private fun RatingRows(layer: ThemeLayer.Rating) {
    ColorRow(layer.color) { next -> editLayer<ThemeLayer.Rating>(layer.id) { it.copy(color = next) } }
    StackRow(layer.stack) { stack -> editLayer<ThemeLayer.Rating>(layer.id) { it.copy(stack = stack) } }
}

@Composable
private fun PanelRows(layer: ThemeLayer.Panel) {
    ColorRow(layer.color) { next -> editLayer<ThemeLayer.Panel>(layer.id) { it.copy(color = next) } }
    CornerRow(layer.corner) { next -> editLayer<ThemeLayer.Panel>(layer.id) { it.copy(corner = next) } }
    InspectToggleRow(R.string.jarnfe_editor_outlined, layer.outlined) { on -> editLayer<ThemeLayer.Panel>(layer.id) { it.copy(outlined = on) } }
    InspectChoiceRow(R.string.jarnfe_editor_fade, layer.fade?.edge, Edges, { it.labelRes }) { edge ->
        editLayer<ThemeLayer.Panel>(layer.id) { it.copy(fade = edge?.let { e -> ThemeFade(e) }) }
    }
}

@Composable
private fun VideoRows(layer: ThemeLayer.Video) {
    InspectToggleRow(R.string.jarnfe_editor_video_sound, layer.audio) { on -> editLayer<ThemeLayer.Video>(layer.id) { it.copy(audio = on) } }
    FallbackRow(layer.fallback) { next -> editLayer<ThemeLayer.Video>(layer.id) { it.copy(fallback = next) } }
    CornerRow(layer.corner) { next -> editLayer<ThemeLayer.Video>(layer.id) { it.copy(corner = next) } }
}

/** Size, color and alignment, shared by text and dates. */
@Composable
private fun TextLookRows(
    style: ThemeTextRole,
    color: ThemeColorRole,
    align: ThemeAlign,
    vertical: ThemeAlign,
    onChange: (ThemeTextRole, ThemeColorRole, ThemeAlign, ThemeAlign) -> Unit,
) {
    InspectChoiceRow(R.string.jarnfe_editor_text_size, style.editorSize, ThemeEditorTextSize.entries, { it.labelRes }) { next ->
        onChange(next.role, color, align, vertical)
    }
    ColorRow(color) { next -> onChange(style, next, align, vertical) }
    InspectChoiceRow(R.string.jarnfe_editor_align, align, ThemeAlign.entries, { it.labelRes }) { next -> onChange(style, color, next, vertical) }
    InspectChoiceRow(R.string.jarnfe_editor_valign, vertical, ThemeAlign.entries, { it.labelRes }) { next -> onChange(style, color, align, next) }
}

@Composable
private fun ColorRow(color: ThemeColorRole, onChange: (ThemeColorRole) -> Unit) {
    InspectChoiceRow(R.string.jarnfe_editor_color, color, TextColors, { it.labelRes }, onChoose = onChange)
}

@Composable
private fun CornerRow(corner: ThemeCorner, onChange: (ThemeCorner) -> Unit) {
    InspectChoiceRow(R.string.jarnfe_editor_corner, corner, ThemeCorner.entries, { it.labelRes }, onChoose = onChange)
}

@Composable
private fun FallbackRow(current: ThemePicture?, onChange: (ThemePicture?) -> Unit) {
    InspectChoiceRow(R.string.jarnfe_editor_fallback, current, Fallbacks, { it.labelRes }, onChoose = onChange)
}

@Composable
private fun StackRow(stack: String?, onChange: (String?) -> Unit) {
    InspectToggleRow(R.string.jarnfe_editor_stack, stack != null, stringResource(R.string.jarnfe_editor_stack_hint)) { on ->
        onChange(if (on) stack ?: EDITOR_STACK else null)
    }
}
