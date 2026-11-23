package jr.brian.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.R
import jr.brian.core.ui.focus.jarnClickable
import jr.brian.core.ui.focus.jarnDisabledAlpha
import jr.brian.core.ui.focus.jarnFocus
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnTheme

enum class JarnButtonVariant { Primary, Secondary, Danger }

private data class JarnButtonColors(val container: Color, val content: Color)

@Composable
private fun JarnButtonVariant.colors(): JarnButtonColors {
    val colors = JarnTheme.colors
    return when (this) {
        JarnButtonVariant.Primary -> JarnButtonColors(colors.accent, colors.onAccent)
        JarnButtonVariant.Secondary -> JarnButtonColors(colors.raised, colors.textHigh)
        JarnButtonVariant.Danger -> JarnButtonColors(colors.danger, colors.onDanger)
    }
}

@Composable
private fun JarnButtonVariant.textStyle(): TextStyle =
    if (this == JarnButtonVariant.Primary) JarnTheme.type.heading else JarnTheme.type.labelStrong

@Composable
fun JarnButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: JarnButtonVariant = JarnButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    focusableWhenDisabled: Boolean = false,
    icon: Painter? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val shape = JarnTheme.radius.mdShape
    val buttonColors = variant.colors()
    val loadingDescription = stringResource(R.string.jarn_state_loading)
    Row(
        modifier = modifier
            .jarnFocus(shape, source, enabled && !loading)
            .jarnClickable(
                interactionSource = source,
                enabled = enabled && !loading,
                focusableWhenDisabled = focusableWhenDisabled || (enabled && loading),
                onClick = onClick,
            )
            .semantics { if (loading) stateDescription = loadingDescription }
            .jarnDisabledAlpha(enabled)
            .heightIn(min = 48.dp)
            .background(buttonColors.container, shape)
            .padding(horizontal = JarnTheme.space.lg, vertical = JarnTheme.space.sm),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JarnButtonLeading(loading, icon, buttonColors.content)
        JarnText(label, variant.textStyle(), buttonColors.content, maxLines = 1)
    }
}

@Composable
private fun JarnButtonLeading(loading: Boolean, icon: Painter?, tint: Color) {
    when {
        loading -> JarnSpinner(tint)
        icon != null -> JarnIcon(icon, tint)
    }
}

@Composable
internal fun JarnIcon(icon: Painter, tint: Color, modifier: Modifier = Modifier) {
    Image(
        painter = icon,
        contentDescription = null,
        modifier = modifier.size(20.dp),
        colorFilter = ColorFilter.tint(tint),
    )
}

@Composable
fun JarnActionTile(
    label: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusableWhenDisabled: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val shape = JarnTheme.radius.mdShape
    Column(
        modifier = modifier
            .jarnInteractive(shape, source, enabled, focusableWhenDisabled, onClick = onClick)
            .widthIn(min = 88.dp)
            .heightIn(min = 80.dp)
            .background(JarnTheme.colors.surface, shape)
            .padding(JarnTheme.space.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm, Alignment.CenterVertically),
    ) {
        JarnIcon(icon, JarnTheme.colors.textHigh)
        JarnText(
            text = label,
            style = JarnTheme.type.label,
            color = JarnTheme.colors.textHigh,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}
