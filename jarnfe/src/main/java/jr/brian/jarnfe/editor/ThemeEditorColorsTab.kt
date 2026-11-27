package jr.brian.jarnfe.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.model.theme.ThemeColor
import jr.brian.core.model.theme.ThemeColors
import jr.brian.core.ui.components.JarnChoiceRow
import jr.brian.core.ui.components.JarnSliderRow
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import kotlin.math.roundToInt

private const val HUE_MAX = 360f
private const val HUE_STEPS = 72
private const val SHADE_STEPS = 50
private const val PERCENT = 100

/**
 * The theme's colors: follow the player's Look, or its own named colors, each set by hue,
 * saturation and lightness. Colors Jarn can't read get a Fix.
 */
@Composable
internal fun ThemeEditorColorsTab(editor: ThemeEditorState) {
    val follows = editor.spec.meta.followsLook
    val lookColors = JarnTheme.colors.toThemeColors()
    InspectToggleRow(R.string.jarnfe_editor_follow_look, follows, stringResource(R.string.jarnfe_editor_follow_look_hint)) { on ->
        JarnThemeEditor.edit { withFollowsLook(on, lookColors) }
    }
    if (follows) return
    val colors = editor.spec.tokens.colors
    if (!colors.isReadable) ReadabilityWarning()
    var editing by remember { mutableStateOf(ThemeEditorColor.Accent) }
    ThemeEditorColor.entries.forEach { named ->
        NamedColorRow(named, colors.get(named), chosen = named == editing) { editing = named }
    }
    HslRows(colors.get(editing)) { next -> JarnThemeEditor.edit { withColors(colors.with(editing, next)) } }
}

@Composable
private fun ReadabilityWarning() {
    JarnText(stringResource(R.string.jarnfe_editor_hard_to_read), JarnTheme.type.body, JarnTheme.colors.danger)
    InspectActionRow(stringResource(R.string.jarnfe_editor_fix), stringResource(R.string.jarnfe_editor_fix_hint)) {
        JarnThemeEditor.edit { withColors(spec.tokens.colors.fixedForReading()) }
    }
}

@Composable
private fun NamedColorRow(named: ThemeEditorColor, value: ThemeColor, chosen: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ColorSwatch(value)
        JarnChoiceRow(
            label = stringResource(named.labelRes),
            chosen = chosen,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            supporting = value.toString(),
        )
    }
}

@Composable
private fun ColorSwatch(value: ThemeColor) {
    Box(
        Modifier
            .size(24.dp)
            .background(Color(value.argb), CircleShape)
            .border(1.dp, JarnTheme.colors.line, CircleShape),
    )
}

@Composable
private fun HslRows(value: ThemeColor, onChange: (ThemeColor) -> Unit) {
    val hsl = value.hsl
    JarnSliderRow(
        label = stringResource(R.string.jarnfe_editor_hue),
        value = hsl.hue,
        valueText = hsl.hue.roundToInt().toString(),
        onValueChange = { onChange(hsl.copy(hue = it).toThemeColor()) },
        modifier = Modifier.fillMaxWidth(),
        valueRange = 0f..HUE_MAX,
        steps = HUE_STEPS,
    )
    ShadeRow(R.string.jarnfe_editor_saturation, hsl.saturation) { onChange(hsl.copy(saturation = it).toThemeColor()) }
    ShadeRow(R.string.jarnfe_editor_lightness, hsl.lightness) { onChange(hsl.copy(lightness = it).toThemeColor()) }
}

@Composable
private fun ShadeRow(labelRes: Int, value: Float, onChange: (Float) -> Unit) {
    JarnSliderRow(
        label = stringResource(labelRes),
        value = value,
        valueText = stringResource(R.string.jarnfe_settings_percent, (value * PERCENT).roundToInt()),
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        steps = SHADE_STEPS,
    )
}

/** Turning the Look off starts the theme's colors from the Look the player sees right now. */
internal fun ThemeEditorState.withFollowsLook(follows: Boolean, lookColors: ThemeColors): ThemeEditorState {
    val meta = spec.meta.copy(followsLook = follows)
    val tokens = if (follows) spec.tokens else spec.tokens.copy(colors = lookColors)
    return copy(spec = spec.copy(meta = meta, tokens = tokens))
}

internal fun ThemeEditorState.withColors(colors: ThemeColors): ThemeEditorState =
    copy(spec = spec.copy(tokens = spec.tokens.copy(colors = colors)))
